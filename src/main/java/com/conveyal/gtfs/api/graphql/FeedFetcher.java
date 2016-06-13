package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.model.FeedInfo;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by matthewc on 3/10/16.
 */
public class FeedFetcher {
    public static List<WrappedGTFSEntity<FeedInfo>> apex(DataFetchingEnvironment environment) {
        List<String> feedIds = environment.getArgument("feed_id");
        if (feedIds != null) {
            return feedIds.stream()
                    .map(ApiMain.feedSources::get)
                    .map(fs -> {
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
                    })
                    .collect(Collectors.toList());
        }
        else {
            return ApiMain.feedSources.values().stream()
                    .map(fs -> {
                        FeedInfo ret;
                        if (fs.feed.feedInfo.size() > 0) ret = fs.feed.feedInfo.values().iterator().next();
                        else {
                            ret = new FeedInfo();
                        }

                        if (ret.feed_id == null || "NONE".equals(ret.feed_id)) {
                            ret = ret.clone();
                            ret.feed_id = fs.feed.feedId;
                        }

                        return new WrappedGTFSEntity<>(fs.id, ret);
                    })
                    .collect(Collectors.toList());
        }
    }
}
