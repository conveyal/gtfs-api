package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Route;
import graphql.schema.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            .field(newFieldDefinition()
                    .name("pattern")
                    .type(new GraphQLTypeReference("pattern"))
                    .dataFetcher(PatternFetcher::fromTrip)
                    .build()
            )
            .field(newFieldDefinition()
                    .name("stop_times")
                    .type(new GraphQLList(stopTimeType))
                    .dataFetcher(StopTimeFetcher::fromTrip)
                    .build()
            )
            .build();

    public static GraphQLScalarType lineStringType = new GraphQLScalarType("GeoJSON", "GeoJSON", new GeoJsonCoercing());

    public static GraphQLObjectType patternType = newObject()
            .name("pattern")
            .field(string("pattern_id"))
            .field(newFieldDefinition()
                    .type(lineStringType)
                    .dataFetcher(new FieldDataFetcher("geometry"))
                    .name("geometry")
                    .build()
            )
            .field(newFieldDefinition()
                    .name("trips")
                    .type(new GraphQLList(tripType))
                    .dataFetcher(TripDataFetcher::fromPattern)
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
                    .type(new GraphQLList(patternType))
                    .name("patterns")
                    .dataFetcher(PatternFetcher::fromRoute)
                    .build()
            )
            .build();

    public static GraphQLObjectType routeQuery = newObject()
            .name("routeQuery")
            .field(newFieldDefinition()
                    .name("routes")
                    .type(new GraphQLList(routeType))
                    .argument(stringArg("route_id"))
                    .argument(stringArg("feed_id"))
                    .dataFetcher(environment -> {
                        Map<String, Object> args = environment.getArguments();

                        Collection<FeedSource> feeds;

                        if (args.get("feed_id") != null) {
                            String feedId = (String) args.get("feed_id");
                            feeds = ApiMain.feedSources.values().stream()
                                    .filter(f -> feedId.equals(f.feed.feedId))
                                    .collect(Collectors.toList());
                        } else {
                            feeds = ApiMain.feedSources.values();
                        }

                        List<Route> routes = new ArrayList<>();

                        for (FeedSource feed : feeds) {
                            if (args.get("route_id") != null) {
                                String routeId = (String) args.get("route_id");
                                if (feed.feed.routes.containsKey(routeId)) routes.add(feed.feed.routes.get(routeId));
                            }
                            else {
                                routes.addAll(feed.feed.routes.values());
                            }
                        }

                        return routes;
                    })
                    .build()
            )
            .field(newFieldDefinition()
                    .name("stops")
                    .type(new GraphQLList(stopType))
                    .argument(stringArg("feed_id"))
                    .argument(stringArg("stop_id"))
                    .dataFetcher(StopFetcher::apex)
                    .build()
            )
            .build();



    public static GraphQLSchema schema = GraphQLSchema.newSchema().query(routeQuery).build();

    public static GraphQLFieldDefinition string (String name) {
        return newFieldDefinition()
                .name(name)
                .type(GraphQLString)
                .dataFetcher(new FieldDataFetcher(name))
                .build();
    }

    public static GraphQLFieldDefinition intt (String name) {
        return newFieldDefinition()
                .name(name)
                .type(GraphQLInt)
                .dataFetcher(new FieldDataFetcher(name))
                .build();
    }

    public static GraphQLFieldDefinition doublee (String name) {
        return newFieldDefinition()
                .name(name)
                .type(GraphQLFloat)
                .dataFetcher(new FieldDataFetcher(name))
                .build();
    }

    public static GraphQLArgument stringArg (String name) {
        return newArgument()
                .name(name)
                .type(GraphQLString)
                .build();
    }

}