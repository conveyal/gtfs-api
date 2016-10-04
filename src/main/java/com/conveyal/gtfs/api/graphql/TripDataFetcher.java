package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Agency;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Service;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import graphql.execution.ExecutionContext;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import org.mapdb.Fun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.time.temporal.ChronoUnit.*;

import static spark.Spark.halt;

/**
 * Fetch trip data given a route.
 */
public class TripDataFetcher {
    public static List<WrappedGTFSEntity<Trip>> apex(DataFetchingEnvironment env) {
        Collection<FeedSource> feeds;

        List<String> feedId = (List<String>) env.getArgument("feed_id");
        feeds = feedId.stream().map(ApiMain::getFeedSource).collect(Collectors.toList());

        List<WrappedGTFSEntity<Trip>> trips = new ArrayList<>();

        for (FeedSource feed : feeds) {
            if (env.getArgument("trip_id") != null) {
                List<String> tripId = (List<String>) env.getArgument("trip_id");
                tripId.stream()
                        .filter(feed.feed.trips::containsKey)
                        .map(feed.feed.trips::get)
                        .map(trip -> new WrappedGTFSEntity(feed.id, trip))
                        .forEach(trips::add);
            }
            else if (env.getArgument("route_id") != null) {
                List<String> routeId = (List<String>) env.getArgument("route_id");
                feed.feed.trips.values().stream()
                        .filter(t -> routeId.contains(t.route_id))
                        .map(trip -> new WrappedGTFSEntity(feed.id, trip))
                        .forEach(trips::add);
            }
        }

        return trips;
    }
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

        return feed.feed.trips.values().stream()
                .filter(t -> t.route_id.equals(route.entity.route_id))
                .count();
    }

    public static WrappedGTFSEntity<Trip> fromStopTime (DataFetchingEnvironment env) {
        WrappedGTFSEntity<StopTime> stopTime = (WrappedGTFSEntity<StopTime>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(stopTime.feedUniqueId);
        Trip trip = feed.feed.trips.get(stopTime.entity.trip_id);

        return new WrappedGTFSEntity<>(stopTime.feedUniqueId, trip);
    }

    public static List<WrappedGTFSEntity<Trip>> fromPattern (DataFetchingEnvironment env) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(pattern.feedUniqueId);

        Long beginTime = env.getArgument("begin_time");
        Long endTime = env.getArgument("end_time");

        if (beginTime != null && endTime != null) {
            String agencyId = feed.feed.routes.get(pattern.entity.route_id).agency_id;
            Agency agency = agencyId != null ? feed.feed.agency.get(agencyId) : null;
            if (beginTime >= endTime) {
                halt(404, "end_time must be greater than begin_time.");
            }
            LocalDateTime beginDateTime = LocalDateTime.ofEpochSecond(beginTime, 0, ZoneOffset.UTC);
            int beginSeconds = beginDateTime.getSecond();
            LocalDateTime endDateTime = LocalDateTime.ofEpochSecond(endTime, 0, ZoneOffset.UTC);
            int endSeconds = endDateTime.getSecond();
            long days = ChronoUnit.DAYS.between(beginDateTime, endDateTime);
            ZoneId zone =  agency != null ? ZoneId.of(agency.agency_timezone) : ZoneId.systemDefault();
            Set<String> services = feed.feed.services.values().stream()
                    .filter(s -> {
                        for (int i = 0; i < days; i++) {
                            LocalDate date = beginDateTime.toLocalDate().plusDays(i);
                            if (s.activeOn(date)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .map(s -> s.service_id)
                    .collect(Collectors.toSet());
            return pattern.entity.associatedTrips.stream().map(feed.feed.trips::get)
                    .filter(t -> services.contains(t.service_id))
                    .map(t -> new WrappedGTFSEntity<>(feed.id, t))
                    .collect(Collectors.toList());
        }
        else {
            return pattern.entity.associatedTrips.stream().map(feed.feed.trips::get)
                    .map(t -> new WrappedGTFSEntity<>(feed.id, t))
                    .collect(Collectors.toList());
        }
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
