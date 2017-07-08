package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.conveyal.gtfs.api.graphql.GraphQLGtfsSchema.*;
import static spark.Spark.halt;

/**
 * This Spark Controller contains methods to provide HTTP responses to GraphQL queries, including a query for the
 * GraphQL schema.
 */
public class GraphQLController {
    // todo shared objectmapper
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(GraphQLController.class);

    // TODO Is it correct to share one of these objects between many instances? Is it supposed to be long-lived or threadsafe?
    // Analysis-backend creates a new GraphQL object on every request.
    // Schema used to be coming from GraphQlGtfsSchema but we're re-doing it using the GraphQlController from analysis-backend.
    private static final GraphQL GRAPHQL = new GraphQL(GraphQLGtfsSchema.feedBasedSchema);

    /**
     * A Spark Controller that responds to a GraphQL query in HTTP GET query parameters.
     */
    public static Object get (Request request, Response response) {
        Map<String, Object> variables = null;
        String varsJson = request.queryParams("variables");
        String queryJson = request.queryParams("query");
        return doQuery(varsJson, queryJson, response);
    }

    /**
     * A Spark Controller that responds to a GraphQL query in an HTTP POST body.
     */
    public static Object post (Request req, Response res) {
        JsonNode node = null;
        try {
            node = mapper.readTree(req.body());
        } catch (IOException e) {
            LOG.warn("Error processing POST body JSON", e);
            halt(400, "Malformed JSON");
        }
        String vars = node.get("variables").asText();
        String query = node.get("query").asText();
        return doQuery(vars, query, res);
    }

    private static Object doQuery (String varsJson, String queryJson, Response response) {
        long startTime = System.currentTimeMillis();
        if (varsJson == null && queryJson == null) {
            return GRAPHQL.execute(IntrospectionQuery.INTROSPECTION_QUERY).getData();
        }
        try {
            Map<String, Object> variables = mapper.readValue(varsJson, new TypeReference<Map<String, Object>>(){});
            ExecutionResult er = GRAPHQL.execute(queryJson, null, null, variables);
            List<GraphQLError> errs = er.getErrors();
            if (!errs.isEmpty()) {
                response.status(400);
                return errs;
            } else {
                long endTime = System.currentTimeMillis();
                LOG.info("Query took {} msec", endTime - startTime);
                return er.getData();
            }
        } catch (IOException e) {
            LOG.warn("Error processing variable JSON", e);
            halt(404, "Malformed JSON");
        }
        return null;
    }


    /**
     * A Spark Controller that returns the GraphQL schema.
     */
    public static Object getSchema (Request req, Response res) {
        return GRAPHQL.execute(IntrospectionQuery.INTROSPECTION_QUERY).getData();
    }


}
