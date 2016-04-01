package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import graphql.execution.ExecutionContext;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Fetch trip data given a route.
 */
public class TripDataFetcher {
    public static List<Trip> fromRoute(DataFetchingEnvironment dataFetchingEnvironment) {
        Route route = (Route) dataFetchingEnvironment.getSource();
        // TODO huge hack, figure out how to inject this down
        FeedSource feed = ApiMain.feedSources.values().stream()
                .filter(s -> s.feed.routes.get(route.route_id) == route)
                .findFirst().orElse(null);

        return feed.feed.trips.values().stream()
                .filter(t -> t.route == route)
                .collect(Collectors.toList());
    }

    public static List<Trip> fromPattern (DataFetchingEnvironment env) {
        Pattern pattern = (Pattern) env.getSource();

        // TODO huge hack, figure out how to inject this down
        FeedSource feed = ApiMain.feedSources.values().stream()
                .filter(s -> s.feed.patterns.get(pattern.pattern_id) == pattern)
                .findFirst().orElse(null);

        return pattern.associatedTrips.stream().map(feed.feed.trips::get).collect(Collectors.toList());
    }

    public static Integer getStartTime(DataFetchingEnvironment env) {
        Trip trip = (Trip) env.getSource();
        // TODO hack
        FeedSource feed = ApiMain.feedSources.values().stream()
                .filter(s -> s.feed.trips.get(trip.trip_id) == trip)
                .findFirst().orElse(null);

        for (StopTime st : feed.feed.getOrderedStopTimesForTrip(trip.trip_id)) {
            return st.arrival_time;
        }

        return null;
    }

    public static Integer getDuration(DataFetchingEnvironment env) {
        Trip trip = (Trip) env.getSource();
        // TODO hack
        FeedSource feed = ApiMain.feedSources.values().stream()
                .filter(s -> s.feed.trips.get(trip.trip_id) == trip)
                .findFirst().orElse(null);

        int firstArrival = StopTime.INT_MISSING;
        int lastDeparture = StopTime.INT_MISSING;

        for (StopTime st : feed.feed.getOrderedStopTimesForTrip(trip.trip_id)) {
            if (firstArrival == StopTime.INT_MISSING) firstArrival = st.arrival_time;
            lastDeparture = st.departure_time;
        }

        return firstArrival != StopTime.INT_MISSING ? lastDeparture - firstArrival : null;
    }
}
