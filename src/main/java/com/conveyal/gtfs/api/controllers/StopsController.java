package com.conveyal.gtfs.api.controllers;


import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.api.util.GeomUtil;
import com.conveyal.gtfs.model.*;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import com.conveyal.gtfs.api.ApiMain;

import spark.Request;
import spark.Response;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;

import static spark.Spark.*;

/**
 * Created by landon on 2/4/16.
 */
public class StopsController {
    public static Double radius = 1.0; // default 1 km search radius
    public static Object getStops(Request req, Response res){

        Set<Stop> stops = new HashSet<>();
        List<String> feeds = new ArrayList();

        if (req.queryParams("feed") != null) {

            for (String feedId : req.queryParams("feed").split(",")){
                if (ApiMain.feedSources.get(feedId) != null) {
                    feeds.add(feedId);
                }
            }
            if (feeds.size() == 0){
                halt(404, "Must specify valid feed id.");
            }
            // If feed is only param.
            else if (req.params("id") == null) {
                stops = new HashSet<>();
                for (String feedId : feeds){
                    stops.addAll(ApiMain.feedSources.get(feedId).feed.stops.values());
                }
            }
        }
        else{
//            res.body("Must specify valid feed id.");
//            return "Must specify valid feed id.";
            halt(404, "Must specify valid feed id.");
        }


        // Continue through params
        if (req.params("id") != null) {
            Stop s = ApiMain.feedSources.get(feeds.get(0)).feed.stops.get(req.params("id"));
            if(s != null) // && currentUser(req).hasReadPermission(s.projectId))
                return s;
            else
                halt(404, "Stop " + req.params("id") + " not found");
        }
        // bounding box
        else if (req.queryParams("max_lat") != null && req.queryParams("max_lon") != null && req.queryParams("min_lat") != null && req.queryParams("min_lon") != null){
            stops = new HashSet<>();
            Coordinate maxCoordinate = new Coordinate(Double.valueOf(req.queryParams("max_lon")), Double.valueOf(req.queryParams("max_lat")));
            Coordinate minCoordinate = new Coordinate(Double.valueOf(req.queryParams("min_lon")), Double.valueOf(req.queryParams("min_lat")));
            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);

            for (String feedId : feeds) {
                List<Stop> searchResults = ApiMain.feedSources.get(feedId).stopIndex.query(searchEnvelope);
                stops.addAll(searchResults);
            }
            return limitStops(req, stops);
        }
        // lat lon + radius
        else if (req.queryParams("lat") != null && req.queryParams("lon") != null){
            stops = new HashSet<>();
            Coordinate latLon = new Coordinate(Double.valueOf(req.queryParams("lon")), Double.valueOf(req.queryParams("lat")));
            if (req.queryParams("radius") != null){
                StopsController.radius = Double.valueOf(req.queryParams("radius"));
            }
            Envelope searchEnvelope = GeomUtil.getBoundingBox(latLon, radius);
            for (String feedId : feeds) {
                List<Stop> searchResults = ApiMain.feedSources.get(feedId).stopIndex.query(searchEnvelope);
                stops.addAll(searchResults);
            }
            return limitStops(req, stops);
        }
        // query name
        else if (req.queryParams("name") != null){
            stops = new HashSet<>();
            System.out.println(req.queryParams("name"));

            for (String feedId : feeds) {
                System.out.println("looping feed: " + feedId);

                System.out.println("checking feed: " + feedId);

                // search query must be in upper case to match radix tree keys
//              Iterable<Stop> searchResults = ApiMain.feedSources.get(feedId).stopTree.getValuesForClosestKeys(req.queryParams("name").toUpperCase());
//                Iterable<Stop> searchResults = ApiMain.feedSources.get(feedId).stopTree.getValuesForKeysStartingWith(req.queryParams("name").toUpperCase());
                Iterable<Stop> searchResults = ApiMain.feedSources.get(feedId).stopTree.getValuesForKeysContaining(req.queryParams("name").toUpperCase());
//                Iterable<Stop> searchResults = ApiMain.feedSources.get(feedId).stopTree.getValues
//              stops = Iterables.toArray(searchResults, Stop.class);
                System.out.println(Iterables.size(searchResults));
                for (Stop stop : searchResults) {
                    System.out.println(stop.stop_name);
                    stops.add(stop);
                }
            }
            return limitStops(req, stops);
        }
        // query for route_id (i.e., get all stops that exist in patterns for a given route)
        else if (req.queryParams("route") != null){
            return getStopsForRoute(req, feeds);
        }

        return limitStops(req, stops);
    }
    
    public static Set<Stop> getStopsForRoute(Request req, List<String> feeds){
        if (req.queryParams("route") != null){
            String routeId = req.queryParams("route");
            System.out.println(routeId);
            Set<Stop> stops = new HashSet<>();
            // loop through feeds
            for (String feedId : feeds) {
                Set<String> stopIds = new HashSet<>();
                FeedSource source = ApiMain.feedSources.get(feedId);

                // loop through patterns, check for route and return pattern stops
                for (Pattern pattern : source.feed.patterns.values()) {
                    for (String routeIdInPattern : pattern.associatedRoutes){
                        if (routeId.equals(routeIdInPattern)){
                            stopIds.addAll(pattern.orderedStops);
                            break;
                        }
                    }
                }

                for (String stopId : stopIds) {
                    Stop stop = ApiMain.feedSources.get(feedId).feed.stops.get(stopId);
                    System.out.println(stopId);
                    stops.add(stop);
                }
            }
            return limitStops(req, stops);
        }
        return null;
    }

    public static Set<Stop> limitStops(Request req, Set<Stop> stops) {
        if (req.queryParams("limit") != null)
            return stops.stream().limit(Long.valueOf(req.queryParams("limit"))).collect(Collectors.toSet());
        else
            return stops;
    }

}
