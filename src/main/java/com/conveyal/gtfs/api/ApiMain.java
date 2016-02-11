package com.conveyal.gtfs.api;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.models.FeedSource;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;

/**
 * Created by landon on 2/3/16.
 */
public class ApiMain {
    public static Map<String, FeedSource> feedSources;
    public static final Properties config = new Properties();
    private static String s3credentials;
    private static AmazonS3Client s3;
    private static String feedBucket;
    private static ObjectListing gtfsList;
    private static boolean workOffline;
    public static void main(String[] args) throws Exception {
        FileInputStream in;

        if (args.length == 0)
            in = new FileInputStream(new File("application.conf"));
        else
            in = new FileInputStream(new File(args[0]));

        config.load(in);

        initialize();
        Routes.routes();
    }

    /**
     * initialize the database
     */
    public static void initialize() throws IOException {
        // Connect to s3
        workOffline = Boolean.parseBoolean(
                ApiMain.config.getProperty("s3.work-offline"));
        s3credentials = ApiMain.config.getProperty("s3.aws-credentials");
        feedBucket = ApiMain.config.getProperty("s3.feeds-bucket");

        if(!workOffline) {
            if (s3credentials != null) {
                AWSCredentials creds = new ProfileCredentialsProvider(s3credentials, "default").getCredentials();
                s3 = new AmazonS3Client(creds);
            }
            else {
                // default credentials providers, e.g. IAM role
                s3 = new AmazonS3Client();
            }


            gtfsList = s3.listObjects(feedBucket);
//            s3.getbucket;
        }


        // Load GTFS datasets

        // Use s3
        ApiMain.feedSources = Maps.newHashMap();

        if(!workOffline) {
            int count = 0;
            for (S3ObjectSummary objSummary : gtfsList.getObjectSummaries()){
                String feedId = objSummary.getKey();
                System.out.println("Loading feed: " + feedId);
                InputStream obj = s3.getObject(feedBucket, objSummary.getKey()).getObjectContent();

                // create tempfile so we can pass GTFSFeed.fromFile a string file path
                File tempFile = File.createTempFile("test", ".zip");
                tempFile.getAbsolutePath();
                tempFile.deleteOnExit();
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    IOUtils.copy(obj, out);
                }
                ApiMain.feedSources.put(feedId, new FeedSource(tempFile.getAbsolutePath()));
                count++;
                // break after one feed is loaded from aws.
                if (count > 0){
                    break;
                }
            }
        }

        // Use hard-coded paths
        else{
            String[] listOfFeeds = new String[]{"/Users/landon/Downloads/google_transit.zip", "/Users/landon/Downloads/google_transit_staten_island.zip"};
            int count = 0;
            for (String feedPath : listOfFeeds) {
                String feedId = "feed-" + String.valueOf(count);
                System.out.println("Loading feed: " + feedId);
                System.out.println("Loading feed: " + feedPath);
                ApiMain.feedSources.put(feedId, new FeedSource(feedPath));
                count++;
            }
        }

    }

    public GTFSFeed getFeedByAgency(String agencyId) {
        return new GTFSFeed();
    }

    public String[] getAgencyIdsForFeed(GTFSFeed feed) {
//        String[] agencies = new String[]{};

        return feed.agency.keySet().toArray(new String[feed.agency.size()]);
//        for(Agency agency : feed.agency.){
//            if (key == "agency_id"){
//                agencies.add(feed.agency.get("key"));
//            }
//        }
//        return agencies;
    }

}