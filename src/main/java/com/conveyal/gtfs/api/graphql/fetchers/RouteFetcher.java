package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.GraphQLMain;
import com.conveyal.gtfs.api.graphql.WrappedGTFSEntity;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.loader.JDBCTableReader;
import com.conveyal.gtfs.model.FeedInfo;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import graphql.schema.DataFetchingEnvironment;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.dbutils.DbUtils;

/**
 * Created by matthewc on 3/10/16.
 */
public class RouteFetcher {
    public static List<WrappedGTFSEntity<Route>> apex (DataFetchingEnvironment environment) {
        Map<String, Object> args = environment.getArguments();

        Collection<FeedSource> feeds;

        List<String> feedId = (List<String>) args.get("feed_id");
        feeds = ApiMain.getFeedSources(feedId);

        List<WrappedGTFSEntity<Route>> routes = new ArrayList<>();

        // TODO: clear up possible scope issues feed and route IDs
        for (FeedSource feed : feeds) {
            if (args.get("route_id") != null) {
                List<String> routeId = (List<String>) args.get("route_id");
                routeId.stream()
                        .filter(feed.feed.routes::containsKey)
                        .map(feed.feed.routes::get)
                        .map(r -> new WrappedGTFSEntity(feed.id, r))
                        .forEach(routes::add);
            }
            else {
                feed.feed.routes.values().stream().map(r -> new WrappedGTFSEntity<>(feed.id, r)).forEach(routes::add);
            }
        }

        return routes;
    }

    public static Integer fromStopCount(DataFetchingEnvironment environment) {
        Map<String, Object> parentFeedMap = environment.getSource();
        String namespace = (String) parentFeedMap.get("namespace");
        String stop_id = (String) parentFeedMap.get("stop_id");
        Connection connection = null;
        Integer count = null;
        try {
            connection = GraphQLMain.dataSource.getConnection();
            connection.setSchema(namespace);
            Statement statement = connection.createStatement();
            String sql = String.format("select count(distinct r.route_id) as num " +
            		"from pattern_stops ps " +
            		"join trips t on ps.pattern_id = t.pattern_id " +
            		"join routes r on t.route_id= r.route_id " +
            		"where ps.stop_id = '%s'", stop_id);
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

    public static List<Map<String, Object>> fromStop(DataFetchingEnvironment environment) {
        // This will contain one Map<String, Object> for each row fetched from the sql resulset.
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> parentFeedMap = environment.getSource();
        String namespace = (String) parentFeedMap.get("namespace");
        String stop_id = (String) parentFeedMap.get("stop_id");
        Connection connection = null;
        try {
            connection = GraphQLMain.dataSource.getConnection();
            connection.setSchema(namespace);
            Statement statement = connection.createStatement();
            String sql = String.format(
              "select distinct r.* " +
              "from routes r " +
              "join patterns p on r.route_id = p.route_id " +
              "join pattern_stops ps on p.pattern_id = ps.pattern_id " +
              "where ps.stop_id = '%s'", stop_id);
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
 
    @Deprecated
    public static List<WrappedGTFSEntity<Route>> fromStopOld(DataFetchingEnvironment environment) {
        WrappedGTFSEntity<Stop> stop = (WrappedGTFSEntity<Stop>) environment.getSource();
        List<String> routeIds = environment.getArgument("route_id");

        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(stop.feedUniqueId);
        if (fs == null) return null;

        List<WrappedGTFSEntity<Route>> routes = fs.feed.patterns.values().stream()
                .filter(p -> p.orderedStops.contains(stop.entity.stop_id))
                .map(p -> fs.feed.routes.get(p.route_id))
                .distinct()
                .map(r -> new WrappedGTFSEntity<>(fs.id, r))
                .collect(Collectors.toList());

        if (routeIds != null) {
            return routes.stream()
                    .filter(r -> routeIds.contains(r.entity.route_id))
                    .collect(Collectors.toList());
        }
        else {
            return routes;
        }
    }

    public static WrappedGTFSEntity<Route> fromPattern(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Pattern> pattern = (WrappedGTFSEntity<Pattern>) env.getSource();
        List<String> routeIds = env.getArgument("route_id");

        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(pattern.feedUniqueId);
        if (fs == null) return null;

        return new WrappedGTFSEntity<>(fs.id, fs.feed.routes.get(pattern.entity.route_id));
    }

    public static List<WrappedGTFSEntity<Route>> fromFeed(DataFetchingEnvironment environment) {
        WrappedGTFSEntity<FeedInfo> fi = (WrappedGTFSEntity<FeedInfo>) environment.getSource();
        List<String> routeIds = environment.getArgument("route_id");

        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(fi.feedUniqueId);
        if (fs == null) return null;

        if (routeIds != null) {
            return routeIds.stream()
                    .filter(id -> id != null && fs.feed.routes.containsKey(id))
                    .map(fs.feed.routes::get)
                    .map(r -> new WrappedGTFSEntity<>(fs.id, r))
                    .collect(Collectors.toList());
        }
        else {
            return fs.feed.routes.values().stream()
                    .map(r -> new WrappedGTFSEntity<>(fs.id, r))
                    .collect(Collectors.toList());
        }
    }

    public static Long fromFeedCount(DataFetchingEnvironment environment) {
        WrappedGTFSEntity<FeedInfo> fi = (WrappedGTFSEntity<FeedInfo>) environment.getSource();

        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(fi.feedUniqueId);
        if (fs == null) return null;

        return fs.feed.routes.values().stream().count();
    }
}
