package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import graphql.schema.DataFetchingEnvironment;
import org.mapdb.Fun;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by matthewc on 3/9/16.
 */
public class StopTimeFetcher {
    public static List<WrappedGTFSEntity<StopTime>> apex(DataFetchingEnvironment env) {
        Collection<FeedSource> feeds;

        List<String> feedId = (List<String>) env.getArgument("feed_id");
        feeds = feedId.stream().map(ApiMain::getFeedSource).collect(Collectors.toList());

        List<WrappedGTFSEntity<StopTime>> stopTimes = new ArrayList<>();

        for (FeedSource feed : feeds) {
            if (env.getArgument("stop_id") != null) {
                List<String> stopId = (List<String>) env.getArgument("stop_id");

                for (String id : stopId) {
                    feed.feed.getStopTimesForStop(id).stream()
                            .map(t -> feed.feed.stop_times.get(t.b))
                            .map(st -> new WrappedGTFSEntity(feed.id, st))
                            .forEach(stopTimes::add);
                }
            }
            else if (env.getArgument("trip_id") != null) {
                List<String> tripId = (List<String>) env.getArgument("trip_id");
                tripId.stream()
                        .map(id -> feed.feed.getOrderedStopTimesForTrip(id))
                        .map(st -> new WrappedGTFSEntity(feed.id, st))
                        .forEach(stopTimes::add);
            }
        }

        return stopTimes;
    }
    public static List<WrappedGTFSEntity<StopTime>> fromTrip(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Trip> trip = (WrappedGTFSEntity<Trip>) env.getSource();
        FeedSource fs = ApiMain.getFeedSource(trip.feedUniqueId);
        List<String> stopIds = env.getArgument("stop_id");

        // get stop_times in order
        Stream<StopTime> stopTimes = StreamSupport.stream(fs.feed.getOrderedStopTimesForTrip(trip.entity.trip_id).spliterator(), false);
        if (stopIds != null) {
            return stopTimes
                    .filter(stopTime -> stopIds.contains(stopTime.stop_id))
                    .map(st -> new WrappedGTFSEntity<>(fs.id, st))
                    .collect(Collectors.toList());
        }
        else {
            return stopTimes
                    .map(st -> new WrappedGTFSEntity<>(fs.id, st))
                    .collect(Collectors.toList());
        }
    }

    public static List<WrappedGTFSEntity<StopTime>> fromStop (DataFetchingEnvironment env) {
        WrappedGTFSEntity<Stop> stop = (WrappedGTFSEntity<Stop>) env.getSource();
        FeedSource fs = ApiMain.getFeedSource(stop.feedUniqueId);

        Long beginTime = env.getArgument("begin_time");
        Long endTime = env.getArgument("end_time");

        ZoneId zone = fs.feed.getTimeZoneForStop(stop.entity.stop_id);

        ZoneOffset offset = zone.getRules().getOffset(Instant.now()); // TODO: is this the best way to get the offset?
        LocalDateTime beginDateTime = LocalDateTime.ofEpochSecond(beginTime, 0, offset);
        int beginSeconds = beginDateTime.getHour() * 3600 + beginDateTime.getMinute() * 60 + beginDateTime.getSecond();
        LocalDateTime endDateTime = LocalDateTime.ofEpochSecond(endTime, 0, offset);
        int endSeconds = endDateTime.getHour() * 3600 + endDateTime.getMinute() * 60 + endDateTime.getSecond();

        long days = ChronoUnit.DAYS.between(beginDateTime, endDateTime); // get days active
        Set<String> services = fs.feed.services.values().stream()
                .filter(s -> {
                    for (int i = 0; i <= days; i++) {
                        LocalDate date = beginDateTime.toLocalDate().plusDays(i);
                        if (s.activeOn(date)) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(s -> s.service_id)
                .collect(Collectors.toSet());

        SortedSet<Fun.Tuple2<String, Fun.Tuple2>> index = fs.feed.stopStopTimeSet
                .subSet(new Fun.Tuple2<>(stop.entity.stop_id, null), new Fun.Tuple2(stop.entity.stop_id, Fun.HI));

        List<WrappedGTFSEntity<StopTime>> stopTimes = index.stream()
                .map(t -> fs.feed.stop_times.get(t.b))
                .filter(st -> {
                    Trip trip = fs.feed.trips.get(st.trip_id);

                    return services.contains(trip.service_id) // trip's service calendar is active on one of the days included in datetime range
                            && (st.departure_time > beginSeconds && st.departure_time < endSeconds);
                })
                .map(st -> new WrappedGTFSEntity<>(fs.id, st))
                .collect(Collectors.toList());

        return stopTimes;
    }
}
