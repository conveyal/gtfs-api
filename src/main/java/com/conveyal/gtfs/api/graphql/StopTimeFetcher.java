package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import graphql.schema.DataFetchingEnvironment;
import org.mapdb.Fun;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by matthewc on 3/9/16.
 */
public class StopTimeFetcher {
    public static List<WrappedGTFSEntity<StopTime>> fromTrip(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Trip> trip = (WrappedGTFSEntity<Trip>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(trip.feedUniqueId);
        List<String> stopIds = env.getArgument("stop_id");

        // get stop_times in order
        Stream<StopTime> stopTimes = StreamSupport.stream(feed.feed.getOrderedStopTimesForTrip(trip.entity.trip_id).spliterator(), false);
        if (stopIds != null) {
            return stopTimes
                    .filter(stopTime -> stopIds.contains(stopTime.stop_id))
                    .map(st -> new WrappedGTFSEntity<>(feed.id, st))
                    .collect(Collectors.toList());
        }
        else {
            return stopTimes
                    .map(st -> new WrappedGTFSEntity<>(feed.id, st))
                    .collect(Collectors.toList());
        }
    }

    public static List<WrappedGTFSEntity<StopTime>> fromStop (DataFetchingEnvironment env) {
        WrappedGTFSEntity<Stop> stop = (WrappedGTFSEntity<Stop>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(stop.feedUniqueId);

        Long beginTime = env.getArgument("begin_time");
        Long endTime = env.getArgument("end_time");

        LocalDateTime beginDateTime = LocalDateTime.ofEpochSecond(beginTime, 0, ZoneOffset.UTC);
        int beginSeconds = beginDateTime.getHour() * 3600 + beginDateTime.getMinute() * 60 + beginDateTime.getSecond();
        LocalDateTime endDateTime = LocalDateTime.ofEpochSecond(endTime, 0, ZoneOffset.UTC);
        int endSeconds = endDateTime.getHour() * 3600 + endDateTime.getMinute() * 60 + endDateTime.getSecond();

        SortedSet<Fun.Tuple2<String, Fun.Tuple2>> index = feed.feed.stopStopTimeSet
                .subSet(new Fun.Tuple2<>(stop.entity.stop_id, null), new Fun.Tuple2(stop.entity.stop_id, Fun.HI));

        List<WrappedGTFSEntity<StopTime>> stopTimes = index.stream()
                .map(t -> feed.feed.stop_times.get(t.b))
                .filter(st -> st.departure_time > beginSeconds && st.departure_time < endSeconds)
                .map(st -> new WrappedGTFSEntity<>(feed.id, st))
                .collect(Collectors.toList());

        return stopTimes;
    }
}
