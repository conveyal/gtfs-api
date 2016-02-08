package com.conveyal.gtfs.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import spark.ResponseTransformer;

/**
 * Serve output as json.
 */
public class JsonTransformer implements ResponseTransformer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String render(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    /** set the content type */
    public void type (Request request, Response response) {
        response.type("application/json");
    }
}
