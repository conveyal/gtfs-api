package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.GraphQLMain;
import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import com.conveyal.gtfs.model.Entity;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.conveyal.gtfs.api.util.GraphQLUtil.multiStringArg;
import static com.conveyal.gtfs.api.util.GraphQLUtil.stringArg;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

/**
 * A generic fetcher to get fields out of an SQL database table.
 */
public class JDBCFetcher implements DataFetcher {

    public static final Logger LOG = LoggerFactory.getLogger(JDBCFetcher.class);

    // Make this an option to the GraphQL query.
    public static final int DEFAULT_ROWS_TO_FETCH = 50;
    public static final int MAX_ROWS_TO_FETCH = 500;

    String tableName;

    // Supply an SQL result row -> Object transformer

    public JDBCFetcher(String tableName) {
        this.tableName = tableName;
    }

    // Generate a field definition for inclusion in a GraphQL schema.
    // Hmm, this doesn't work well because we need custom inclusion of sub-tables in each table type.
    // We should make stock field definitions for these tables
    public static GraphQLFieldDefinition field (String tableName) {
        return newFieldDefinition()
                .name(tableName)
                .type(new GraphQLList(GraphQLGtfsSchema.routeType))
                .argument(stringArg("namespace"))
                .argument(multiStringArg("route_id"))
                .dataFetcher(new JDBCFetcher(tableName))
                .build();
    }

    // Horrifically, we're going from SQL response to Gtfs-lib Java model object to GraphQL Java object to JSON.
    // What if we did direct SQL->JSON?
    // Could we transform JDBC ResultSets directly to JSON?
    // With Jackson streaming API we can make a ResultSet serializer: https://stackoverflow.com/a/8120442

    // We could apply a transformation from ResultSet to Gtfs-lib model object, but then more DataFetchers
    // need to be defined to pull the fields out of those model objects. I'll try to skip those intermediate objects.

    // Unfortunately we can't just apply DataFetchers directly to the ResultSets because they're cursors, and we'd
    // have to somehow advance them at the right moment. So we need to transform the SQL results into fully materialized
    // Java objects, then transform those into GraphQL fields. Fortunately the final transformation is trivial fetching
    // from a Map<String, Object>.
    // But what are the internal GraphQL objects, i.e. what does an ExecutionResult return? Are they Map<String, Object>?


    // TODO another parameter for where clause key in the parent-object
    @Override
    public List<Map<String, Object>> get (DataFetchingEnvironment environment) {
        List<Map<String, Object>> results = new ArrayList<>();
        // Apparently you can't get the arguments from the parent - how do you have arguments on sub-fields?
        // String namespace = environment.getArgument("namespace"); // This is going to be the unique prefix, not the feedId in the usual sense
        // This DataFetcher only makes sense when the enclosing parent object is a feed or something in a feed.
        // So it should always be represented as a map with a namespace key.

        // GetSource is the context in which this this DataFetcher has been created, in this case a map representing the parent feed.
        Map<String, Object> parentEntityMap = environment.getSource();
        String namespace = (String) parentEntityMap.get("namespace");
        StringBuilder sqlBuilder = new StringBuilder();
        // TODO select only the specified fields using environment.getFields(), for now we just get them all.
        // The advantage of selecting * is that we don't need to validate the field names.
        sqlBuilder.append(String.format("select * from %s.%s", namespace, tableName));
        List<String> conditions = new ArrayList<>();
        if (environment.getSource() instanceof Entity) {
            Entity source = (Entity) environment.getSource();
            if (source != null) {
                conditions.add(String.join(" = ", source.getId(), source.getId())); // FIXME should be getting field name, not contents
            }
        }
        for (String key : environment.getArguments().keySet()) {
            // Limit and Offset arguments are for pagination. All others become "where X in A, B, C" clauses.
            if ("limit".equals(key) || "offset".equals(key)) continue;
            List<String> values = (List<String>) environment.getArguments().get(key);
            if (values != null && !values.isEmpty()) conditions.add(makeInClause(key, values));
        }
        if ( ! conditions.isEmpty()) {
            sqlBuilder.append(" where ");
            sqlBuilder.append(String.join(" and ", conditions));
        }
        Integer limit = (Integer) environment.getArguments().get("limit");
        if (limit == null) {
            limit = DEFAULT_ROWS_TO_FETCH;
        }
        if (limit > MAX_ROWS_TO_FETCH) {
            limit = MAX_ROWS_TO_FETCH;
        }
        sqlBuilder.append(" limit " + limit);
        Integer offset = (Integer) environment.getArguments().get("offset");
        if (offset != null && offset >= 0) {
            sqlBuilder.append(" offset " + offset);
        }
        Connection connection = null;
        try {
            connection = GraphQLMain.dataSource.getConnection();
            Statement statement = connection.createStatement();
            LOG.info("SQL: {}", sqlBuilder.toString());
            if (statement.execute(sqlBuilder.toString())) {
                ResultSet resultSet = statement.getResultSet();
                ResultSetMetaData meta = resultSet.getMetaData();
                int nColumns = meta.getColumnCount();
                // Iterate over result rows
                while (resultSet.next()) {
                    // Create a Map to hold the contents of this row, injecting the feed_id into every map
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("namespace", namespace);
                    for (int i = 1; i < nColumns; i++) {
                        resultMap.put(meta.getColumnName(i), resultSet.getObject(i));
                    }
                    results.add(resultMap);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(connection);
        }
        // Return a List of Maps, one Map for each row in the result.
        return results;
    }

    private String makeInClause(String key, List<String> strings) {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        if (strings.size() == 1) {
            sb.append(" = ");
            quote(sb, strings.get(0));
        } else {
            sb.append(" in (");
            for (int i = 0; i < strings.size(); i++) {
                if (i > 0) sb.append(",");
                quote(sb, strings.get(i));
            }
            sb.append(")");
        }
        return sb.toString();
    }

    // TODO SQL sanitization to avoid injection
    private void quote(StringBuilder sb, String string) {
        sb.append("'");
        sb.append(string);
        sb.append("'");
    }

}
