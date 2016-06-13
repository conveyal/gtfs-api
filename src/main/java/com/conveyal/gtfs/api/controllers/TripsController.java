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
public class TripsController {
    public static Double radius = 1.0; // default 1 km search radius
    public static Object getTrips(Request req, Response res){

        List<TripSummary> trips = new ArrayList<>();
        List<String> feeds = new ArrayList();

        if (req.queryParams("feed") != null) {

            for (String feedId : req.queryParams("feed").split(",")){
                if (ApiMain.feedSources.get(feedId) != null) {
                    feeds.add(feedId);
                }
            }
            if (feeds.size() == 0){
                return "Must specify valid feed id.";
            }
            // If feed is only param.
            else if (req.queryParams().size() == 1) {
                for (String feedId : req.queryParams("feed").split(",")){
                    GTFSFeed feed = ApiMain.feedSources.get(feedId).feed;
                    feed.trips.values().stream().map(t -> new TripSummary(t, feed)).forEach(trips::add);
                }
            }
        }
        else{
//            res.body("Must specify valid feed id.");
            return "Must specify valid feed id.";
//            halt(404, "Must specify valid feed id.");
        }


        // Continue through params
        if (req.params("id") != null) {
            GTFSFeed feed = ApiMain.feedSources.get(feeds.get(0)).feed;
            Trip t = feed.trips.get(req.params("id"));
            if(t != null) // && currentUser(req).hasReadPermission(s.projectId))
                return new TripSummary(t, feed);
            else
                halt(404, "Trip " + req.params("id") + " not found");
        }
        else if (req.queryParams("route") != null){
            String route_id = req.queryParams("route");
            System.out.println(route_id);
            GTFSFeed feed = ApiMain.feedSources.get(feeds.get(0)).feed;
            List<Trip> tripsForRoute = feed.trips.values()
                    .stream().filter(t -> t.route_id.equals(route_id)).collect(Collectors.toList());
            System.out.println(tripsForRoute.toString());
            tripsForRoute.stream().map(t -> new TripSummary(t, feed)).forEach(trips::add);
            return trips;
        }
//        // bounding box
//        else if (req.queryParams("max_lat") != null && req.queryParams("max_lon") != null && req.queryParams("min_lat") != null && req.queryParams("min_lon") != null){
//
//            Coordinate maxCoordinate = new Coordinate(Double.valueOf(req.queryParams("max_lon")), Double.valueOf(req.queryParams("max_lat")));
//            Coordinate minCoordinate = new Coordinate(Double.valueOf(req.queryParams("min_lon")), Double.valueOf(req.queryParams("min_lat")));
//            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);
//
//            for (String feedId : feeds) {
//                List<Trip> searchResults = ApiMain.feedSources.get(feedId).stopIndex.query(searchEnvelope);
//                trips.addAll(searchResults);
//            }
//            return trips;
//        }
//        // lat lon + radius
//        else if (req.queryParams("lat") != null && req.queryParams("lon") != null){
//            Coordinate latLon = new Coordinate(Double.valueOf(req.queryParams("lon")), Double.valueOf(req.queryParams("lat")));
//            if (req.queryParams("radius") != null){
//                TripsController.radius = Double.valueOf(req.queryParams("radius"));
//            }
//            Envelope searchEnvelope = GeomUtil.getBoundingBox(latLon, radius);
//            for (String feedId : feeds) {
//                List<Trip> searchResults = ApiMain.feedSources.get(feedId).stopIndex.query(searchEnvelope);
//                trips.addAll(searchResults);
//            }
//            return trips;
//        }

        return null;
    }

    public static class TripSummary {
        public Route  route;
        //public Service service;
        public String trip_id;
        public String trip_headsign;
        public String trip_short_name;
        public int    direction_id;
        public String block_id;
        public String shape_id;
        public int    bikes_allowed;
        public int    wheelchair_accessible;
        public Collection<Frequency> frequencies;

        public int start_time;
        public int trip_length_seconds;

        public TripSummary (Trip trip, GTFSFeed feed) {
            route = feed.routes.get(trip.route_id);
            //service = trip.service;
            trip_id = trip.trip_id;
            trip_headsign = trip.trip_headsign;
            trip_short_name = trip.trip_short_name;
            direction_id = trip.direction_id;
            block_id = trip.block_id;
            shape_id = trip.shape_id;
            bikes_allowed = trip.bikes_allowed;
            wheelchair_accessible = trip.wheelchair_accessible;
            frequencies = feed.getFrequencies(trip.trip_id);

            Iterable<StopTime> stopTimes = feed.getOrderedStopTimesForTrip(trip_id);

            StopTime first = null, last = null;

            for (StopTime st : stopTimes) {
                if (first == null) first = st;
                last = st;
            }

            start_time = first.arrival_time;
            trip_length_seconds = last.departure_time - first.arrival_time;
        }
    }
}