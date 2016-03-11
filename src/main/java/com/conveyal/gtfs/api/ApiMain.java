package com.conveyal.gtfs.api;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
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
    private static String dataDirectory;
    private static ObjectListing gtfsList;
    private static boolean workOffline;
    public static void main(String[] args) throws Exception {
        FileInputStream in;

        if (args.length == 0)
            in = new FileInputStream(new File("application.conf"));
        else
            in = new FileInputStream(new File(args[0]));

        config.load(in);
        workOffline = Boolean.parseBoolean(
                ApiMain.config.getProperty("s3.work-offline"));
        s3credentials = ApiMain.config.getProperty("s3.aws-credentials");
        feedBucket = ApiMain.config.getProperty("s3.feeds-bucket");
        dataDirectory = ApiMain.config.getProperty("application.data");
        String[] feedList = {"9d464404-fae7-4f08-8cad-207554a61cbc"};
        initialize(s3credentials, workOffline, feedBucket, dataDirectory, feedList);
        Routes.routes();
    }

    /**
     * initialize the database
     */
    public static void initialize(String dataDirectory) throws IOException {
        initialize(null, true, "", dataDirectory, null);
    }
    public static void initialize(String s3credentials, Boolean workOffline, String feedBucket, String dataDirectory) throws IOException {
        initialize(s3credentials, workOffline, feedBucket, dataDirectory, null);
    }
    public static void initialize(String s3credentials, Boolean workOffline, String feedBucket, String dataDirectory, String[] feedList) throws IOException {

        // Load GTFS datasets

        // Use s3
        ApiMain.feedSources = Maps.newHashMap();

        if(!workOffline) {

            // connect to s3
            if (s3credentials != null) {
                AWSCredentials creds = new ProfileCredentialsProvider(s3credentials, "default").getCredentials();
                s3 = new AmazonS3Client(creds);
            }
            else {
                // default credentials providers, e.g. IAM role
                s3 = new AmazonS3Client();
            }
            if (feedList == null){
                gtfsList = s3.listObjects(feedBucket);
                int count = 0;
                for (S3ObjectSummary objSummary : gtfsList.getObjectSummaries()){
                    String feedId = objSummary.getKey();
                    System.out.println("Loading feed: " + feedId);
                    String keyName = objSummary.getKey();
                    InputStream obj = s3.getObject(feedBucket, keyName).getObjectContent();

                    // create tempfile so we can pass GTFSFeed.fromFile a string file path
                    File tempFile = File.createTempFile(keyName, ".zip");
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
                if (count == 0){
                    System.out.println("No feeds found");
                }
            }
            else {
                // cycle through list of feedSourceIds
                for (String feedSource : feedList) {
                    try {
                        System.out.println("Downloading feed from s3");
                        S3Object object = s3.getObject(
                                new GetObjectRequest(feedBucket, feedSource + ".zip"));
                        InputStream obj = object.getObjectContent();
                        System.out.println(object.getObjectMetadata().getETag());

                        // create file so we can pass GTFSFeed.fromFile a string file path
                        String tDir = System.getProperty("java.io.tmpdir");
                        File tempFile = new File(tDir, feedSource + ".zip");
                        System.out.println(tempFile.getAbsolutePath());

                        try (FileOutputStream out = new FileOutputStream(tempFile)) {
                            IOUtils.copy(obj, out);
                        }
                        ApiMain.feedSources.put(feedSource, new FeedSource(tempFile.getAbsolutePath()));
                        tempFile.deleteOnExit();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (AmazonServiceException ase) {
                        System.out.println("Error downloading from s3");
                        ase.printStackTrace();
                    }
                }
            }

        }

        // Use application.data directory from config
        else{
            final File folder = new File(dataDirectory);
            int count = 0;
            for (File file  : folder.listFiles()) {
                if (file.getName().endsWith(".zip")){
                    String feedId = file.getName().split(".zip")[0];
                    String feedPath = file.getAbsolutePath();
                    System.out.println("Loading feed: " + feedId + " at " + feedPath);

                    // TODO: use gtfs-lib provided feedId (feedId from feed or filename minus ".zip"
                    ApiMain.feedSources.put(feedId, new FeedSource(feedPath));
                    count++;
                }
            }
            if (count == 0){
                System.out.println("No feeds found");
            }
        }

    }

}