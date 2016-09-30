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
    public static List<WrappedGTFSEntity<Pattern>> fromRoute(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Route> route = (WrappedGTFSEntity<Route>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(route.feedUniqueId);
        List<String> stopIds = env.getArgument("stop_id");

        List<WrappedGTFSEntity<Pattern>> patterns = feed.feed.patterns.values().stream()
                .filter(p -> p.route_id.equals(route.entity.route_id))
                .map(p -> new WrappedGTFSEntity<>(feed.id, p))
                .collect(Collectors.toList());
        if (stopIds != null) {
            return patterns.stream()
                    .filter(p -> p.entity.orderedStops.containsAll(stopIds))
                    .collect(Collectors.toList());
        }
        else {
            return patterns;
        }
    }

    public static long fromRouteCount(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Route> route = (WrappedGTFSEntity<Route>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(route.feedUniqueId);

        return feed.feed.patterns.values().stream()
                .filter(p -> p.route_id.equals(route.entity.route_id))
                .count();
    }

    public static WrappedGTFSEntity<Pattern> fromTrip(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Trip> trip = (WrappedGTFSEntity<Trip>) env.getSource();
        FeedSource feed = ApiMain.getFeedSource(trip.feedUniqueId);
        Pattern patt = feed.feed.patterns.get(feed.feed.tripPatternMap.get(trip.entity.trip_id));
        return new WrappedGTFSEntity<>(feed.id, patt);
    }
}
