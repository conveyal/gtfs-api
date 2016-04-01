package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Trip;
import graphql.schema.DataFetchingEnvironment;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Created by matthewc on 3/9/16.
 */
public class PatternFetcher {
    public static List<Pattern> fromRoute(DataFetchingEnvironment env) {
        Route route = (Route) env.getSource();
        // TODO hack
        FeedSource feed = ApiMain.feedSources.values().stream()
                .filter(s -> s.feed.routes.get(route.route_id) == route)
                .findFirst().orElse(null);

        Set<Pattern> patternSet = feed.feed.trips.values().stream()
                .filter(t -> t.route == route)
                .map(t -> feed.feed.patterns.get(feed.feed.tripPatternMap.get(t.trip_id)))
                // use set so patterns are unique
                .collect(Collectors.toCollection(HashSet::new));

        // graphql insists on receiving list
        return new ArrayList<>(patternSet);
    }

    public static Pattern fromTrip(DataFetchingEnvironment env) {
        Trip trip = (Trip) env.getSource();
        // TODO hack
        FeedSource feed = ApiMain.feedSources.values().stream()
                .filter(s -> s.feed.trips.get(trip.trip_id) == trip)
                .findFirst().orElse(null);

        return feed.feed.patterns.get(feed.feed.tripPatternMap.get(trip.trip_id));
    }
}
