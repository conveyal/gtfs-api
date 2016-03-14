package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.util.GeomUtil;
import com.conveyal.gtfs.model.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import com.conveyal.gtfs.api.ApiMain;

import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.common.collect.Maps;
import java.util.Map.Entry;

import static spark.Spark.*;
import static spark.Spark.halt;

/**
 * Created by landon on 2/4/16.
 */
public class RoutesController {
    public static Double radius = 1.0; // default 1 km search radius
    public static Object getRoutes(Request req, Response res){

        List<Route> routes= new ArrayList<>();
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
                    routes.addAll(ApiMain.feedSources.get(feedId).feed.routes.values());
                }
                return routes;
            }
        }
        else{
//            res.body("Must specify valid feed id.");
            return "Must specify valid feed id.";
//            halt(404, "Must specify valid feed id.");
        }

        // get specific route
        if (req.params("id") != null) {
            Route r = ApiMain.feedSources.get(feeds.get(0)).feed.routes.get(req.params("id"));
            if(r != null) // && currentUser(req).hasReadPermission(s.projectId))
                return r;
            else
                halt(404, "Route " + req.params("id") + " not found");
        }
        // bounding box
        else if (req.queryParams("max_lat") != null && req.queryParams("max_lon") != null && req.queryParams("min_lat") != null && req.queryParams("min_lon") != null){
            Coordinate maxCoordinate = new Coordinate(Double.valueOf(req.queryParams("max_lon")), Double.valueOf(req.queryParams("max_lat")));
            Coordinate minCoordinate = new Coordinate(Double.valueOf(req.queryParams("min_lon")), Double.valueOf(req.queryParams("min_lat")));
            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);
            for (String feedId : feeds) {
                List<Route> searchResults = ApiMain.feedSources.get(feedId).routeIndex.query(searchEnvelope);
                routes.addAll(searchResults);
            }
            return routes;
        }
        // lat lon + radius
        else if (req.queryParams("lat") != null && req.queryParams("lon") != null){
            Coordinate latLon = new Coordinate(Double.valueOf(req.queryParams("lon")), Double.valueOf(req.queryParams("lat")));
            if (req.queryParams("radius") != null){
                StopsController.radius = Double.valueOf(req.queryParams("radius"));
            }
            Envelope searchEnvelope = GeomUtil.getBoundingBox(latLon, radius);

            for (String feedId : feeds) {
                List<Route> searchResults = ApiMain.feedSources.get(feedId).routeIndex.query(searchEnvelope);
                routes.addAll(searchResults);
            }
            return routes;
        }
        else if (req.queryParams("name") != null){
            System.out.println(req.queryParams("name"));

            for (String feedId : feeds) {
                System.out.println("looping feed: " + feedId);

                // Check if feed is specified in feed sources requested
                // TODO: Check if user has access to feed source? (Put this in the before call.)
//                if (Arrays.asList(feeds).contains(entry.getKey())) {
                System.out.println("checking feed: " + feedId);

                // search query must be in upper case to match radix tree keys
                Iterable<Route> searchResults = ApiMain.feedSources.get(feedId).routeTree.getValuesForKeysContaining(req.queryParams("name").toUpperCase());
                for (Route route : searchResults) {
                    routes.add(route);
                }
            }

            return routes;
        }

        return null;
    }

}
