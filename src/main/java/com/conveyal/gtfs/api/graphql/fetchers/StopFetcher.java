package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.GraphQLMain;
import com.conveyal.gtfs.api.graphql.WrappedGTFSEntity;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.api.util.GeomUtil;
import com.conveyal.gtfs.model.FeedInfo;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Stop;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import graphql.schema.DataFetchingEnvironment;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.conveyal.gtfs.api.util.GraphQLUtil.argumentDefined;

/**
 * Created by matthewc on 3/9/16.
 */
public class StopFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(StopFetcher.class);
    private static final Double DEFAULT_RADIUS = 1.0; // default 1 km search radius

    /** top level stops query (i.e. not inside a stoptime etc) */
    public static List<WrappedGTFSEntity<Stop>> apex(DataFetchingEnvironment env) {
        Map<String, Object> args = env.getArguments();

        Collection<FeedSource> feeds;

        List<String> feedId = (List<String>) args.get("feed_id");
        feeds = ApiMain.getFeedSources(feedId);

        List<WrappedGTFSEntity<Stop>> stops = new ArrayList<>();

        // TODO: clear up possible scope issues feed and stop IDs
        for (FeedSource fs : feeds) {
            if (args.get("stop_id") != null) {
                List<String> stopId = (List<String>) args.get("stop_id");
                stopId.stream()
                        .filter(id -> id != null && fs.feed.stops.containsKey(id))
                        .map(fs.feed.stops::get)
                        .map(s -> new WrappedGTFSEntity(fs.id, s))
                        .forEach(stops::add);
            }
            // TODO: should pattern pre-empt route or should they operate together?
            else if (args.get("pattern_id") != null) {
                List<String> patternId = (List<String>) args.get("pattern_id");

                fs.feed.patterns.values().stream()
                        .filter(p -> patternId.contains(p.pattern_id))
                        .map(p -> fs.feed.getOrderedStopListForTrip(p.associatedTrips.get(0)))
                        .flatMap(List::stream)
                        .map(fs.feed.stops::get)
                        .distinct()
                        .map(stop -> new WrappedGTFSEntity(fs.id, stop))
                        .forEach(stops::add);
            }
            else if (args.get("route_id") != null) {
                List<String> routeId = (List<String>) args.get("route_id");

                fs.feed.patterns.values().stream()
                        .filter(p -> routeId.contains(p.route_id))
                        .map(p -> fs.feed.getOrderedStopListForTrip(p.associatedTrips.get(0)))
                        .flatMap(List::stream)
                        .map(fs.feed.stops::get)
                        .distinct()
                        .map(stop -> new WrappedGTFSEntity(fs.id, stop))
                        .forEach(stops::add);
            }
            else {
                // get stops by lat/lon/radius
                if (argumentDefined(env, "lat") && argumentDefined(env, "lon")) {
                    Double lat = (Double) args.get("lat");
                    Double lon = (Double) args.get("lon");
                    Double radius = args.get("radius") == null ? DEFAULT_RADIUS : (Double) args.get("radius");
                    Coordinate latLng = new Coordinate(lon, lat);
                    Envelope searchEnvelope = GeomUtil.getBoundingBox(latLng, radius);

                    List<Stop> results = fs.stopIndex.query(searchEnvelope);
                    results.stream()
                            .map(s -> new WrappedGTFSEntity(fs.id, s))
                            .forEach(stops::add);
                }
                // get stops by bounding box
                else if (argumentDefined(env, "min_lat") && argumentDefined(env, "max_lat") &&
                        argumentDefined(env, "min_lon") && argumentDefined(env, "max_lon")) {
                    Coordinate maxCoordinate = new Coordinate((Double) args.get("max_lon"), (Double) args.get("max_lat"));
                    Coordinate minCoordinate = new Coordinate((Double) args.get("min_lon"), (Double) args.get("min_lat"));
                    Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);

                    List<Stop> results = fs.stopIndex.query(searchEnvelope);
                    results.stream()
                            .map(s -> new WrappedGTFSEntity(fs.id, s))
                            .forEach(stops::add);
                }
                // get all
                else {
                    fs.feed.stops.values().stream()
                            .map(s -> new WrappedGTFSEntity(fs.id, s))
                            .forEach(stops::add);
                }
            }
        }

        return stops;
    }

    public static Integer fromRouteCount(DataFetchingEnvironment environment) {
        Map<String, Object> parentFeedMap = environment.getSource();
        String namespace = (String) parentFeedMap.get("namespace");
        String route_id = (String) parentFeedMap.get("route_id");
        Connection connection = null;
        Integer count = null;
        try {
            connection = GraphQLMain.dataSource.getConnection();
            connection.setSchema(namespace);
            Statement statement = connection.createStatement();
            String sql = String.format("select count(distinct stop_id) " +
              "from pattern_stops ps " +
              "join patterns p on p.route_id = p.route_id " +
              "where p.route_id = '%s'", route_id);
            if (statement.execute(sql)) {
                ResultSet resultSet = statement.getResultSet();
                resultSet.next();
                count = resultSet.getInt(1);
            }
        } catch (SQLException e) {
	        throw new RuntimeException(e);
	    } finally {
	        DbUtils.closeQuietly(connection);
	    }
        return count;
    }

    public static List<Map<String, Object>> fromRoute(DataFetchingEnvironment environment) {
        // This will contain one Map<String, Object> for each row fetched from the sql resulset.
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> parentFeedMap = environment.getSource();
        String namespace = (String) parentFeedMap.get("namespace");
        String route_id = (String) parentFeedMap.get("route_id");
        Connection connection = null;
        try {
            connection = GraphQLMain.dataSource.getConnection();
            connection.setSchema(namespace);
            Statement statement = connection.createStatement();
            String sql = String.format(
              "select distinct s.* " +
              "from stops s " +
              "join pattern_stops ps on s.stop_id = ps.stop_id " +
              "join patterns p on ps.pattern_id = p.pattern_id " +
              "where p.route_id = '%s'", route_id);
            if (statement.execute(sql)) {
                ResultSet resultSet = statement.getResultSet();
                ResultSetMetaData meta = resultSet.getMetaData();
                int nColumns = meta.getColumnCount();
                // Iterate over result rows
                while (resultSet.next()) {
                    // Create a Map to hold the contents of this row, injecting the stop into every map
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("namespace", namespace);
                    for (int i = 1; i < nColumns; i++) {
                        resultMap.put(meta.getColumnName(i), resultSet.getObject(i));
                    }
                    results.add(resultMap);
                }
            }
        } catch (SQLException e) {
	        throw new RuntimeException(e);
	    } finally {
	        DbUtils.closeQuietly(connection);
	    }
        // Return a List of Maps, one Map for each row in the result.
        return results;
    }

    public static List<WrappedGTFSEntity<Stop>> fromPattern(DataFetchingEnvironment environment) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) environment.getSource();

        if (pattern.entity.associatedTrips.isEmpty()) {
            LOG.warn("Empty pattern!");
            return Collections.emptyList();
        }

        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(pattern.feedUniqueId);
        if (fs == null) return null;

        return fs.feed.getOrderedStopListForTrip(pattern.entity.associatedTrips.get(0))
                .stream()
                .map(fs.feed.stops::get)
                .map(s -> new WrappedGTFSEntity<>(fs.id, s))
                .collect(Collectors.toList());
    }

    /** @return the number of stops in the given pattern */
    public static Long fromPatternCount(DataFetchingEnvironment environment) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) environment.getSource();

        if (pattern.entity.associatedTrips.isEmpty()) {
            LOG.warn("Empty pattern!");
            return 0L;
        }

        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(pattern.feedUniqueId);
        if (fs == null) return null;

        return fs.feed.getOrderedStopListForTrip(pattern.entity.associatedTrips.get(0))
                .stream().count();
    }

    public static List<WrappedGTFSEntity<Stop>> fromFeed(DataFetchingEnvironment env) {
        WrappedGTFSEntity<FeedInfo> fi = (WrappedGTFSEntity<FeedInfo>) env.getSource();
        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(fi.feedUniqueId);
        if (fs == null) return null;

        Collection<Stop> stops = fs.feed.stops.values();
        List<String> stopIds = env.getArgument("stop_id");

        if (stopIds != null) {
            return stopIds.stream()
                    .filter(id -> id != null && fs.feed.stops.containsKey(id))
                    .map(fs.feed.stops::get)
                    .map(s -> new WrappedGTFSEntity<>(fs.id, s))
                    .collect(Collectors.toList());
        }
        // check for bbox query
        if(argumentDefined(env, "min_lat") && argumentDefined(env, "max_lat") &&
                argumentDefined(env, "min_lon") && argumentDefined(env, "max_lon")) {
            System.out.println("min_lat, etc. present " + env.getArgument("min_lat"));
            Coordinate maxCoordinate = new Coordinate(env.getArgument("max_lon"), env.getArgument("max_lat"));
            Coordinate minCoordinate = new Coordinate(env.getArgument("min_lon"), env.getArgument("min_lat"));
            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);
            stops = fs.stopIndex.query(searchEnvelope);
        }

        return stops.stream()
                .map(s -> new WrappedGTFSEntity<>(fs.id, s))
                .collect(Collectors.toList());
    }

    public static Long fromFeedCount(DataFetchingEnvironment env) {
        WrappedGTFSEntity<FeedInfo> fi = (WrappedGTFSEntity<FeedInfo>) env.getSource();
        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(fi.feedUniqueId);
        if (fs == null) return null;

        Collection<Stop> stops = fs.feed.stops.values();

        // check for bbox query
        if(argumentDefined(env, "min_lat") && argumentDefined(env, "max_lat") &&
                argumentDefined(env, "min_lon") && argumentDefined(env, "max_lon")) {
            System.out.println("min_lat, etc. present " + env.getArgument("min_lat"));
            Coordinate maxCoordinate = new Coordinate(env.getArgument("max_lon"), env.getArgument("max_lat"));
            Coordinate minCoordinate = new Coordinate(env.getArgument("min_lon"), env.getArgument("min_lat"));
            Envelope searchEnvelope = new Envelope(maxCoordinate, minCoordinate);
            stops = fs.stopIndex.query(searchEnvelope);
        }

        return stops.stream().count();
    }
}
