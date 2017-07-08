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

    // The order here is critical. Each new type that's defined can refer to other types directly by object
    // reference or by name. Names can only be used for types that are already reachable recursively by
    // reference from the top of the schema. So you want as many direct references as you can.
    // It really seems like all this should be done automatically, maybe we should be using a text schema
    // instead of code.
    // I do wonder whether these should all be statically initialized. Doing this in a non-static context
    // in one big block with local variables, the dependencies would be checked by compiler.
    // The order:
    // Instantiate starting with leaf nodes (reverse topological sort of the dependency graph).
    // All forward references must use names and GraphQLTypeReference.
    // Additionally the tree will be explored once top-down following explicit object references, and only
    // objects reached that way will be available by name reference.
    // Another way to accomplish this would be to use name references in every definition except the top level,
    // and make a dummy declaration that will call them all to be pulled in by reference at once.

    public static GraphQLObjectType patternType = PatternType.build(); // forward ref to route, stop, trip

    public static GraphQLObjectType tripType = TripType.build(); // refers to pattern, fwd ref to stopTime

    public static GraphQLObjectType stopTimeType = StopTimeType.build(); // refers to trip

    public static GraphQLObjectType routeType = RouteType.build(); // refers to trip

    public static GraphQLObjectType stopType = StopType.build(); // refers to stopTime and route

    public static GraphQLObjectType feedType = FeedType.build(); // refers to route and stop

    private void example () {
        newObject()
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
                        // And this stuff should be grouped into a single obfject {lat:, lon:...}
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
                        .argument(multiStringArg("feed_id"))
                        .argument(multiStringArg("stop_id"))
                        .argument(multiStringArg("trip_id"))
                        .dataFetcher(StopTimeFetcher::apex)
                        .type(new GraphQLList(stopTimeType))
                        .build()
                )
                .build();
    }


    /**
     * This is the top-level query - you must always specify a feed to fetch, and then some other things inside that feed.
     */
    private static GraphQLObjectType feedQuery = newObject()
            .name("feedQuery")
            .field(newFieldDefinition()
                    .name("feed")
                    .type(GraphQLGtfsSchema.feedType)
                    // single feed namespace, otherwise route_ids and such are ambiguous
                    .argument(stringArg("namespace"))
                    .dataFetcher(new FeedFetcher())
                    .build()
            )
            .build();

    /** This is the new schema as of July 2017, where all sub-entities are wrapped in a feed. */
    public static GraphQLSchema feedBasedSchema = GraphQLSchema.newSchema().query(feedQuery).build();

}
