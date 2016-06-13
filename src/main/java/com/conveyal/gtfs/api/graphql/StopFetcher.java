package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.FeedInfo;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by matthewc on 3/9/16.
 */
public class StopFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(StopFetcher.class);

    /** top level stops query (i.e. not inside a stoptime etc) */
    public static List<WrappedGTFSEntity<Stop>> apex(DataFetchingEnvironment env) {
        Map<String, Object> args = env.getArguments();

        Collection<FeedSource> feeds;

        if (args.get("feed_id") != null) {
            List<String> feedId = (List<String>) args.get("feed_id");
            feeds = feedId.stream().map(ApiMain.feedSources::get).collect(Collectors.toList());
        } else {
            feeds = ApiMain.feedSources.values();
        }

        List<WrappedGTFSEntity<Stop>> stops = new ArrayList<>();

        for (FeedSource feed : feeds) {
            if (args.get("stop_id") != null) {
                List<String> stopId = (List<String>) args.get("stop_id");
                stopId.stream()
                        .filter(feed.feed.stops::containsKey)
                        .map(feed.feed.stops::get)
                        .map(s -> new WrappedGTFSEntity(feed.id, s))
                        .forEach(stops::add);
            }
            else {
                feed.feed.stops.values().stream()
                        .map(s -> new WrappedGTFSEntity(feed.id, s))
                        .forEach(stops::add);
            }
        }

        return stops;
    }

    public static List<WrappedGTFSEntity<Stop>> fromPattern(DataFetchingEnvironment environment) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) environment.getSource();

        if (pattern.entity.associatedTrips.isEmpty()) {
            LOG.warn("Empty pattern!");
            return Collections.emptyList();
        }

        FeedSource source = ApiMain.feedSources.get(pattern.feedUniqueId);

        return source.feed.getOrderedStopListForTrip(pattern.entity.associatedTrips.get(0))
                .stream()
                .map(source.feed.stops::get)
                .map(s -> new WrappedGTFSEntity<>(source.id, s))
                .collect(Collectors.toList());
    }

    public static List<WrappedGTFSEntity<Stop>> fromFeed(DataFetchingEnvironment environment) {
        WrappedGTFSEntity<FeedInfo> fi = (WrappedGTFSEntity<FeedInfo>) environment.getSource();
        FeedSource source = ApiMain.feedSources.get(fi.feedUniqueId);
        return source.feed.stops.values().stream()
                .map(s -> new WrappedGTFSEntity<>(source.id, s))
                .collect(Collectors.toList());
    }
}
