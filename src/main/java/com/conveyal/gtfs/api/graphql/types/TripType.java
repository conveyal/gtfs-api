package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.fetchers.MapFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.PatternFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.StopTimeFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.TripDataFetcher;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.Scalars.GraphQLInt;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by landon on 10/3/16.
 */
public class TripType {
    public static GraphQLObjectType build () {
        return newObject()
                .name("trip")
                .field(MapFetcher.field("trip_id"))
                .field(MapFetcher.field("trip_headsign"))
                .field(MapFetcher.field("trip_short_name"))
                .field(MapFetcher.field("block_id"))
                .field(MapFetcher.field("direction_id", GraphQLInt))
                .field(MapFetcher.field("route_id"))
                .field(feed())
                .field(newFieldDefinition()
                        .name("pattern")
                        .type(new GraphQLTypeReference("pattern"))
                        .dataFetcher(PatternFetcher::fromTrip)
                        .build()
                )
                .field(newFieldDefinition()
                        .name("stop_times")
                        .type(new GraphQLList(new GraphQLTypeReference("stopTime")))
                        .argument(multiStringArg("stop_id"))
                        .dataFetcher(StopTimeFetcher::fromTrip)
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
