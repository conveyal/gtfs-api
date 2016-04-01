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
    public static List<Stop> apex(DataFetchingEnvironment env) {
        Map<String, Object> args = env.getArguments();

        Collection<FeedSource> feeds;

        if (args.get("feed_id") != null) {
            List<String> feedId = (List<String>) args.get("feed_id");
            feeds = feedId.stream().map(ApiMain.feedSources::get).collect(Collectors.toList());
        } else {
            feeds = ApiMain.feedSources.values();
        }

        List<Stop> stops = new ArrayList<>();

        for (FeedSource feed : feeds) {
            if (args.get("stop_id") != null) {
                List<String> stopId = (List<String>) args.get("stop_id");
                stopId.stream()
                        .filter(feed.feed.stops::containsKey)
                        .map(feed.feed.stops::get)
                        .forEach(stops::add);
            }
            else {
                stops.addAll(feed.feed.stops.values());
            }
        }

        return stops;
    }

    public static List<Stop> fromPattern(DataFetchingEnvironment environment) {
        Pattern pattern = (Pattern) environment.getSource();

        if (pattern.associatedTrips.isEmpty()) {
            LOG.warn("Empty pattern!");
            return Collections.emptyList();
        }

        GTFSFeed feed = ApiMain.feedSources.get(pattern.feed_id).feed;

        return feed.getOrderedStopListForTrip(pattern.associatedTrips.get(0))
                .stream()
                .map(feed.stops::get)
                .collect(Collectors.toList());
    }

    public static List<Stop> fromFeed(DataFetchingEnvironment environment) {
        FeedInfo fi = (FeedInfo) environment.getSource();
        GTFSFeed feed = ApiMain.feedSources.get(fi.feed_id).feed;
        return new ArrayList<>(feed.stops.values());
    }
}
