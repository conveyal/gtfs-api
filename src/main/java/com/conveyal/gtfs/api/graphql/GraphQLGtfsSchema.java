package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.graphql.fetchers.*;
import com.conveyal.gtfs.api.graphql.types.FeedType;
import com.conveyal.gtfs.api.graphql.types.PatternType;
import com.conveyal.gtfs.api.graphql.types.RouteType;
import com.conveyal.gtfs.api.graphql.types.StopTimeType;
import com.conveyal.gtfs.api.graphql.types.StopType;
import com.conveyal.gtfs.api.graphql.types.TripType;
import graphql.schema.*;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * This defines the types for our GraphQL API, and wires them up to functions that can fetch data from JDBC databases.
 */
public class GraphQLGtfsSchema {

    public static GraphQLObjectType stopType = StopType.build();

    public static GraphQLObjectType stopTimeType = StopTimeType.build();

    public static GraphQLObjectType tripType = TripType.build();

    public static GraphQLObjectType patternType = PatternType.build();

    public static GraphQLObjectType routeType = RouteType.build();

    public static GraphQLObjectType feedType = FeedType.build();

    public static GraphQLObjectType rootQuery = newObject()
            .name("rootQuery")
            .description("Root level query for routes, stops, feeds, patterns, trips, and stopTimes within GTFS feeds.")
            .field(newFieldDefinition()
                    .name("routes")
                    .description("List of GTFS routes optionally filtered by route_id. feed_id must be specified.")
                    .type(new GraphQLList(routeType))
                    .argument(multiStringArg("feed_id"))
                    .argument(multiStringArg("route_id"))
                    .dataFetcher(new JDBCFetcher("routes"))
                    .build()
            )
            .field(newFieldDefinition()
                    .name("stops")
                    .type(new GraphQLList(stopType))
                    // We actually want to accept a list of groups of these parameters
                    // {feed_id: xyz, stop_id: [1,2,3]}, {feed_id: xyz, stop_id: [1,2,3]}
                    // In fact maybe we shouldnt even allow arrays inside these, just one feedId and one stop or route ID.
                    .argument(multiStringArg("feed_id"))
                    .argument(multiStringArg("stop_id"))
                    .argument(multiStringArg("route_id"))
                    .argument(multiStringArg("pattern_id"))
                    // And this stuff should be grouped into a single object {lat:, lon:...}
                    .argument(floatArg("lat"))
                    .argument(floatArg("lon"))
                    .argument(floatArg("radius"))
                    .argument(floatArg("max_lat"))
                    .argument(floatArg("max_lon"))
                    .argument(floatArg("min_lat"))
                    .argument(floatArg("min_lon"))
                    .dataFetcher(StopFetcher::apex)
                    .build()
            )
            .field(newFieldDefinition()
                    .name("feeds")
                    .argument(multiStringArg("feed_id"))
                    .dataFetcher(FeedFetcher::apex)
                    .type(new GraphQLList(feedType))
                    .build()
            )
            // TODO: determine if there's a better way to get at the refs for patterns, trips, and stopTimes than injecting them at the root.
            .field(newFieldDefinition()
                    .name("patterns")
                    .type(new GraphQLList(patternType))
                    .argument(multiStringArg("feed_id"))
                    .argument(multiStringArg("pattern_id"))
                    .argument(floatArg("lat"))
                    .argument(floatArg("lon"))
                    .argument(floatArg("radius"))
                    .argument(floatArg("max_lat"))
                    .argument(floatArg("max_lon"))
                    .argument(floatArg("min_lat"))
                    .argument(floatArg("min_lon"))
                    .dataFetcher(PatternFetcher::apex)
                    .build()
            )
            .field(newFieldDefinition()
                    .name("trips")
                    .argument(multiStringArg("feed_id"))
                    .argument(multiStringArg("trip_id"))
                    .argument(multiStringArg("route_id"))
                    .dataFetcher(new JDBCFetcher("trips"))
                    .type(new GraphQLList(tripType))
                    .build()
            )
            .field(newFieldDefinition()
                    .name("stopTimes")
                    .argument(multiStringArg("stop_id"))
                    .argument(multiStringArg("trip_id"))
                    .dataFetcher(StopTimeFetcher::apex)
                    .type(new GraphQLList(stopTimeType))
                    .build()
            )
            .build();

    public static GraphQLSchema schema = GraphQLSchema.newSchema().query(rootQuery).build();

}
