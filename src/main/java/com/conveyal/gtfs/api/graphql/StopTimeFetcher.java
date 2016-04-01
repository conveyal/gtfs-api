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
    public static List<StopTime> fromTrip(DataFetchingEnvironment env) {
        Trip trip = (Trip) env.getSource();
        // TODO hack
        FeedSource feed = ApiMain.feedSources.values().stream()
                .filter(s -> s.feed.trips.get(trip.trip_id) == trip)
                .findFirst().orElse(null);

        // TODO stoptimes stay in correct order, right?
        return StreamSupport.stream(feed.feed.getOrderedStopTimesForTrip(trip.trip_id).spliterator(), false)
                .collect(Collectors.toList());
    }
}
