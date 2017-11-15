package com.conveyal.gtfs.api.graphql.fetchers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.conveyal.gtfs.api.GraphQLMain;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

/**
 * A generic fetcher to get row count out of an SQL table filtered by parent join field.
 */
public class RowCountFilteredFetcher implements DataFetcher<Integer> {

    public static final Logger LOG = LoggerFactory.getLogger(RowCountFilteredFetcher.class);

    public final String tableName;
    public final String parentJoinField;

    /**
     * @param tableName the database table from which to fetch rows.
     * @param parentJoinField The field in the enclosing level of the Graphql query to use in a where clause.
     *        This allows e.g. selecting the number of stop_times within a trip, using the enclosing trip's trip_id.
     */
    public RowCountFilteredFetcher (String tableName, String parentJoinField) {
        this.tableName = tableName;
        this.parentJoinField = parentJoinField;
    }


    @Override
    public Integer get (DataFetchingEnvironment environment) {

        Map<String, Object> parentEntityMap = environment.getSource();
        String namespace = (String) parentEntityMap.get("namespace");
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(String.format("select count(*) as row_count from %s.%s", namespace, tableName));

        // We will build up additional sql clauses in this List.
        List<String> conditions = new ArrayList<>();

        // If we are fetching an item nested within a GTFS entity in the Graphql query, we want to add an SQL "where"
        // clause. This could conceivably be done automatically, but it's clearer to just express the intent.
        // Note, this is assuming the type of the field in the parent is a string.
        if (parentJoinField != null) {
            Map<String, Object> enclosingEntity = environment.getSource();
            // FIXME SQL injection: enclosing entity's ID could contain malicious character sequences; quote and sanitize the string.
            conditions.add(String.join(" = ", parentJoinField, JDBCFetcher.quote(enclosingEntity.get(parentJoinField).toString())));
        }
        for (String key : environment.getArguments().keySet()) {
            // Limit and Offset arguments are for pagination. All others become "where X in A, B, C" clauses.
            if ("limit".equals(key) || "offset".equals(key)) continue;
            List<String> values = (List<String>) environment.getArguments().get(key);
            if (values != null && !values.isEmpty()) conditions.add(JDBCFetcher.makeInClause(key, values));
        }
        if ( ! conditions.isEmpty()) {
            sqlBuilder.append(" where ");
            sqlBuilder.append(String.join(" and ", conditions));
        }

        Connection connection = null;
        Integer count = null;
        try {
            connection = GraphQLMain.dataSource.getConnection();
            Statement statement = connection.createStatement();
            // LOG.info("SQL: {}", sqlBuilder.toString());
            if (statement.execute(sqlBuilder.toString())) {
                ResultSet resultSet = statement.getResultSet();
                resultSet.next();
                count = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return count;
    }
}
