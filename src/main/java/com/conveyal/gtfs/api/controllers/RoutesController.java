package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.api.util.GeomUtil;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static spark.Spark.halt;

/**
 * Created by landon on 2/4/16.
 */
public class RoutesController {
    private static final Logger LOG = LoggerFactory.getLogger(RoutesController.class);

    public static Double radius = 1.0; // default 1 km search radius
    public static Object getRoutes(Request req, Response res){

        List<Route> routes= new ArrayList<>();
        List<FeedSource> feeds = new ArrayList();

        if (req.queryParams("feed") != null) {

            for (String feedId : req.queryParams("feed").split(",")){
                try {
                    FeedSource feedSource = ApiMain.getFeedSource(feedId);
                    if (feedSource != null) feeds.add(feedSource);
                } catch (Exception e) {
                    halt(404, "Must specify valid feed ids.");
                }
            }

            if (feeds.size() == 0){
                halt(404, "Must specify valid feed id.");
            }
            // If feed is only param.
            else if (req.queryParams().size() == 1 && req.params("id") == null) {
                for (FeedSource feedSource : feeds){
                    routes.addAll(feedSource.feed.routes.values());
                }
                return routes;
            }
        }
        else{
//            res.body("Must specify valid feed id.");
//            return "Must specify valid feed id.";
            halt(404, "Must specify valid feed id.");
        }

        // get specific route
        if (req.params("id") != null) {

            Route r = feeds.get(0).feed.routes.get(req.params("id"));
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
            for (FeedSource feedSource : feeds) {
                // TODO: these are actually patterns being returned, NOT routes
                List<Route> searchResults = feedSource.routeIndex.query(searchEnvelope);
                routes.addAll(searchResults);
            }
            return routes;
        }
        // lat lon + radius
        else if (req.queryParams("lat") != null && req.queryParams("lon") != null){
            Coordinate latLon = new Coordinate(Double.valueOf(req.queryParams("lon")), Double.valueOf(req.queryParams("lat")));
            if (req.queryParams("radius") != null){
                RoutesController.radius = Double.valueOf(req.queryParams("radius"));
            }
            Envelope searchEnvelope = GeomUtil.getBoundingBox(latLon, radius);

            for (FeedSource feedSource : feeds) {
                List<Route> searchResults = feedSource.routeIndex.query(searchEnvelope);
                routes.addAll(searchResults);
            }
            return routes;
        }
        else if (req.queryParams("name") != null){
            LOG.info(req.queryParams("name"));

            for (FeedSource feedSource : feeds) {
                LOG.info("looping feed: " + feedSource.feed.feedId);

                // Check if feed is specified in feed sources requested
                // TODO: Check if user has access to feed source? (Put this in the before call.)
//                if (Arrays.asList(feeds).contains(entry.getKey())) {
                LOG.info("checking feed: " + feedSource.feed.feedId);

                // search query must be in upper case to match radix tree keys
                Iterable<Route> searchResults = feedSource.routeTree.getValuesForKeysContaining(req.queryParams("name").toUpperCase());
                for (Route route : searchResults) {
                    routes.add(route);
                }
            }

            return routes;
        }
        // query for stop_id (i.e., get all routes that operate along patterns for a given stop)
        else if (req.queryParams("stop") != null){
            return getRoutesForStop(req, feeds);
        }

        return null;
    }

    public static Set<Route> getRoutesForStop(Request req, List<FeedSource> feedSources){
        if (req.queryParams("stop") != null){
            String stopId = req.queryParams("stop");
            LOG.info(stopId);
            Set<Route> routes = new HashSet<>();
            // loop through feeds
            for (FeedSource feedSource : feedSources) {
                // loop through patterns, check for route and return pattern stops
                for (Pattern pattern : feedSource.feed.patterns.values()) {
                    if (pattern.orderedStops.contains(stopId)){
                        routes.add(feedSource.feed.routes.get(pattern.route_id));
                    }
                }
            }
            return routes;
        }
        return null;
    }

}
