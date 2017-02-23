package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.introspection.IntrospectionQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static spark.Spark.halt;

/**
 * Created by matthewc on 3/9/16.
 */
public class GraphQLController {
    // todo shared objectmapper
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLController.class);
    private static final GraphQL GRAPHQL = new GraphQL(GraphQLGtfsSchema.schema);

    public static Object get (Request req, Response res) {
        Map<String, Object> variables = null;
        String vars = req.queryParams("variables");
        String query = req.queryParams("query");

        if (vars == null && query == null) {
            return GRAPHQL.execute(IntrospectionQuery.INTROSPECTION_QUERY).getData();
        }
        try {
            variables = mapper.readValue(vars, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            LOG.warn("Error processing variable JSON", e);
            halt(404, "Malformed JSON");
        }

        ExecutionResult er = GRAPHQL.execute(query, null, null, variables);
        List<GraphQLError> errs = er.getErrors();
        if (!errs.isEmpty()) {
            res.status(400);
            return errs;
        }
        else {
            return er.getData();
        }
    }

    public static Object getSchema (Request req, Response res) {
        return GRAPHQL.execute(IntrospectionQuery.INTROSPECTION_QUERY).getData();
    }
}
