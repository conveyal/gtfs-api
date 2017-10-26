package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.GraphQLMain;
import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.conveyal.gtfs.api.util.GraphQLUtil.multiStringArg;
import static com.conveyal.gtfs.api.util.GraphQLUtil.stringArg;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

/**
 * This wraps an SQL row fetcher, extracting only a single column of the specified type.
 * Because there's only one column, it collapses the result down into a list of elements of that column's type,
 * rather than a list of maps (one for each row) as the basic SQL fetcher does.
 */
public class SQLColumnFetcher<T> implements DataFetcher<List<T>> {

    public static final Logger LOG = LoggerFactory.getLogger(SQLColumnFetcher.class);

    public final String columnName;

    private final JDBCFetcher jdbcFetcher;

    /**
     * Constructor for tables that don't need any restriction by a where clause based on the enclosing entity.
     * These would typically be at the topmost level, directly inside a feed rather than nested in some GTFS entity type.
     */
    public SQLColumnFetcher(String tableName, String parentJoinField, String columnName) {
        this.columnName = columnName;
        this.jdbcFetcher = new JDBCFetcher(tableName, parentJoinField);

    }

    @Override
    public List<T> get (DataFetchingEnvironment environment) {
        List<T> result = new ArrayList<>();
        // Ideally we'd only fetch one column in the wrapped row fetcher.
        for (Map<String, Object> row : jdbcFetcher.get(environment)) {
            result.add((T)row.get(columnName));
        }
        return result;
    }

}
