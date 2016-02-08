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

import static spark.Spark.*;

/**
 * Created by landon on 2/4/16.
 */
public class StopsController {
    public static Double radius = 1.0; // default 1 km search radius
    public static Object getStops(Request req, Response res){

//        System.out.println(req.queryParams().toString());

        //TODO: Add feed query param?
        if (req.params("id") != null) {
            Stop s = ApiMain.feed.stops.get(req.params("id"));
            if(s != null) // && currentUser(req).hasReadPermission(s.projectId))
                return s;
            else
                halt(404, "Stop " + req.params("id") + " not found");
        }

        Map<String, Stop> stops = Maps.newHashMap();

        if (req.queryParams().size() == 0){
            for (GTFSFeed feed : ApiMain.feeds){
                stops.putAll(feed.stops);
            }

        }
        if (req.queryParams("feed") != null){
            stops = ApiMain.feedMap.get(req.queryParams("feed")).stops;
        }

        // TODO: chain if statements together to filter out stops by feed?

        if (req.queryParams("max_lat") != null && req.queryParams("max_lon") != null && req.queryParams("min_lat") != null && req.queryParams("min_lon") != null){
            Coordinate maxCoordinate = new Coordinate(Double.valueOf(req.queryParams("max_lon")), Double.valueOf(req.queryParams("max_lat")));
            Coordinate minCoordinate = new Coordinate(Double.valueOf(req.queryParams("min_lon")), Double.valueOf(req.queryParams("min_lat")));
            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);

            List<Stop> searchResults = ApiMain.stopIndex.query(searchEnvelope);
            System.out.println(searchResults.toString());
            for (Stop stop : searchResults){
                stops.put(stop.stop_id, stop);
            }
            return stops;
        }
        if (req.queryParams("lat") != null && req.queryParams("lon") != null){
            Coordinate latLon = new Coordinate(Double.valueOf(req.queryParams("lon")), Double.valueOf(req.queryParams("lat")));
            if (req.queryParams("radius") != null){
                StopsController.radius = Double.valueOf(req.queryParams("radius"));
            }
            Envelope searchEnvelope = GeomUtil.getBoundingBox(latLon, radius);

            List<Stop> searchResults = ApiMain.stopIndex.query(searchEnvelope);
            System.out.println(searchResults.toString());
            for (Stop stop : searchResults){
                stops.put(stop.stop_id, stop);
            }
            return stops;
        }
        if (req.queryParams("name") != null){
            System.out.println(req.queryParams("name"));
            // TODO: get stops by name with Radix tree

            // search query must be in upper case to match radix tree keys
            Iterable<Stop> searchResults = ApiMain.stopTree.getValuesForClosestKeys(req.queryParams("name").toUpperCase());
//            Iterable<Stop> searchResults = ApiMain.stopTree.getValuesForKeysStartingWith(req.queryParams("name").toUpperCase());
            for (Stop stop: searchResults){
                stops.put(stop.stop_id, stop);
            }
        }

        return stops;
    }

}
