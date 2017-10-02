package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.graphql.fetchers.*;
import com.conveyal.gtfs.loader.StringField;
import graphql.Scalars;
import graphql.schema.*;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
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


    // The old types are defined in separate class files. I'm defining new ones here.

    // by using static fields to hold these types, backward references are enforced. a few forward references are inserted explicitly.

    // Represents rows from trips.txt
    public static final GraphQLObjectType tripType = newObject()
            .name("trip")
            .field(MapFetcher.field("trip_id"))
            .field(MapFetcher.field("trip_headsign"))
            .field(MapFetcher.field("trip_short_name"))
            .field(MapFetcher.field("block_id"))
            .field(MapFetcher.field("direction_id", GraphQLInt))
            .field(MapFetcher.field("route_id"))
            // TODO add patterns
//            .field(newFieldDefinition()
//                    .name("stop_times")
//                    .type(new GraphQLTypeReference("stopTime")) // forward reference as yet undefined stopTimeType
//                    .argument(multiStringArg("stop_id"))
//                    .dataFetcher(new JDBCFetcher("stop_times")) // Add param for where-clause in parent object
//                    .build()
//            )
//            // some pseudo-fields to reduce the amount of data that has to be fetched over GraphQL to summarize
//            .field(newFieldDefinition()
//                    .name("start_time")
//                    .type(GraphQLInt)
//                    .dataFetcher(TripDataFetcher::getStartTime)
//                    .build()
//            )
//            .field(newFieldDefinition()
//                    .name("duration")
//                    .type(GraphQLInt)
//                    .dataFetcher(TripDataFetcher::getDuration)
//                    .build()
//            )
            .build();


    // Represents rows from stop_times.txt
    public static final GraphQLObjectType stopTimeType = newObject().name("stopTime")
            .field(intt("arrival_time"))
            .field(intt("departure_time"))
            .field(intt("stop_sequence"))
            .field(string("stop_id"))
            .field(string("stop_headsign"))
            .field(doublee("shape_dist_traveled"))
