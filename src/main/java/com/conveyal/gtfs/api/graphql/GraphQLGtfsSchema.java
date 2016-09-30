package com.conveyal.gtfs.api.graphql;

import graphql.schema.*;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.Scalars.*;

/**
 * Created by matthewc on 3/9/16.
 */
public class GraphQLGtfsSchema {
    public static GraphQLObjectType stopType = newObject()
            .name("stop")
            .field(string("stop_id"))
            .field(string("stop_name"))
            .field(string("stop_code"))
            .field(string("stop_desc"))
            .field(doublee("stop_lon"))
            .field(doublee("stop_lat"))
            .field(string("zone_id"))
            .field(string("stop_url"))
            .field(string("stop_timezone"))
            .field(newFieldDefinition()
                    .name("routes")
                    .type(new GraphQLList(new GraphQLTypeReference("route")))
                    .argument(multiStringArg("route_id"))
                    .dataFetcher(RouteFetcher::fromStop)
                    .build()
            )
            .build();

    public static GraphQLObjectType stopTimeType = newObject()
            .name("stopTime")
            .field(intt("arrival_time"))
            .field(intt("departure_time"))
            .field(intt("stop_sequence"))
            .field(string("stop_id"))
            .field(string("stop_headsign"))
            .field(doublee("shape_dist_traveled"))
            .build();

    public static GraphQLObjectType tripType = newObject()
            .name("trip")
            .field(string("trip_id"))
            .field(string("trip_headsign"))
            .field(string("trip_short_name"))
            .field(string("block_id"))
            .field(intt("direction_id"))
            .field(newFieldDefinition()
                    .name("pattern")
                    .type(new GraphQLTypeReference("pattern"))
                    .dataFetcher(PatternFetcher::fromTrip)
                    .build()
            )
            .field(newFieldDefinition()
                    .name("stop_times")
                    .type(new GraphQLList(stopTimeType))
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

    public static GraphQLScalarType lineStringType = new GraphQLScalarType("GeoJSON", "GeoJSON", new GeoJsonCoercing());

    public static GraphQLObjectType patternType = newObject()
            .name("pattern")
            .field(string("pattern_id"))
            .field(string("name"))
            .field(newFieldDefinition()
                    .name("stops")
                    .dataFetcher(StopFetcher::fromPattern)
                    .type(new GraphQLList(stopType))
                    .build()
            )
            .field(newFieldDefinition()
                    .type(GraphQLLong)
                    .name("stop_count")
                    .dataFetcher(StopFetcher::fromPatternCount)
                    .build()
            )
            .field(newFieldDefinition()
                    .type(lineStringType)
                    .dataFetcher(new WrappedEntityFieldFetcher("geometry"))
                    .name("geometry")
                    .build()
            )
            .field(newFieldDefinition()
                    .name("trips")
                    .type(new GraphQLList(tripType))
                    .dataFetcher(TripDataFetcher::fromPattern)
                    .argument(longArg("begin_time"))
                    .argument(longArg("end_time"))
                    .build()
            )
            .field(newFieldDefinition()
                    .type(GraphQLLong)
                    .name("trip_count")
                    .dataFetcher(TripDataFetcher::fromPatternCount)
                    .build()
            )
            .build();

    public static GraphQLObjectType routeType = newObject()
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
                    .type(new GraphQLList(tripType))
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
                    .type(new GraphQLList(patternType))
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
            .build();

    public static GraphQLObjectType feedType = newObject()
            .name("feed")
            .field(string("feed_id"))
            .field(string("feed_publisher_name"))
            .field(string("feed_publisher_url"))
            .field(string("feed_lang"))
            .field(string("feed_version"))
            .field(newFieldDefinition()
                    .name("routes")
                    .type(new GraphQLList(routeType))
                    .argument(multiStringArg("route_id"))
                    .dataFetcher(RouteFetcher::fromFeed)
                    .build()
            )
            .field(newFieldDefinition()
                    .type(GraphQLLong)
                    .name("route_count")
                    .dataFetcher(RouteFetcher::fromFeedCount)
                    .build()
            )
            .field(newFieldDefinition()
                    .name("stops")
                    .type(new GraphQLList(stopType))
                    .argument(floatArg("min_lat"))
                    .argument(floatArg("min_lon"))
                    .argument(floatArg("max_lat"))
                    .argument(floatArg("max_lon"))
                    .dataFetcher(StopFetcher::fromFeed)
                    .build()
            )
            .field(newFieldDefinition()
                    .type(GraphQLLong)
                    .name("stop_count")
                    .dataFetcher(StopFetcher::fromFeedCount)
                    .build()
            )

            .build();

    public static GraphQLObjectType rootQuery = newObject()
            .name("rootQuery")
            .field(newFieldDefinition()
                    .name("routes")
                    .type(new GraphQLList(routeType))
                    .argument(multiStringArg("route_id"))
                    .argument(multiStringArg("feed_id"))
                    .dataFetcher(RouteFetcher::apex)
                    .build()
            )
            .field(newFieldDefinition()
                    .name("stops")
                    .type(new GraphQLList(stopType))
                    .argument(multiStringArg("feed_id"))
                    .argument(multiStringArg("stop_id"))
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
            .build();



    public static GraphQLSchema schema = GraphQLSchema.newSchema().query(rootQuery).build();

    public static GraphQLFieldDefinition string (String name) {
        return newFieldDefinition()
                .name(name)
                .type(GraphQLString)
                .dataFetcher(new WrappedEntityFieldFetcher(name))
                .build();
    }

    public static GraphQLFieldDefinition intt (String name) {
        return newFieldDefinition()
                .name(name)
                .type(GraphQLInt)
                .dataFetcher(new WrappedEntityFieldFetcher(name))
                .build();
    }

    public static GraphQLFieldDefinition doublee (String name) {
        return newFieldDefinition()
                .name(name)
                .type(GraphQLFloat)
                .dataFetcher(new WrappedEntityFieldFetcher(name))
                .build();
    }

    public static GraphQLArgument stringArg (String name) {
        return newArgument()
                .name(name)
                .type(GraphQLString)
                .build();
    }

    public static GraphQLArgument multiStringArg (String name) {
        return newArgument()
                .name(name)
                .type(new GraphQLList(GraphQLString))
                .build();
    }

    public static GraphQLArgument floatArg (String name) {
        return newArgument()
                .name(name)
                .type(GraphQLFloat)
                .build();
    }

    public static GraphQLArgument longArg (String name) {
        return newArgument()
                .name(name)
                .type(GraphQLLong)
                .build();
    }
}
