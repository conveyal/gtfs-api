package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;

/**
 * Created by matthewc on 3/9/16.
 */
public class GraphQLController {
    public static Object get (Request req, Response res) {
        ExecutionResult er = new GraphQL(GraphQLGtfsSchema.schema).execute(req.queryParams("query"));
        List<GraphQLError> errs = er.getErrors();
        if (!errs.isEmpty()) {
            res.status(400);
            return errs;
        }
        else {
            return er.getData();
        }
    }
}
