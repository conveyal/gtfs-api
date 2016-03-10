package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.model.FeedInfo;
import graphql.schema.DataFetchingEnvironment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by matthewc on 3/10/16.
 */
public class FeedFetcher {
    public static List<FeedInfo> apex(DataFetchingEnvironment environment) {
        List<String> feedIds = environment.getArgument("feed_id");
        if (feedIds != null) {
            return feedIds.stream()
                    .map(ApiMain.feedSources::get)
                    .map(fs -> {
                        if (fs.feed.feedInfo.size() > 0) return fs.feed.feedInfo.values().iterator().next();
                        else {
                            FeedInfo ret = new FeedInfo();
                            ret.feed_id = fs.feed.feedId;
                            return ret;
                        }
                    })
                    .collect(Collectors.toList());
        }
        else {
            return ApiMain.feedSources.values().stream()
                    .map(fs -> {
                        if (fs.feed.feedInfo.size() > 0) return fs.feed.feedInfo.values().iterator().next();
                        else {
                            FeedInfo ret = new FeedInfo();
                            ret.feed_id = fs.feed.feedId;
                            return ret;
                        }
                    })
                    .collect(Collectors.toList());
        }
    }
}
