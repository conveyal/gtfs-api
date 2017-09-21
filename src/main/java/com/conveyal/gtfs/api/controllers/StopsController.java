package com.conveyal.gtfs.api.controllers;


import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.api.util.GeomUtil;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Stop;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static spark.Spark.halt;

/**
 * Created by landon on 2/4/16.
 */
public class StopsController {
    private static final Logger LOG = LoggerFactory.getLogger(StopsController.class);

    private static Double radius = 1.0; // default 1 km search radius
    public static Object getStops(Request req, Response res){

        Set<Stop> stops = new HashSet<>();
        List<FeedSource> feeds = new ArrayList();

        if (req.queryParams("feed") != null) {

            for (String feedId : req.queryParams("feed").split(",")){
                try {
                    FeedSource feedSource = ApiMain.getFeedSource(feedId);
                    if (feedSource != null) {
                        feeds.add(feedSource);
                    }
                } catch (Exception e) {
                    LOG.error("Error retrieving feed", e);
                    halt(404, "Error retrieving feed " + feedId);
                }
            }
            if (feeds.size() == 0){
                halt(404, "Must specify valid feed id.");
            }
            // If feed is only param.
            else if (req.params("id") == null) {
                stops = new HashSet<>();
                for (FeedSource feedSource : feeds){
                    stops.addAll(feedSource.feed.stops.values());
                }
            }
        }
        else {
            halt(404, "Must specify valid feed id.");
        }


        // Continue through params
        if (req.params("id") != null) {
            Stop s = feeds.get(0).feed.stops.get(req.params("id"));
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

            for (FeedSource feedSource : feeds) {
                List<Stop> searchResults = feedSource.stopIndex.query(searchEnvelope);
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
            for (FeedSource feedSource : feeds) {
                List<Stop> searchResults = feedSource.stopIndex.query(searchEnvelope);
                stops.addAll(searchResults);
            }
            return limitStops(req, stops);
        }
        // query name
        else if (req.queryParams("name") != null){
            stops = new HashSet<>();
            LOG.info(req.queryParams("name"));

            for (FeedSource feedSource : feeds) {
                LOG.info("looping feed: " + feedSource.feed.feedId);

                LOG.info("checking feed: " + feedSource.feed.feedId);

                // search query must be in upper case to match radix tree keys
                Iterable<Stop> searchResults = feedSource.stopTree.getValuesForKeysContaining(req.queryParams("name").toUpperCase());

                LOG.info(Integer.toString(Iterables.size(searchResults)));
                for (Stop stop : searchResults) {
                    LOG.info(stop.stop_name);
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
    
    public static Set<Stop> getStopsForRoute(Request req, List<FeedSource> feeds){
        if (req.queryParams("route") != null){
            String routeId = req.queryParams("route");
            LOG.info(routeId);
            Set<Stop> stops = new HashSet<>();
            // loop through feeds
            for (FeedSource feedSource : feeds) {
                Set<String> stopIds = new HashSet<>();

                // loop through patterns, check for route and return pattern stops
                for (Pattern pattern : feedSource.feed.patterns.values()) {
                    if (routeId.equals(pattern.route_id)) {
                        stopIds.addAll(pattern.orderedStops);
                        break;
                    }
                }

                for (String stopId : stopIds) {
                    Stop stop = feedSource.feed.stops.get(stopId);
                    LOG.info(stopId);
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
