package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import com.conveyal.gtfs.api.graphql.fetchers.MapFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.RouteFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.StatFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.StopTimeFetcher;
import graphql.schema.*;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.Scalars.GraphQLFloat;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Factory to create a GraphQL type for GTFS stops.
 */
public abstract class StopType {

    public static GraphQLObjectType build () {

        // transferPerformance should be modeled after com.conveyal.gtfs.stats.model.TransferPerformanceSummary
        GraphQLObjectType transferPerformance = newObject()
                .name("transferPerformance")
                .description("Transfer performance for a stop")
                .field(string("fromRoute"))
                .field(string("toRoute"))
                .field(intt("bestCase"))
                .field(intt("worstCase"))
                .field(intt("typicalCase"))
//                .field(newFieldDefinition()
//                        .name("missedOpportunities")
//                        .type(new GraphQLList(new GraphQLTypeReference("stopTime")))
////                        .dataFetcher(StopTimeFetcher::fromStop)
//                        .build()
//                )

//                .field(new GraphQLList(intt()))
                .build();

        // stopStats should be modeled after com.conveyal.gtfs.stats.model.StopStatistic
        GraphQLObjectType stopStats = newObject()
                .name("stopStats")
                .description("Statistics about a stop")
                .field(doublee("headway"))
                .field(intt("tripCount"))
                .field(intt("routeCount"))
                .build();

        return newObject()
                .name("stop")
                .description("A GTFS stop object")
                .field(MapFetcher.field("stop_id"))
                .field(MapFetcher.field("stop_name"))
                .field(MapFetcher.field("stop_code"))
                .field(MapFetcher.field("stop_desc"))
                .field(MapFetcher.field("stop_lon", GraphQLFloat))
                .field(MapFetcher.field("stop_lat", GraphQLFloat))
                .field(MapFetcher.field("zone_id"))
                .field(MapFetcher.field("stop_url"))
                .field(MapFetcher.field("stop_timezone"))
                .field(feed()) // FIXME feed reference
                .field(newFieldDefinition()
                        .name("stop_times")
                        .description("The list of stop_times for a stop")
                        .type(new GraphQLList(GraphQLGtfsSchema.stopTimeType))
                        .argument(stringArg("date"))
                        .argument(longArg("from"))
                        .argument(longArg("to"))
                        .dataFetcher(StopTimeFetcher::fromStop)
                        .build()
                )
                .field(newFieldDefinition()
                        .name("routes")
                        .description("The list of routes that serve a stop")
                        .type(new GraphQLList(GraphQLGtfsSchema.routeType))
                        .argument(multiStringArg("route_id"))
                        .dataFetcher(RouteFetcher::fromStop)
                        .build()
                )
                .field(newFieldDefinition()
                        .type(stopStats)
                        .name("stats")
                        .argument(stringArg("date"))
                        .argument(longArg("from"))
                        .argument(longArg("to"))
                        .dataFetcher(StatFetcher::fromStop)
                        .build()
                )
                .field(newFieldDefinition()
                        .type(new GraphQLList(transferPerformance))
                        .name("transferPerformance")
                        .argument(stringArg("date"))
                        .argument(longArg("from"))
                        .argument(longArg("to"))
                        .dataFetcher(StatFetcher::getTransferPerformance)
                        .build()
                )
                .build();
    }
}
