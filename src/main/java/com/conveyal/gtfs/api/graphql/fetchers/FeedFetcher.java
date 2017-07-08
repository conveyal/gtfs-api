package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.GraphQLMain;
import com.conveyal.gtfs.api.graphql.WrappedGTFSEntity;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Entity;
import com.conveyal.gtfs.model.FeedInfo;
import com.vividsolutions.jts.geom.Geometry;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FeedFetcher implements DataFetcher {

    public static final Logger LOG = LoggerFactory.getLogger(DataFetcher.class);

    @Override
    public Map<String, Object> get (DataFetchingEnvironment environment) {
        String namespace = environment.getArgument("namespace"); // This is the unique table prefix.
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append(String.format("select * from feed_info where namespace = '%s'", namespace));
        Connection connection = null;
        try {
            connection = GraphQLMain.dataSource.getConnection();
            Statement statement = connection.createStatement();
            LOG.info("SQL: {}", sqlBuilder.toString());
            if (statement.execute(sqlBuilder.toString())) {
                ResultSet resultSet = statement.getResultSet();
                ResultSetMetaData meta = resultSet.getMetaData();
                int nColumns = meta.getColumnCount();
                // Iterate over result rows
                while (resultSet.next()) {
                    // Create a Map to hold the contents of this row, injecting the feed_id into every map
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("namespace", namespace);
                    for (int i = 1; i < nColumns; i++) {
                        resultMap.put(meta.getColumnName(i), resultSet.getObject(i));
                    }
                    connection.close();
                    return resultMap;
                }
            }
            throw new RuntimeException("No rows found.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    /////////// Legacy code below here

    public static List<WrappedGTFSEntity<FeedInfo>> apex(DataFetchingEnvironment environment) {

        List<String> feedId = environment.getArgument("feed_id");
        return ApiMain.getFeedSources(feedId).stream()
                .map(fs -> getFeedInfo(fs))
                .collect(Collectors.toList());

    }

    private static WrappedGTFSEntity<FeedInfo> getFeedInfo(FeedSource fs) {
        FeedInfo ret;
        if (fs.feed.feedInfo.size() > 0) ret = fs.feed.feedInfo.values().iterator().next();
        else {
            ret = new FeedInfo();
        }

        // NONE is a special value used in GTFS Lib feed info
        if (ret.feed_id == null || "NONE".equals(ret.feed_id)) {
            ret = ret.clone();
            ret.feed_id = fs.feed.feedId;
        }

        return new WrappedGTFSEntity<>(fs.id, ret);
    }

    public static Geometry getMergedBuffer(DataFetchingEnvironment env) {
        WrappedGTFSEntity<FeedInfo> feedInfo = (WrappedGTFSEntity<FeedInfo>) env.getSource();
        FeedSource fs = ApiMain.getFeedSource(feedInfo.feedUniqueId);
        if (fs == null) return null;

        return fs.feed.getMergedBuffers();
    }

    public static WrappedGTFSEntity<FeedInfo> forWrappedGtfsEntity (DataFetchingEnvironment env) {
        WrappedGTFSEntity<FeedInfo> feedInfo = (WrappedGTFSEntity<FeedInfo>) env.getSource();
        FeedSource fs = ApiMain.getFeedSource(feedInfo.feedUniqueId);
        if (fs == null) return null;

        return getFeedInfo(fs);
    }
}
