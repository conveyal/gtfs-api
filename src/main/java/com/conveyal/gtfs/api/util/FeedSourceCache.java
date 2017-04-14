package com.conveyal.gtfs.api.util;

import com.conveyal.gtfs.BaseGTFSCache;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.models.FeedSource;

import java.io.File;

/**
 * Created by matthewc on 4/14/17.
 */
public class FeedSourceCache extends BaseGTFSCache<FeedSource> {
    public FeedSourceCache(String bucket, File cacheDir) {
        super(bucket, cacheDir);
    }

    public FeedSourceCache(String bucket, String bucketFolder, File cacheDir) {
        super(bucket, bucketFolder, cacheDir);
    }

    @Override
    protected FeedSource processFeed(GTFSFeed feed) {
        return new FeedSource(feed);
    }
}
