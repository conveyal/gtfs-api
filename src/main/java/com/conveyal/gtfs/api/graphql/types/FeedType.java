package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.WrappedEntityFieldFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.FeedFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.RouteFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.StatFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.StopFetcher;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static com.conveyal.gtfs.api.util.GraphQLUtil.doublee;
import static graphql.Scalars.GraphQLLong;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by landon on 10/3/16.
 */
public class FeedType {
    public static GraphQLObjectType build () {
        // TODO: add feedStats to FeedType. 
//        GraphQLObjectType feedStats = newObject()
//                .name("feedStats")
//                .description("Statistics about a feed")
//                .field(intt("headway"))
//                .field(doublee("avgSpeed"))
//                .field(intt("tripCount"))
//                .field(intt("revenueTime"))
//                .build();

        return newObject()
                .name("feed")
                .description("Provides information for a GTFS feed and access to the entities it contains")
                .field(string("feed_id"))
                .field(string("feed_publisher_name"))
                .field(string("feed_publisher_url"))
                .field(string("feed_lang"))
                .field(string("feed_version"))
                .field(newFieldDefinition()
                        .name("routes")
                        .type(new GraphQLList(new GraphQLTypeReference("route")))
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
                        .type(new GraphQLList(new GraphQLTypeReference("stop")))
                        .dataFetcher(StopFetcher::fromFeed)
                        .build()
                )
                .field(newFieldDefinition()
                        .type(GraphQLLong)
                        .name("stop_count")
                        .dataFetcher(StopFetcher::fromFeedCount)
                        .build()
                )
                .field(newFieldDefinition()
                        .name("mergedBuffer")
                        .type(lineString())
                        .description("Merged buffers around all stops in feed")
                        .dataFetcher(FeedFetcher::getMergedBuffer)
                        .build()
                )
//                .field(newFieldDefinition()
//                        .type(feedStats)
//                        .name("stats")
//                        .argument(stringArg("date"))
//                        .argument(longArg("from"))
//                        .argument(longArg("to"))
//                        .dataFetcher(StatFetcher::fromFeed)
//                        .build()
//                )
                .build();
    }
}
