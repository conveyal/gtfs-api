package com.conveyal.gtfs.api;

import com.conveyal.gtfs.BaseGTFSCache;
import com.conveyal.gtfs.GTFSCache;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.api.util.CorsFilter;
import com.conveyal.gtfs.api.util.FeedSourceCache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by landon on 2/3/16.
 */
public class ApiMain {
    public static final Properties config = new Properties();
    private static String feedBucket;
    private static String dataDirectory;
    private static String bucketFolder;
    private static FeedSourceCache cache;

    /** IDs of feed sources this API instance is aware of */
    public static Collection<String> registeredFeedSources;

    public static final Logger LOG = LoggerFactory.getLogger(ApiMain.class);
    public static void main(String[] args) throws Exception {
        FileInputStream in;

        if (args.length == 0)
            in = new FileInputStream(new File("application.conf"));
        else
            in = new FileInputStream(new File(args[0]));

        config.load(in);
        initialize(config.getProperty("s3.feeds-bucket"), config.getProperty("s3.bucket-folder"), config.getProperty("application.data"));

        CorsFilter.apply();
        Routes.routes("api");
    }

    public static void initialize (String feedBucket, String dataDirectory) {
        initialize(feedBucket, null, dataDirectory);
    }

    /** Set up the GTFS API. If bundleBucket is null, S3 will not be used */
    public static void initialize (String feedBucket, String bucketFolder, String dataDirectory) {
        ApiMain.feedBucket = feedBucket;
        ApiMain.dataDirectory = dataDirectory;
        ApiMain.bucketFolder = bucketFolder;
        cache = new FeedSourceCache(feedBucket, bucketFolder, new File(dataDirectory));
    }

    /** Register a new feed source with the API */
    public static FeedSource registerFeedSource (String id, File gtfsFile) throws Exception {
        FeedSource fs = cache.put(id, gtfsFile);
        registeredFeedSources.add(id);
        return fs;
    }

    /** Register a new feed source with generated ID. The idGenerator must return the same value when called on the same GTFS feed. */
    public static FeedSource registerFeedSource (Function<GTFSFeed, String> idGenerator, File gtfsFile) throws Exception {
        FeedSource feed = cache.put(idGenerator, gtfsFile);
        String id = idGenerator.apply(feed.feed);
        registeredFeedSources.add(id);
        return feed;
    }

    /** convenience function to get a feed source without throwing checked exceptions, for example for use in lambdas */
    public static FeedSource getFeedSource (String id) {
        try {
            return cache.get(id);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<FeedSource> getFeedSources (List<String> feedIds) {
        return feedIds.stream()
                .map(ApiMain::getFeedSource)
                .filter(fs -> fs != null)
                .collect(Collectors.toList());
    }
}
