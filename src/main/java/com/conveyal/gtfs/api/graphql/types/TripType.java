package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import com.conveyal.gtfs.api.graphql.fetchers.*;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.Scalars.GraphQLInt;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Factory to create a GraphQL type for GTFS trips.
 */
public abstract class TripType {
    public static GraphQLObjectType build () {
        return newObject()
                .name("trip")
                .field(MapFetcher.field("trip_id"))
                .field(MapFetcher.field("trip_headsign"))
                .field(MapFetcher.field("trip_short_name"))
                .field(MapFetcher.field("block_id"))
                .field(MapFetcher.field("direction_id", GraphQLInt))
                .field(MapFetcher.field("route_id"))
                // TODO add patterns
                .field(newFieldDefinition()
                        .name("stop_times")
                        .type(new GraphQLTypeReference("stopTime")) // forward reference
                        .argument(multiStringArg("stop_id"))
                        .dataFetcher(new JDBCFetcher("stop_times"))
                        .build()
                )
                // some pseudo-fields to reduce the amount of data that has to be fetched
                .field(newFieldDefinition()
                        .name("start_time")
                        .type(GraphQLInt)
                        .dataFetcher(TripDataFetcher::getStartTime)
                        .build()
                )
                .field(newFieldDefinition()
                        .name("duration")
                        .type(GraphQLInt)
                        .dataFetcher(TripDataFetcher::getDuration)
                        .build()
                )
                .build();
    }
}
