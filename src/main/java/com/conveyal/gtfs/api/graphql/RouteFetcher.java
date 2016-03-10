package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.FeedInfo;
import com.conveyal.gtfs.model.Route;
import graphql.schema.DataFetchingEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by matthewc on 3/10/16.
 */
public class RouteFetcher {
    public static List<Route> apex (DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();

        Collection<FeedSource> feeds;

        if (args.get("feed_id") != null) {
            List<String> feedId = (List<String>) args.get("feed_id");
            feeds = feedId.stream().map(ApiMain.feedSources::get).collect(Collectors.toList());
        } else {
            feeds = ApiMain.feedSources.values();
        }

        List<Route> routes = new ArrayList<>();

        for (FeedSource feed : feeds) {
            if (args.get("route_id") != null) {
                List<String> routeId = (List<String>) args.get("route_id");
                routeId.stream()
                        .filter(feed.feed.routes::containsKey)
                        .map(feed.feed.routes::get)
                        .forEach(routes::add);
            }
            else {
                routes.addAll(feed.feed.routes.values());
            }
        }

        return routes;
    }

    public static List<Route> forFeed(DataFetchingEnvironment environment) {
        FeedInfo fi = (FeedInfo) environment.getSource();
        return new ArrayList<>(ApiMain.feedSources.get(fi.feed_id).feed.routes.values());
    }
}
