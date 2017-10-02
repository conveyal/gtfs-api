package com.conveyal.gtfs.api.util;

import com.conveyal.gtfs.BaseGTFSCache;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.models.FeedSource;

import java.io.File;

/**
 * TODO add description. In what way does this differ from GTFSCache?
 * Why is this the only other subclass of BaseGTFSCache?
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

    @Override
    public GTFSFeed getFeed (String id) {
        return this.get(id).feed;
    }
}
