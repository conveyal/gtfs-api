package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.FeedInfo;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
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

        List<String> feedId = (List<String>) args.get("feed_id");
        feeds = feedId.stream().map(ApiMain::getFeedSource).collect(Collectors.toList());

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

        FeedSource source = ApiMain.getFeedSource(pattern.feedUniqueId);

        return source.feed.getOrderedStopListForTrip(pattern.entity.associatedTrips.get(0))
                .stream()
                .map(source.feed.stops::get)
                .map(s -> new WrappedGTFSEntity<>(source.id, s))
                .collect(Collectors.toList());
    }

    public static Long fromPatternCount(DataFetchingEnvironment environment) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) environment.getSource();

        if (pattern.entity.associatedTrips.isEmpty()) {
            LOG.warn("Empty pattern!");
            return 0L;
        }

        FeedSource source = ApiMain.getFeedSource(pattern.feedUniqueId);

        return source.feed.getOrderedStopListForTrip(pattern.entity.associatedTrips.get(0))
                .stream().count();
    }

    public static List<WrappedGTFSEntity<Stop>> fromFeed(DataFetchingEnvironment env) {
        WrappedGTFSEntity<FeedInfo> fi = (WrappedGTFSEntity<FeedInfo>) env.getSource();
        FeedSource source = ApiMain.getFeedSource(fi.feedUniqueId);

        Collection<Stop> stops = source.feed.stops.values();

        // check for bbox query
        if(argumentDefined(env, "min_lat") && argumentDefined(env, "max_lat") &&
                argumentDefined(env, "min_lon") && argumentDefined(env, "max_lon")) {
            System.out.println("min_lat, etc. present " + env.getArgument("min_lat"));
            Coordinate maxCoordinate = new Coordinate(env.getArgument("max_lon"), env.getArgument("max_lat"));
            Coordinate minCoordinate = new Coordinate(env.getArgument("min_lon"), env.getArgument("min_lat"));
            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);
            stops = source.stopIndex.query(searchEnvelope);
        }

        return stops.stream()
                .map(s -> new WrappedGTFSEntity<>(source.id, s))
                .collect(Collectors.toList());
    }

    public static Long fromFeedCount(DataFetchingEnvironment env) {
        WrappedGTFSEntity<FeedInfo> fi = (WrappedGTFSEntity<FeedInfo>) env.getSource();
        FeedSource source = ApiMain.getFeedSource(fi.feedUniqueId);

        Collection<Stop> stops = source.feed.stops.values();

        // check for bbox query
        if(argumentDefined(env, "min_lat") && argumentDefined(env, "max_lat") &&
                argumentDefined(env, "min_lon") && argumentDefined(env, "max_lon")) {
            System.out.println("min_lat, etc. present " + env.getArgument("min_lat"));
            Coordinate maxCoordinate = new Coordinate(env.getArgument("max_lon"), env.getArgument("max_lat"));
            Coordinate minCoordinate = new Coordinate(env.getArgument("min_lon"), env.getArgument("min_lat"));
            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);
            stops = source.stopIndex.query(searchEnvelope);
        }

        return stops.stream().count();
    }

    public static boolean argumentDefined(DataFetchingEnvironment env, String name) {
        return (env.containsArgument(name) && env.getArgument(name) != null);
    }
}
