package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.util.GeomUtil;
import com.conveyal.gtfs.model.*;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import com.conveyal.gtfs.api.ApiMain;

import spark.Request;
import spark.Response;

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


        //TODO: Add feed query param?
        if (req.params("id") != null) {
            Route r = ApiMain.feed.routes.get(req.params("id"));
            if(r != null) // && currentUser(req).hasReadPermission(s.projectId))
                return r;
            else
                halt(404, "Route " + req.params("id") + " not found");
        }

        Map<String, Route> routes = Maps.newHashMap();

//        System.out.println(req.queryParams());


        // If no query params specified, then return all routes from all feeds.
        // TODO: Do we need to detect id collision?
        if (req.queryParams().size() == 0){
            for (GTFSFeed feed : ApiMain.feeds){
                routes.putAll(feed.routes);
//                for (Entry<String, Route> route : feed.routes.entrySet()){
//                    routes.put(route.getKey(), route.getValue());
//                }
            }
        }
        if (req.queryParams("feed") != null){
            routes = ApiMain.feedMap.get(req.queryParams("feed")).routes;
        }
        if (req.queryParams("max_lat") != null && req.queryParams("max_lon") != null && req.queryParams("min_lat") != null && req.queryParams("min_lon") != null){
            Coordinate maxCoordinate = new Coordinate(Double.valueOf(req.queryParams("max_lon")), Double.valueOf(req.queryParams("max_lat")));
            Coordinate minCoordinate = new Coordinate(Double.valueOf(req.queryParams("min_lon")), Double.valueOf(req.queryParams("min_lat")));
            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);

            List<Route> searchResults = ApiMain.routeIndex.query(searchEnvelope);
            System.out.println(searchResults.toString());
            for (Route route : searchResults){
                routes.put(route.route_id, route);
            }
            return routes;
        }
        if (req.queryParams("lat") != null && req.queryParams("lon") != null){
            Coordinate latLon = new Coordinate(Double.valueOf(req.queryParams("lon")), Double.valueOf(req.queryParams("lat")));
            if (req.queryParams("radius") != null){
                StopsController.radius = Double.valueOf(req.queryParams("radius"));
            }
            Envelope searchEnvelope = GeomUtil.getBoundingBox(latLon, radius);

            List<Route> searchResults = ApiMain.routeIndex.query(searchEnvelope);
            System.out.println(searchResults.toString());
            for (Route route : searchResults){
                routes.put(route.route_id, route);
            }
            return routes;
        }

        if (req.queryParams("name") != null){
            System.out.println(req.queryParams("name"));

            // search query must be in upper case to match radix tree keys
            long startTime = System.nanoTime();


            Iterable<Route> searchResults = ApiMain.routeTree.getValuesForClosestKeys(req.queryParams("name").toUpperCase());

            long endTime = System.nanoTime();

            long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.

            System.out.println("query time: " + duration);

            for (Route route: searchResults){
                routes.put(route.route_id, route);
            }
        }

        return routes;
    }

}
