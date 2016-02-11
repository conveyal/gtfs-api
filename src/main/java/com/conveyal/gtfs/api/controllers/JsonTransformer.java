package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.model.Service;
import com.conveyal.gtfs.model.Shape;
import com.conveyal.gtfs.model.Trip;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import spark.ResponseTransformer;

import java.util.Map;

/**
 * Serve output as json.
 */

public class JsonTransformer implements ResponseTransformer {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String render(Object o) throws Exception {
//        objectMapper.getSerializationConfig().addMixInAnnotations(Trip.class, TripMixIn.class);
//        objectMapper.getDeserializationConfig().addMixInAnnotations(Trip.class, TripMixIn.class);
        objectMapper.addMixIn(Trip.class, TripMixIn.class);
        return objectMapper.writeValueAsString(o);
    }

    /** set the content type */
    public void type (Request request, Response response) {
        response.type("application/json");
    }
    public abstract class TripMixIn {
        @JsonIgnore public Map<Integer, Shape> shape_points;
        @JsonIgnore public Service service;
    }
}
