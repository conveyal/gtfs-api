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
        List<String> eTags = initialize(null, false, "datatools-gtfs-mtc", null, null, "completed/");
        loadFeedFromBucket(feedBucket, "a9b462ce-5c94-429a-8186-28ac84c3a02c", "completed/");
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

                    // drop .zip
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
    public static String loadFeedFromBucket(String feedBucket, String keyName, String prefix){



        // drop .zip and any folder prefix
        String feedId = keyName.split(".zip")[0];

        if (feedId.contains("/")){
            String[] pathParts = feedId.split("/");

            // feedId equals last part
            feedId = pathParts[pathParts.length - 1];
        }

        String feedPath;
        String eTag = "";
        String tDir = System.getProperty("java.io.tmpdir");

        if (prefix != null) {
            feedPath = prefix + feedId + ".zip";
        }
        else {
            feedPath = feedId + ".zip";
        }
        try {

            System.out.println("Downloading feed from s3 at " + feedPath);

            S3Object object = s3.getObject(
                    new GetObjectRequest(feedBucket, feedPath));
            InputStream obj = object.getObjectContent();
            eTag = object.getObjectMetadata().getETag();
            System.out.println(eTag);

            // create file so we can pass GTFSFeed.fromFile a string file path
            File tempFile = new File(tDir, feedId + ".zip");
            System.out.println(tempFile.getAbsolutePath());

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                IOUtils.copy(obj, out);
            }
            ApiMain.feedSources.put(feedId, new FeedSource(tempFile.getAbsolutePath()));
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
            List<S3ObjectSummary> summaries = gtfsList.getObjectSummaries();

            // number of feeds to load is size minus one (because folder is listed here also)
            System.out.println(summaries.size() - 1 + " feeds to load");

            for (S3ObjectSummary objSummary : summaries){

                String keyName = objSummary.getKey();

                // drop .zip and any folder prefix
                String feedId = keyName.split(".zip")[0];

                if (feedId.equals(prefix)){
                    continue;
                }
                if (feedId.contains("/")){
                    String[] pathParts = feedId.split("/");

                    // feedId equals last part
                    feedId = pathParts[pathParts.length - 1];
                }
                String eTag = objSummary.getETag();

                System.out.println("Loading feed: " + feedId);

                String tDir = System.getProperty("java.io.tmpdir");

                try {
                    System.out.println("Downloading feed from s3 at " + keyName);

                    S3Object object = s3.getObject(
                            new GetObjectRequest(feedBucket, keyName));
                    InputStream obj = object.getObjectContent();
                    eTag = object.getObjectMetadata().getETag();
                    System.out.println(eTag);

                    // create file so we can pass GTFSFeed.fromFile a string file path
                    File tempFile = new File(tDir, feedId + ".zip");
                    System.out.println(tempFile.getAbsolutePath());

                    try (FileOutputStream out = new FileOutputStream(tempFile)) {
                        IOUtils.copy(obj, out);
                    }
                    ApiMain.feedSources.put(feedId, new FeedSource(tempFile.getAbsolutePath()));
                    tempFile.deleteOnExit();
                    eTags.add(eTag);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AmazonServiceException ase) {
                    System.out.println("Error downloading from s3 for " + keyName);
                    ase.printStackTrace();
                }

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
                eTags.add(loadFeedFromBucket(feedBucket, feedSource, prefix));
                count++;
            }
        }
        return eTags;
    }

}