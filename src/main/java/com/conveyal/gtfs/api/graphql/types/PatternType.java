package com.conveyal.gtfs.api.graphql.types;

import com.conveyal.gtfs.api.graphql.StopFetcher;
import com.conveyal.gtfs.api.graphql.TripDataFetcher;
import com.conveyal.gtfs.api.graphql.WrappedEntityFieldFetcher;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import static com.conveyal.gtfs.api.util.GraphQLUtil.lineString;
import static com.conveyal.gtfs.api.util.GraphQLUtil.longArg;
import static com.conveyal.gtfs.api.util.GraphQLUtil.string;
import static graphql.Scalars.GraphQLLong;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by landon on 10/3/16.
 */
public class PatternType {
    public static GraphQLObjectType build () {

        return newObject()
                .name("pattern")
                .field(string("pattern_id"))
                .field(string("name"))
                .field(newFieldDefinition()
                        .name("stops")
                        .dataFetcher(StopFetcher::fromPattern)
                        .type(new GraphQLList(new GraphQLTypeReference("stop")))
                        .build()
                )
                .field(newFieldDefinition()
                        .type(GraphQLLong)
                        .name("stop_count")
                        .dataFetcher(StopFetcher::fromPatternCount)
                        .build()
                )
                .field(newFieldDefinition()
                        .type(lineString())
                        .dataFetcher(new WrappedEntityFieldFetcher("geometry"))
                        .name("geometry")
                        .build()
                )
                .field(newFieldDefinition()
                        .name("trips")
                        .type(new GraphQLList(new GraphQLTypeReference("trip")))
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
    }
}
