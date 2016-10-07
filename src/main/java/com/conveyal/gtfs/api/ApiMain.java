package com.conveyal.gtfs.api;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.conveyal.gtfs.GTFSCache;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.api.util.CorsFilter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
//import com.google.common.io.Files;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.zip.ZipInputStream;

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
    public static LoadingCache<String, FeedSource> feedSources;
    private static GTFSCache gtfsCache;

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

        gtfsCache = new GTFSCache(feedBucket, bucketFolder, new File(dataDirectory));
        initialize(gtfsCache);
    }

    public static void initialize (GTFSCache gtfsCache) {
        // wrap the GTFS cache in a feed source cache
        feedSources = CacheBuilder.newBuilder()
                .maximumSize(15)
                .build(new CacheLoader<String, FeedSource>() {
                    @Override
                    public FeedSource load(String id) throws Exception {
                        GTFSFeed feed = gtfsCache.get(id);
                        if (feed == null) return null;
                        FeedSource fs = new FeedSource(feed);
                        fs.id = id;
                        return fs;
                    }
                });

        registeredFeedSources = new ArrayList<>();
    }

    /** Register a new feed source with the API */
    public static FeedSource registerFeedSource (String id, File gtfsFile) throws Exception {
        gtfsCache.put(id, gtfsFile);

        // get the feed source through the loading cache so that it is primed and cached for later
        FeedSource fs = feedSources.get(id);
        registeredFeedSources.add(id);
        return fs;
    }

    /** Register a new feed source with generated ID. The idGenerator must return the same value when called on the same GTFS feed. */
    public static FeedSource registerFeedSource (Function<GTFSFeed, String> idGenerator, File gtfsFile) throws Exception {
        GTFSFeed feed = gtfsCache.put(idGenerator, gtfsFile);
        String id = idGenerator.apply(feed);
        registeredFeedSources.add(id);
        return feedSources.get(id);
    }

    /** convenience function to get a feed source without throwing checked exceptions, for example for use in lambdas */
    public static FeedSource getFeedSource (String id) {
        try {
            return feedSources.get(id);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
