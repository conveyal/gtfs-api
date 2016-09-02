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
import org.mapdb.Fun;

import java.util.List;
import java.util.Map;
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

    public static Long fromRouteCount(DataFetchingEnvironment dataFetchingEnvironment) {
        WrappedGTFSEntity<Route> route = (WrappedGTFSEntity<Route>) dataFetchingEnvironment.getSource();
        FeedSource feed = ApiMain.getFeedSource(route.feedUniqueId);

        return feed.feed.trips.values().stream().count();
    }

    public static List<WrappedGTFSEntity<Trip>> fromPattern (DataFetchingEnvironment env) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) env.getSource();

        FeedSource feed = ApiMain.getFeedSource(pattern.feedUniqueId);
        return pattern.entity.associatedTrips.stream().map(feed.feed.trips::get)
                .map(t -> new WrappedGTFSEntity<>(feed.id, t))
                .collect(Collectors.toList());
    }

    public static Long fromPatternCount (DataFetchingEnvironment env) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) env.getSource();

        FeedSource feed = ApiMain.getFeedSource(pattern.feedUniqueId);
        return pattern.entity.associatedTrips.stream().map(feed.feed.trips::get).count();
    }

    public static Integer getStartTime(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Trip> trip = (WrappedGTFSEntity<Trip>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(trip.feedUniqueId);

        Map.Entry<Fun.Tuple2, StopTime> st = feed.feed.stop_times.ceilingEntry(new Fun.Tuple2(trip.entity.trip_id, null));
        return st != null ? st.getValue().departure_time : null;
    }

    public static Integer getDuration(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Trip> trip = (WrappedGTFSEntity<Trip>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(trip.feedUniqueId);

        Integer startTime = getStartTime(env);
        Map.Entry<Fun.Tuple2, StopTime> endStopTime = feed.feed.stop_times.floorEntry(new Fun.Tuple2(trip.entity.trip_id, Fun.HI));

        if (startTime == null || endStopTime == null || endStopTime.getValue().arrival_time < startTime) return null;
        else return endStopTime.getValue().arrival_time - startTime;
    }
}
