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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        String[] feedList = {"a9b462ce-5c94-429a-8186-28ac84c3a02c"};
//        List<String> eTags = initialize(s3credentials, workOffline, feedBucket, dataDirectory, feedList, "completed/");
        List<String> eTags = initialize(null, false, feedBucket, null, feedList, "completed/");
        System.out.println(eTags);
        Routes.routes("api");
    }

    /**
     * initialize the database
     */
    public static List<String> initialize(String dataDirectory) throws IOException {
        return initialize(null, true, "", dataDirectory, null, null);
    }
    public static List<String> initialize(String s3credentials, Boolean workOffline, String feedBucket, String dataDirectory) {
        return initialize(s3credentials, workOffline, feedBucket, dataDirectory, null, null);
    }
    public static List<String> initialize(String s3credentials, Boolean workOffline, String feedBucket, String dataDirectory, String[] feedList, String prefix) {

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
            return loadFeedsFromBucket(feedBucket, feedList, prefix);
        }

        // Use application.data directory from config
        else{
            List<String> fileList = new ArrayList<>();
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
                    fileList.add(feedId);

                    // TODO: use md5?
//                    MessageDigest md = null;
//                    try {
//                        md = MessageDigest.getInstance("MD5");
//                    } catch (NoSuchAlgorithmException e) {
//                        e.printStackTrace();
//                    }
//                    try (InputStream is = Files.newInputStream(Paths.get(feedPath));
//                         DigestInputStream dis = new DigestInputStream(is, md))
//                    {
//                    /* Read decorated stream (dis) to EOF as normal... */
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    byte[] digest = md.digest();
                }
            }
            if (count == 0){
                System.out.println("No feeds found");
            }
            return fileList;
        }

    }
    public static String loadFeedFromBucket(String feedBucket, String feedSource, String prefix){
        String feedPath;
        String eTag = "";
        String tDir = System.getProperty("java.io.tmpdir");
        if (prefix != null) {
            feedPath = prefix + feedSource + ".zip";
        }
        else {
            feedPath = feedSource + ".zip";
        }
        try {

            System.out.println("Downloading feed from s3 at " + feedPath);

            S3Object object = s3.getObject(
                    new GetObjectRequest(feedBucket, feedPath));
            InputStream obj = object.getObjectContent();
            eTag = object.getObjectMetadata().getETag();
            System.out.println(eTag);

            // create file so we can pass GTFSFeed.fromFile a string file path
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
            System.out.println("Error downloading from s3 for " + feedPath);
            ase.printStackTrace();
        }
        return eTag;
    }

    public static List<String> loadFeedsFromBucket(String feedBucket){
        return loadFeedsFromBucket(feedBucket, null, null);
    }
    public static List<String> loadFeedsFromBucket(String feedBucket, String prefix){
        return loadFeedsFromBucket(feedBucket, null, prefix);
    }
    public static List<String> loadFeedsFromBucket(String feedBucket, String[] feedList, String prefix){
        List<String> eTags = new ArrayList();
        if (feedList == null){
            if (prefix == null)
                gtfsList = s3.listObjects(feedBucket);
            else
                gtfsList = s3.listObjects(feedBucket, prefix);

            int count = 0;
            for (S3ObjectSummary objSummary : gtfsList.getObjectSummaries()){
                String feedId = objSummary.getKey();
                String eTag = objSummary.getETag();

                System.out.println("Loading feed: " + feedId);
                String keyName = objSummary.getKey();
                InputStream obj = s3.getObject(feedBucket, keyName).getObjectContent();

                // create tempfile so we can pass GTFSFeed.fromFile a string file path
                File tempFile = null;
                try {
                    tempFile = File.createTempFile(keyName, ".zip");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tempFile.getAbsolutePath();

                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    IOUtils.copy(obj, out);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ApiMain.feedSources.put(feedId, new FeedSource(tempFile.getAbsolutePath()));
                eTags.add(eTag);
                tempFile.deleteOnExit();
                count++;
            }
            if (count == 0){
                System.out.println("No feeds found");
            }
        }
        // if feedList != null
        else {
            // cycle through list of feedSourceIds
            int count = 0;
            for (String feedSource : feedList) {
                eTags.add(loadFeedFromBucket(feedSource, prefix));
                count++;
            }
        }
        return eTags;
    }

}