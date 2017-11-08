package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import com.conveyal.gtfs.api.graphql.fetchers.TripDataFetcher;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Factory to create a GraphQL type for GTFS stop_times.
 */
public abstract class StopTimeType {
    public static GraphQLObjectType build () {
        return newObject()
                .name("stopTime")
                .field(intt("arrival_time"))
                .field(intt("departure_time"))
                .field(intt("stop_sequence"))
                .field(string("stop_id"))
                .field(string("stop_headsign"))
                .field(doublee("shape_dist_traveled"))
                .field(feed())
                .field(newFieldDefinition()
                        .name("trip")
                        .type(GraphQLGtfsSchema.tripType)
                        .dataFetcher(TripDataFetcher::fromStopTime)
                        .argument(stringArg("date"))
                        .argument(longArg("from"))
                        .argument(longArg("to"))
                        .build()
                )
                .build();
    }
}
