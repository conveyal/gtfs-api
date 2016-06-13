package com.conveyal.gtfs.api.controllers;

import com.conveyal.geojson.GeometryDeserializer;
import com.conveyal.geojson.GeometrySerializer;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Trip;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.LineString;
import spark.Request;
import spark.Response;

import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.halt;

/**
 * Controller for trip patterns.
 */
public class PatternsController {
    public static List<PatternSummary> getPatterns (Request req, Response res) {
        List<String> feeds = new ArrayList<>();

        if (req.queryParams("feed") != null) {
            for (String feedId : req.queryParams("feed").split(",")){
                if (ApiMain.feedSources.get(feedId) != null) {
                    feeds.add(feedId);
                }
            }
            if (feeds.size() == 0 || feeds.size() > 1){
                halt(400, "Must specify single valid feed id.");
            }
        }
        else{
            halt(400, "Must specify valid feed id.");
        }

        // grab the route
        if (req.queryParams("route") == null) halt(400, "Must specify route");

        if (!ApiMain.feedSources.containsKey(feeds.get(0))) {
            halt(404, "No such feed");
        }

        GTFSFeed feed = ApiMain.feedSources.get(feeds.get(0)).feed;

        String routeId = req.queryParams("route");
        if (!feed.routes.containsKey(routeId)) halt(404, "No such route");

        Route route = feed.routes.get(routeId);

        Multimap<String, String> tripsByPattern = HashMultimap.create();
        for (Trip trip : feed.trips.values()) {
            // not horribly inefficient to filter this way, there aren't that many trips
            if (!trip.route_id.equals(route.route_id)) continue;
            tripsByPattern.put(feed.tripPatternMap.get(trip.trip_id), trip.trip_id);
        }

        return tripsByPattern.asMap().entrySet().stream()
                .map(e -> new PatternSummary(feed.patterns.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    public static class PatternSummary {
        @JsonSerialize(using = GeometrySerializer.class)
        @JsonDeserialize(using = GeometryDeserializer.class)
        public LineString geometry;

        public String name;

        public List<String> orderedStops;

        public String pattern_id;

        public Collection<String> trips;

        public PatternSummary (Pattern pattern, Collection<String> trips) {
            this.geometry = pattern.geometry;
            this.name = pattern.name;
            this.orderedStops = pattern.orderedStops;
            this.pattern_id = pattern.pattern_id;
            this.trips = trips;
        }
    }
}
