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
    public static List<WrappedGTFSEntity<Trip>> fromRoute(DataFetchingEnvironment dataFetchingEnvironment) {
        WrappedGTFSEntity<Route> route = (WrappedGTFSEntity<Route>) dataFetchingEnvironment.getSource();
        FeedSource feed = ApiMain.getFeedSource(route.feedUniqueId);

        return feed.feed.trips.values().stream()
                .filter(t -> t.route_id.equals(route.entity.route_id))
                .map(t -> new WrappedGTFSEntity<>(feed.id, t))
                .collect(Collectors.toList());
    }

    public static List<WrappedGTFSEntity<Trip>> fromPattern (DataFetchingEnvironment env) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) env.getSource();

        FeedSource feed = ApiMain.getFeedSource(pattern.feedUniqueId);
        return pattern.entity.associatedTrips.stream().map(feed.feed.trips::get)
                .map(t -> new WrappedGTFSEntity<>(feed.id, t))
                .collect(Collectors.toList());
    }

    public static Integer getStartTime(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Trip> trip = (WrappedGTFSEntity<Trip>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(trip.feedUniqueId);

        for (StopTime st : feed.feed.getOrderedStopTimesForTrip(trip.entity.trip_id)) {
            return st.arrival_time;
        }

        return null;
    }

    public static Integer getDuration(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Trip> trip = (WrappedGTFSEntity<Trip>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(trip.feedUniqueId);

        int firstArrival = StopTime.INT_MISSING;
        int lastDeparture = StopTime.INT_MISSING;

        for (StopTime st : feed.feed.getOrderedStopTimesForTrip(trip.entity.trip_id)) {
            if (firstArrival == StopTime.INT_MISSING) firstArrival = st.arrival_time;
            lastDeparture = st.departure_time;
        }

        return firstArrival != StopTime.INT_MISSING ? lastDeparture - firstArrival : null;
    }
}
