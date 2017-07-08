package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.GraphQLMain;
import com.conveyal.gtfs.api.graphql.WrappedGTFSEntity;
import com.conveyal.gtfs.model.Entity;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A generic fetcher to get fields out of an SQL database table.
 */
public class JDBCFetcher implements DataFetcher {

    public static final Logger LOG = LoggerFactory.getLogger(JDBCFetcher.class);

    String tableName;

    // Supply an SQL result row -> Object transformer

    public JDBCFetcher(String tableName) {
        this.tableName = tableName;
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

    @Override
    public List<Map<String, Object>> get(DataFetchingEnvironment environment) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> feedIds = environment.getArgument("feed_id");
        for (String feedId : feedIds) {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append(String.format("select * from %s.%s", feedId, tableName));
            List<String> conditions = new ArrayList<>();
            Entity source = (Entity) environment.getSource();
            if (source != null) {
                conditions.add(String.join(" = ", source.getId(), source.getId())); // FIXME should be getting field name, not contents
            }
            for (String key : environment.getArguments().keySet()) {
                if ("feed_id".equals(key)) continue;
                List<String> values = (List<String>) environment.getArguments().get(key);
                if (values != null && !values.isEmpty()) conditions.add(makeInClause(key, values));
            }
            if ( ! conditions.isEmpty()) {
                sqlBuilder.append(" where ");
                sqlBuilder.append(String.join(" and ", conditions));
            }
            try {
                Statement statement = GraphQLMain.connection.createStatement();
                LOG.info("SQL: {}", sqlBuilder.toString());
                if (statement.execute(sqlBuilder.toString())) {
                    ResultSet resultSet = statement.getResultSet();
                    ResultSetMetaData meta = resultSet.getMetaData();
                    int nColumns = meta.getColumnCount();
                    // Iterate over result rows
                    while (resultSet.next()) {
                        // Create a Map to hold the contents of this row, injecting the feed_id into every map
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("feed_id", feedId);
                        for (int i = 1; i < nColumns; i++) {
                            resultMap.put(meta.getColumnName(i), resultSet.getObject(i));
                        }
                        results.add(resultMap);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
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
