package com.conveyal.gtfs.api;

import com.conveyal.gtfs.api.*;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Agency;
import com.conveyal.gtfs.model.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.googlecode.concurrenttrees.common.PrettyPrinter;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radix.node.util.PrettyPrintable;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import java.io.IOException;
import java.util.*;

import static spark.Spark.*;

/**
 * Created by landon on 2/3/16.
 */
public class ApiMain {
    public static GTFSFeed feed;
    public static ArrayList<GTFSFeed> feeds;
    public static Map feedAgencies;
    public static Map<String, GTFSFeed> feedMap;
//    public static STRtree routeIndex;
//    public static STRtree stopIndex;
//    public static RadixTree<Stop> stopTree;
//    public static RadixTree<Route> routeTree;
    public static Map<String, FeedSource> feedSources;

    public static void main(String[] args){

        initialize();

        Routes.routes();
    }

    /** initialize the database */
    public static void initialize () {
//        Load GTFS datasets
//        GTFSFeed[] feeds = new GTFSFeed[];
//        ApiMain.feeds = new ArrayList<GTFSFeed>();
//        ApiMain.feedAgencies = Maps.newHashMap();
//        ApiMain.feedMap = Maps.newHashMap();
        ApiMain.feedSources = Maps.newHashMap();
//        ApiMain.feed = GTFSFeed.fromFile("/Users/landon/Downloads/google_transit.zip");
        String[] listOfFeeds = new String[] {"/Users/landon/Downloads/google_transit.zip", "/Users/landon/Downloads/google_transit_staten_island.zip"};
        int count = 0;
        for (String feedPath : listOfFeeds){
            String feedId = "feed-" + String.valueOf(count);
            System.out.println("Loading feed: " + feedId);
            System.out.println("Loading feed: " + feedPath);
//            GTFSFeed feed = GTFSFeed.fromFile(feedPath);
//            FeedSource feedSource = new FeedSource(feedPath);
            ApiMain.feedSources.put(feedId, new FeedSource(feedPath));
            System.out.println(feedSources.get(feedId).feed.routes.values().iterator().next().route_short_name);
//            ApiMain.feeds.add(feed);
//            ApiMain.feedAgencies.put(feedId, feed.agency);
//            ApiMain.feedMap.put(feedId, feed);
            count++;
        }

    }
    public GTFSFeed getFeedByAgency(String agencyId) {
        return new GTFSFeed();
    }

    public String[] getAgencyIdsForFeed(GTFSFeed feed){
//        String[] agencies = new String[]{};
        return feed.agency.keySet().toArray(new String[feed.agency.size()]);
//        for(Agency agency : feed.agency.){
//            if (key == "agency_id"){
//                agencies.add(feed.agency.get("key"));
//            }
//        }
//        return agencies;
    }

    public Map<List<String>, List<String>> getStopsNearby(String lat, String lon){



        Map<List<String>, List<String>> nearbyStops = Maps.newHashMap();

        int radius = 1; // default radius is 1 mile
        nearbyStops = getStopsNearby(lat, lon, radius);

        return nearbyStops;
    }

    public Map<List<String>, List<String>> getStopsNearby(String lat, String lon, int radius){
        Map<List<String>, List<String>> nearbyStops = Maps.newHashMap();

        return nearbyStops;
    }

}
