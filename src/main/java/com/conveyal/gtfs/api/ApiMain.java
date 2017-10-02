package com.conveyal.gtfs.api;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.api.util.CorsFilter;
import com.conveyal.gtfs.api.util.FeedSourceCache;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by landon on 2/3/16.
 */
public class ApiMain {
    public static final Properties config = new Properties();
    private static FeedSourceCache cache;

    /** IDs of feed sources this API instance is aware of */
    public static ConcurrentHashSet<String> registeredFeedSources = new ConcurrentHashSet<>();

    public static final Logger LOG = LoggerFactory.getLogger(ApiMain.class);

    /**
     * Start a Spark HTTP server providing the GraphQL API.
     */
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

    public static FeedSourceCache initialize (String feedBucket, String dataDirectory) {
        return initialize(feedBucket, null, dataDirectory);
    }

    /** Set up the GTFS API. If bundleBucket is null, S3 will not be used */
    public static FeedSourceCache initialize (String feedBucket, String bucketFolder, String dataDirectory) {
        cache = new FeedSourceCache(feedBucket, bucketFolder, new File(dataDirectory));
        return cache;
    }

    /** Register a new feed source with the API */
    public static FeedSource registerFeedSource (String id, File gtfsFile) throws Exception {
        FeedSource feedSource = cache.put(id, gtfsFile);
        registeredFeedSources.add(id);
        return feedSource;
    }

    /** Register a new feed source with generated ID. The idGenerator must return the same value when called on the same GTFS feed. */
    public static FeedSource registerFeedSource (Function<GTFSFeed, String> idGenerator, File gtfsFile) throws Exception {
        FeedSource feedSource = cache.put(idGenerator, gtfsFile);
        String id = idGenerator.apply(feedSource.feed);
        registeredFeedSources.add(id);
        return feedSource;
    }

    public static FeedSource getFeedSource (String id) throws Exception {
        FeedSource f = cache.get(id);
        // When constructed, the FeedSource object sets its ID to the short FeedId of that feed.
        // We overwrite it here with the longer complete feed unique ID that was used to fetch the feed.
        // This is a complete hack of a "solution" but seems to get the API working.
        // The core problem appears to be an assumption in the GTFS cache classes that the GTFS object
        // alone contains all required information, while in fact we need the longer unique ID.
        f.id = id;
        registeredFeedSources.add(id);
        return f;
    }

    /** Convenience function to get a feed source without throwing checked exceptions, for example for use in lambdas */
    public static FeedSource getFeedSourceWithoutExceptions (String id) {
      try {
        return getFeedSource(id);
      } catch (Exception e) {
        LOG.error("Error retrieving from cache feed " + id, e);
        return null;
      }
    }

    public static List<FeedSource> getFeedSources (List<String> feedIds) {
        return feedIds.stream()
                .map(ApiMain::getFeedSourceWithoutExceptions)
                .filter(fs -> fs != null)
                .collect(Collectors.toList());
    }
}
