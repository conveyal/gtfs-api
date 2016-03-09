package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Trip;
import graphql.execution.ExecutionContext;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;
import java.util.stream.Collectors;

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
}
