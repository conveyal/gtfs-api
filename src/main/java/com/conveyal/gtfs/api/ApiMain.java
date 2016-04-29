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
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final Logger LOG = LoggerFactory.getLogger(ApiMain.class);
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
        LOG.info(eTags.toString());
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
            if (feedList == null) {
                return loadFeedsFromDirectory(dataDirectory);
            }
            else {
                List<String> eTagList = new ArrayList<>();
                for (String feed : feedList) {
                    eTagList.add(loadFeedFromPath(dataDirectory + "/" + feed));
                }
                return eTagList;
            }
        }
    }

    public static String getMd5(File file) {
        byte[] bytesOfMessage = new byte[0];
        try {
            bytesOfMessage = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] md5 = md.digest(bytesOfMessage);

        String hashtext = DigestUtils.md5Hex(md5);

        return hashtext;
    }

    public static List<String> loadFeedsFromDirectory(String dir) {
        List<String> eTagList = new ArrayList<>();
        final File folder = new File(dir);
        int count = 0;
        for (File file  : folder.listFiles()) {
            if (file.getName().endsWith(".zip")){
                String feedPath = file.getAbsolutePath();
                String eTag = loadFeedFromPath(feedPath);
                count++;
                eTagList.add(eTag);
            }
        }
        if (count == 0){
            LOG.info("No feeds found");
        }
        return eTagList;
    }

    public static String loadFeedFromPath(String path){
        String feedId = getFeedIdFromPath(path);
        return loadFeedFromPath(path, feedId);
    }

    public static String loadFeedFromPath(String path, String feedId){
        System.out.println("Loading feed " + feedId + " at " + path);
        FeedSource fs = new FeedSource(path);
        ApiMain.feedSources.put(feedId, fs);
        File tempFile = new File(path);
        tempFile.deleteOnExit();
        return getMd5(tempFile);
    }

    public static String loadFeedFromFile(File file){
        String path = file.getAbsolutePath();
        String feedId = getFeedIdFromPath(path);
        return loadFeedFromFile(file, feedId);
    }

    public static String loadFeedFromFile(File file, String feedId){
        String path = file.getAbsolutePath();
        System.out.println("Loading feed " + feedId + " at " + path);
        FeedSource fs = new FeedSource(path);
        ApiMain.feedSources.put(feedId, fs);
        return getMd5(file);
    }

    public static String getFeedIdFromPath(String path) {
        String feedId = path.split(".zip")[0];

        if (feedId.contains("/")){
            String[] pathParts = feedId.split("/");

            // feedId equals last part
            feedId = pathParts[pathParts.length - 1];
        }
        return feedId;
    }

    public static String loadFeedFromBucket(String feedBucket, String keyName, String prefix){

        // drop .zip and any folder prefix
        String feedId = getFeedIdFromPath(keyName);

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
            LOG.info("Downloading feed from s3 at " + feedPath);
            S3Object object = s3.getObject(
                    new GetObjectRequest(feedBucket, feedPath));
            InputStream obj = object.getObjectContent();
            eTag = object.getObjectMetadata().getETag();

            // create file so we can pass GTFSFeed.fromFile a string file path
            File tempFile = new File(tDir, feedId + ".zip");
            LOG.info("temp file at " + tempFile.getAbsolutePath());

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                IOUtils.copy(obj, out);
            }
            ApiMain.feedSources.put(feedId, new FeedSource(tempFile.getAbsolutePath()));
            tempFile.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AmazonServiceException ase) {
            LOG.error("Error downloading from s3 for " + feedPath);
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
            LOG.info(summaries.size() - 1 + " feeds to load");

            for (S3ObjectSummary objSummary : summaries){


                String keyName = objSummary.getKey();

                // skip keyName if we're just looking at the prefix
                if (keyName.equals(prefix)){
                    continue;
                }
                String eTag = loadFeedFromBucket(feedBucket, keyName, prefix);
                eTags.add(eTag);
                count++;
            }
            if (count == 0){
                LOG.info("No feeds found");
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