//            .field(feed())
//            .field(newFieldDefinition()
//                .name("trip")
//                .type(GraphQLGtfsSchema.tripType)
//                .dataFetcher(TripDataFetcher::fromStopTime)
//                .argument(stringArg("date"))
//                .argument(longArg("from"))
//                .argument(longArg("to"))
//                .build()
//            )
            .build();


    // Represents rows from routes.txt
    public static final GraphQLObjectType routeType = newObject().name("route")
            .description("A line from a GTFS routes.txt table")
            .field(MapFetcher.field("line_number", Scalars.GraphQLInt))
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
//            .field(newFieldDefinition()
//                    .type(new GraphQLList(GraphQLGtfsSchema.tripType))
//                    .name("trips")
//                    .dataFetcher(new JDBCFetcher("trips"))
//                    .build()
//            )
//            .field(newFieldDefinition()
//                    .type(GraphQLLong)
//                    .name("trip_count")
//                    .dataFetcher(TripDataFetcher::fromRouteCount) // TODO proper generic SQL row counter "select count(*) from X where Y = Z"
//                    .build())
            .build();

    // Represents rows from stops.txt
    // Contains a reference to stopTimeType and routeType
    public static final GraphQLObjectType stopType = newObject().name("stop")
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
//            .field(newFieldDefinition()
//                    .name("stop_times")
//                    .description("The list of stop_times for a stop")
//                    .type(new GraphQLList(GraphQLGtfsSchema.stopTimeType))
//                    .argument(stringArg("date"))
//                    .argument(longArg("from"))
//                    .argument(longArg("to"))
//                    .dataFetcher(StopTimeFetcher::fromStop)
//                    .build()
//            )
//            .field(newFieldDefinition()
//                    .name("routes")
//                    .description("The list of routes that serve a stop")
//                    .type(new GraphQLList(GraphQLGtfsSchema.routeType))
//                    .argument(multiStringArg("route_id"))
//                    .dataFetcher(RouteFetcher::fromStop)
//                    .build()
//            )
            .build();

    /**
     * The GraphQL API type representing entries in the table of errors encountered while loading or validating a feed.
     */
    public static GraphQLObjectType validationErrorType = newObject().name("validationError")
            .description("An error detected when loading or validating a feed.")
            .field(MapFetcher.field("error_id", GraphQLInt))
            .field(MapFetcher.field("error_type"))
            .field(MapFetcher.field("entity_type"))
            .field(MapFetcher.field("line_number", GraphQLInt))
            .field(MapFetcher.field("entity_id"))
            .field(MapFetcher.field("entity_sequence", GraphQLInt))
            .field(MapFetcher.field("bad_value"))
            .build();

    /**
     * The GraphQL API type representing counts of rows in the various GTFS tables.
     * The context here for fetching subfields is the feedType. A special dataFetcher is used to pass that identical
     * context down.
     */
    public static GraphQLObjectType rowCountsType = newObject().name("rowCounts")
            .description("Counts of rows in the various GTFS tables.")
            .field(RowCountFetcher.field("stops"))
            .field(RowCountFetcher.field("trips"))
            .field(RowCountFetcher.field("routes"))
            .field(RowCountFetcher.field("stop_times"))
            .field(RowCountFetcher.field("agency"))
            .field(RowCountFetcher.field("calendar"))
            .field(RowCountFetcher.field("calendar_dates"))
            .field(RowCountFetcher.field("errors"))
            .build();

    /**
     * GraphQL does not have a type for arbitrary maps (String -> X). Such maps must be expressed as a list of
     * key-value pairs. This is probably intended to protect us from ourselves (sending untyped data) but it just
     * leads to silly workarounds like this.
     */
    public static GraphQLObjectType errorCountType = newObject().name("errorCount")
            .description("Quantity of validation errors of a specific type.")
            .field(string("type"))
            .field(intt("count"))
            .build();

    /**
     * The GraphQL API type representing entries in the top-level table listing all the feeds imported into a gtfs-api
     * database, and with sub-fields for each table of GTFS entities within a single feed.
     */
    public static final GraphQLObjectType feedType = newObject().name("feedVersion")
            // First, the fields present in the top level table.
            .field(MapFetcher.field("namespace"))
            .field(MapFetcher.field("feed_id"))
            .field(MapFetcher.field("feed_version"))
            .field(MapFetcher.field("filename"))
            .field(MapFetcher.field("md5"))
            .field(MapFetcher.field("sha1"))
            // A field containing row counts for every table.
            .field(newFieldDefinition()
                .name("row_counts")
                .type(rowCountsType)
                .dataFetcher(new SourceObjectFetcher())
                .build())
            // A field containing counts for each type of error independently.
            .field(newFieldDefinition()
                .name("error_counts")
                .type(new GraphQLList(errorCountType))
                .dataFetcher(new ErrorCountFetcher())
                .build())
            // Then the fields for the sub-tables within the feed.
            .field(newFieldDefinition()
                .name("errors")
                .type(new GraphQLList(validationErrorType))
                .argument(stringArg("namespace"))
                .argument(multiStringArg("error_type"))
                .argument(intArg("limit"))
                .argument(intArg("offset"))
                .dataFetcher(new JDBCFetcher("errors"))
                .build()
            )
            .field(newFieldDefinition()
                .name("routes")
                .type(new GraphQLList(GraphQLGtfsSchema.routeType))
                .argument(stringArg("namespace"))
                .argument(multiStringArg("route_id"))
                .argument(intArg("limit"))
                .argument(intArg("offset"))
                .dataFetcher(new JDBCFetcher("routes"))
                .build()
            )
            .field(newFieldDefinition()
                .name("stops")
                .type(new GraphQLList(GraphQLGtfsSchema.stopType))
                .argument(stringArg("namespace"))
                .argument(multiStringArg("stop_id"))
                .argument(intArg("limit"))
                .argument(intArg("offset"))
                .dataFetcher(new JDBCFetcher("stops"))
                .build()
            )
            .field(newFieldDefinition()
                .name("trips")
                .type(new GraphQLList(GraphQLGtfsSchema.tripType))
                .argument(stringArg("namespace"))
                .argument(multiStringArg("trip_id"))
                .argument(multiStringArg("route_id"))
                .argument(intArg("limit"))
                .argument(intArg("offset"))
                .dataFetcher(new JDBCFetcher("trips"))
                .build()
            )
            .field(newFieldDefinition()
                .name("stop_times")
                .type(new GraphQLList(GraphQLGtfsSchema.stopTimeType))
                .argument(stringArg("namespace"))
                .argument(intArg("limit"))
                .argument(intArg("offset"))
                .dataFetcher(new JDBCFetcher("stop_times"))
                .build()
            )
            .build();

    /**
     * This is the top-level query - you must always specify a feed to fetch, and then some other things inside that feed.
     * TODO decide whether to call this feedVersion or feed within gtfs-lib context.
     */
    private static GraphQLObjectType feedQuery = newObject()
            .name("feedQuery")
            .field(newFieldDefinition()
                .name("feed")
                .type(feedType)
                // We scope to a single feed namespace, otherwise GTFS entity IDs are ambiguous.
                .argument(stringArg("namespace"))
                .argument(multiStringArg("error_type"))
                .dataFetcher(new FeedFetcher())
                .build()
            )
            .build();

    /**
     * This is the new schema as of July 2017, where all sub-entities are wrapped in a feed.
     * Because all of these fields are static (ugh) this must be declared after the feedQuery it references.
     */
    public static final GraphQLSchema feedBasedSchema = GraphQLSchema.newSchema().query(feedQuery).build();

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



}
