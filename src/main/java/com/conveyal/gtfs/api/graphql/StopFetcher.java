package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import graphql.schema.DataFetchingEnvironment;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by matthewc on 3/9/16.
 */
public class StopFetcher {
    /** top level stops query (i.e. not inside a stoptime etc) */
    public static List<Stop> apex(DataFetchingEnvironment env) {
        Map<String, Object> args = env.getArguments();

        Collection<FeedSource> feeds;

        if (args.get("feed_id") != null) {
            String feedId = (String) args.get("feed_id");
            feeds = Arrays.asList(ApiMain.feedSources.get(feedId));
        } else {
            feeds = ApiMain.feedSources.values();
        }

        List<Stop> stops = new ArrayList<>();

        for (FeedSource feed : feeds) {
            if (args.get("stop_id") != null) {
                String stopId = (String) args.get("stop_id");
                if (feed.feed.stops.containsKey(stopId)) stops.add(feed.feed.stops.get(stopId));
            }
            else {
                stops.addAll(feed.feed.stops.values());
            }
        }

        return stops;
    }
}
