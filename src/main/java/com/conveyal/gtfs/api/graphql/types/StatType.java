package com.conveyal.gtfs.api.graphql.types;

import graphql.schema.GraphQLObjectType;

import static com.conveyal.gtfs.api.util.GraphQLUtil.*;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Created by landon on 10/4/16.
 */
public class StatType {
    public static GraphQLObjectType build () {
        return newObject()
                .name("stat")
                .description("An abstract statistic for a GTFS entity.")
                .field(doublee("headway"))
                .field(doublee("tripCount"))
                .field(doublee("avgSpeed"))
                .build();
    }
}
