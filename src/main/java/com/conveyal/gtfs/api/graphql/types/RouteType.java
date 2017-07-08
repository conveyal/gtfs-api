package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.fetchers.*;
import com.sun.corba.se.impl.orbutil.graph.Graph;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by landon on 10/3/16.
 */
public class RouteType {
    public static GraphQLObjectType build () {
        // routeStats should be modeled after com.conveyal.gtfs.stats.model.RouteStatistic
        GraphQLObjectType routeStats = newObject()
                .name("routeStats")
                .description("Statistics about a route")
                .field(doublee("headway"))
                .field(doublee("avgSpeed"))
                .field(doublee("stopSpacing"))
                .build();

        return newObject()
                .name("route")
                .description("A line from a GTFS routes.txt table")
                .field(MapFetcher.field("feed_id"))
                .field(MapFetcher.field("agency_id"))
                .field(MapFetcher.field("route_id"))
                .field(MapFetcher.field("route_short_name"))
                .field(MapFetcher.field("route_long_name"))
                .field(MapFetcher.field("route_desc"))
                .field(MapFetcher.field("route_url"))
                // TODO route_type as enum or int
                .field(MapFetcher.field("route_type"))
                .field(MapFetcher.field("route_color"))
                .field(MapFetcher.field("route_text_color"))
                .field(feed())
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
                        .argument(multiStringArg("pattern_id"))
                        .argument(longArg("limit"))
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
