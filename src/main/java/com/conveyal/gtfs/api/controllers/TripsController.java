package com.conveyal.gtfs.api.controllers;


import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Frequency;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.halt;

/**
 * Created by landon on 2/4/16.
 */
public class TripsController {
    private static final Logger LOG = LoggerFactory.getLogger(TripsController.class);

    public static Double radius = 1.0; // default 1 km search radius
    public static Object getTrips(Request req, Response res){

        List<TripSummary> trips = new ArrayList<>();
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
                    halt(400, e.getMessage());
                }
            }
            if (feeds.size() == 0){
                halt(404, "Must specify valid feed id.");
            }
            // If feed is only param.
            else if (req.queryParams().size() == 1) {
                feeds.forEach(feedSource -> {
                    feedSource.feed.trips.values().stream().forEach(t -> {
                        trips.add(new TripSummary(t, feedSource.feed));
                    });
                });
            }
        }
        else{
            halt(404, "Must specify valid feed id.");
        }


        // Continue through params
        if (req.params("id") != null) {
            GTFSFeed feed = feeds.get(0).feed;
            Trip t = feed.trips.get(req.params("id"));
            if(t != null) // && currentUser(req).hasReadPermission(s.projectId))
                return new TripSummary(t, feed);
            else
                halt(404, "Trip " + req.params("id") + " not found");
        }
        else if (req.queryParams("route") != null){
            String route_id = req.queryParams("route");
            System.out.println(route_id);
            GTFSFeed feed = feeds.get(0).feed;
            List<Trip> tripsForRoute = feed.trips.values()
                    .stream().filter(t -> t.route_id.equals(route_id)).collect(Collectors.toList());
            System.out.println(tripsForRoute.toString());
            tripsForRoute.stream().map(t -> new TripSummary(t, feed)).forEach(trips::add);
            return trips;
        }

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
