package com.conveyal.gtfs.api.graphql.fetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;

import java.util.Map;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

/**
 * This is a special data fetcher for cases where the sub-object context is exactly the same as the parent object.
 * This is useful for grouping fields together. For example, row counts of tables within a GTFS feed are conceptually
 * directly under the feed, but we group them together in a sub-object for clarity.
 */
public class SourceObjectFetcher implements DataFetcher {

    @Override
    public Map<String, Object> get(DataFetchingEnvironment dataFetchingEnvironment) {
        return dataFetchingEnvironment.getSource();
    }

}
