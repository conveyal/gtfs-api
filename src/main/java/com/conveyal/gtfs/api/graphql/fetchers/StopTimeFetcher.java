package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.graphql.WrappedGTFSEntity;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import graphql.schema.DataFetchingEnvironment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
        feeds = ApiMain.getFeedSources(feedId);

        List<WrappedGTFSEntity<StopTime>> stopTimes = new ArrayList<>();

        // TODO: clear up possible scope issues feed and stop IDs
        for (FeedSource feed : feeds) {
            if (env.getArgument("stop_id") != null) {
                List<String> stopId = (List<String>) env.getArgument("stop_id");

                for (String id : stopId) {
                    feed.feed.getStopTimesForStop(id).stream()
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
        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(trip.feedUniqueId);
        if (fs == null) return null;

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
        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(stop.feedUniqueId);
        if (fs == null) return null;

        String d = env.getArgument("date");
        Long from = env.getArgument("from");
        Long to = env.getArgument("to");


        LocalDate date = LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE); // 2011-12-03

        List<WrappedGTFSEntity<StopTime>> stopTimes = fs.feed.getStopTimesForStop(stop.entity.stop_id).stream()
                .filter(st -> {
                    Trip trip = fs.feed.trips.get(st.trip_id);

                    // trip's service calendar is active on date and stopTime departs within time window
                    return fs.feed.services.get(trip.service_id).activeOn(date)
                            && (st.departure_time > from && st.departure_time < to);
                })
                .map(st -> new WrappedGTFSEntity<>(fs.id, st))
                .collect(Collectors.toList());

        return stopTimes;
    }
}
