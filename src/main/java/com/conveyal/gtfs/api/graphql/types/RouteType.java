package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.PatternFetcher;
import com.conveyal.gtfs.api.graphql.StatFetcher;
import com.conveyal.gtfs.api.graphql.TripDataFetcher;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.Scalars.GraphQLLong;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by landon on 10/3/16.
 */
public class RouteType {
    public static GraphQLObjectType build () {
        // routeStats should be modeled after com.conveyal.gtfs.stats.model.RouteStatistic
        GraphQLObjectType routeStats = newObject()
                .name("stats")
                .field(doublee("headway"))
                .build();

        return newObject()
                .name("route")
                .field(string("route_id"))
                // TODO agency
                .field(string("route_short_name"))
                .field(string("route_long_name"))
                .field(string("route_desc"))
                .field(string("route_url"))
                // TODO route_type as enum
                .field(string("route_color"))
                .field(string("route_text_color"))
                .field(newFieldDefinition()
                        .type(new GraphQLList(new GraphQLTypeReference("trip")))
                        .name("trips")
                        .dataFetcher(TripDataFetcher::fromRoute)
                        .build()
                )
                .field(newFieldDefinition()
                        .type(GraphQLLong)
                        .name("trip_count")
                        .dataFetcher(TripDataFetcher::fromRouteCount)
                        .build()
                )
                .field(newFieldDefinition()
                        .type(new GraphQLList(new GraphQLTypeReference("pattern")))
                        .name("patterns")
                        .argument(multiStringArg("stop_id"))
                        .dataFetcher(PatternFetcher::fromRoute)
                        .build()
                )
                .field(newFieldDefinition()
                        .type(GraphQLLong)
                        .name("pattern_count")
                        .dataFetcher(PatternFetcher::fromRouteCount)
                        .build()
                )
                .field(newFieldDefinition()
                        .type(routeStats)
                        .name("stats")
                        .argument(stringArg("date"))
                        .argument(longArg("from"))
                        .argument(longArg("to"))
                        .dataFetcher(StatFetcher::fromRoute)
                        .build()
                )
                .build();
    }}
