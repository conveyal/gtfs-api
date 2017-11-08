package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.GraphQLMain;
import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import com.conveyal.gtfs.loader.JDBCTableReader;
import com.conveyal.gtfs.model.Entity;
import graphql.Scalars;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.conveyal.gtfs.api.util.GraphQLUtil.multiStringArg;
import static com.conveyal.gtfs.api.util.GraphQLUtil.stringArg;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

/**
 * Get quantity of rows in the given table.
 */
public class RowCountFetcher implements DataFetcher {

    public static final Logger LOG = LoggerFactory.getLogger(RowCountFetcher.class);

    private final String tableName;

    public RowCountFetcher(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public Integer get(DataFetchingEnvironment environment) {
        Map<String, Object> parentFeedMap = environment.getSource();
        String namespace = (String) parentFeedMap.get("namespace");
        Connection connection = null;
        try {
            connection = GraphQLMain.dataSource.getConnection();
            Statement statement = connection.createStatement();
            String sql = String.format("select count(*) from %s.%s", namespace, tableName);
            if (statement.execute(sql)) {
                ResultSet resultSet = statement.getResultSet();
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            // In case the table doesn't exist in this feed, just return zero and don't print noise to the log.
            // Unfortunately JDBC doesn't seem to define reliable error codes.
            if (! JDBCTableReader.SQL_STATE_UNDEFINED_TABLE.equals(e.getSQLState())) {
                e.printStackTrace();
            }
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return 0;
    }

    /**
     * Convenience method to create a field in a GraphQL schema that fetches the number of rows in a table.
     * Must be on a type that has a "namespace" field for context.
     */
    public static GraphQLFieldDefinition field (String fieldName, String tableName) {
        return newFieldDefinition()
                .name(fieldName)
                .type(Scalars.GraphQLInt)
                .dataFetcher(new RowCountFetcher(tableName))
                .build();
    }

    /**
     * For cases where the GraphQL field name is the same as the table name itself.
     */
    public static GraphQLFieldDefinition field (String tableName) {
        return field(tableName, tableName);
    }

}