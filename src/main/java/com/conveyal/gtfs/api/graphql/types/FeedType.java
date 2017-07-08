package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import com.conveyal.gtfs.api.graphql.WrappedEntityFieldFetcher;
import com.conveyal.gtfs.api.graphql.fetchers.*;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import static com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema.routeType;
import static com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema.stopType;
import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static com.conveyal.gtfs.api.util.GraphQLUtil.doublee;
import static graphql.Scalars.GraphQLLong;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Factory to create a GraphQL type representing individual GTFS files that have been loaded.
 */
public abstract class FeedType {

    // This takes some cues from Matt's implementation in analysis-backend GraphQLController.
    // new field definitions use .type(new GraphQLList(routeType))
    // old ones use .type(new GraphQLList(new GraphQLTypeReference("route")))
    // Why? the GraphQLTypeReference appears to only be intended for building self-referencing types.
    // It appears that there are cycles in the type references. Initialization does not complete
    // because some of the fields refer back to themselves via reference chains, and find themselves null.
    public static GraphQLObjectType build() {
        return newObject()
                .name("feed")
                .description("Provides information for a GTFS feed and access to the entities it contains")
                .field(MapFetcher.field("feed_id"))
                .field(MapFetcher.field("feed_publisher_name"))
                .field(MapFetcher.field("feed_publisher_url"))
                .field(MapFetcher.field("feed_lang"))
                .field(MapFetcher.field("feed_version"))
                .field(MapFetcher.field("namespace"))
                .field(MapFetcher.field("filename"))
                .field(MapFetcher.field("loaded_date"))
//            .field(newFieldDefinition()
//                    .name("checksum")
//                    .type(GraphQLLong)
//                    .dataFetcher(env -> ((WrappedFeedInfo) env.getSource()).checksum)
//                    .build()
//            )
                .field(newFieldDefinition()
                        .name("routes")
                        .type(new GraphQLList(GraphQLGtfsSchema.routeType))
                        .argument(stringArg("namespace"))
                        .argument(multiStringArg("route_id"))
                        .dataFetcher(new JDBCFetcher("routes"))
                        .build()
                )
//                .field(newFieldDefinition()
//                        .type(GraphQLLong)
//                        .name("route_count")
//                        .dataFetcher(RouteFetcher::fromFeedCount)
//                        .build()
//                )
                .field(newFieldDefinition()
                        .name("stops")
                        .type(new GraphQLList(GraphQLGtfsSchema.stopType))
                        .argument(stringArg("namespace"))
                        .argument(multiStringArg("stop_id"))
                        .dataFetcher(new JDBCFetcher("stops"))
                        .build()
                )
//                .field(newFieldDefinition()
//                        .type(GraphQLLong)
//                        .name("stop_count")
//                        .dataFetcher(StopFetcher::fromFeedCount)
//                        .build()
//                )
                // Should we really be doing this by adding GraphQL fields?
//                .field(newFieldDefinition()
//                        .name("mergedBuffer")
//                        .type(lineString())
//                        .description("Merged buffers around all stops in feed")
//                        .dataFetcher(FeedFetcher::getMergedBuffer)
//                        .build()
//                )
                // What is this for?
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
