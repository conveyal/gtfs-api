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

import com.google.common.collect.Maps;

import static spark.Spark.*;

/**
 * Created by landon on 2/4/16.
 */
public class TripsController {
    public static Double radius = 1.0; // default 1 km search radius
    public static Object getTrips(Request req, Response res){

        List<Trip> trips = new ArrayList<>();
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
                    trips.addAll(ApiMain.feedSources.get(feedId).feed.trips.values());
                }
                return trips;
            }
        }
        else{
//            res.body("Must specify valid feed id.");
            return "Must specify valid feed id.";
//            halt(404, "Must specify valid feed id.");
        }


        // Continue through params
        if (req.params("id") != null) {
            Trip t = ApiMain.feedSources.get(feeds.get(0)).feed.trips.get(req.params("id"));
            if(t != null) // && currentUser(req).hasReadPermission(s.projectId))
                return t;
            else
                halt(404, "Trip " + req.params("id") + " not found");
        }
//        else if (req.queryParams("route") != null){
//            for (String feedId : feeds) {
//                List<Trip> tripsForFeed = ApiMain.feedSources.get(feedId).feed.trips.values();
//                tripsForFeed.
//                trips.addAll(searchResults);
//            }
//            return trips;
//        }
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

}