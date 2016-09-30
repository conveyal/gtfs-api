package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.StopTime;
import com.conveyal.gtfs.model.Trip;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;
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

        Stream<StopTime> stopTimes = StreamSupport.stream(feed.feed.getOrderedStopTimesForTrip(trip.entity.trip_id).spliterator(), false);
        if (stopIds != null) {
            return stopTimes
                    .filter(stopTime -> stopIds.contains(stopTime.stop_id))
                    .map(st -> new WrappedGTFSEntity<>(feed.id, st))
                    .collect(Collectors.toList());
        }
        else {
            // TODO stoptimes stay in correct order, right?
            return stopTimes
                    .map(st -> new WrappedGTFSEntity<>(feed.id, st))
                    .collect(Collectors.toList());
        }
    }
}
