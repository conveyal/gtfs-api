package com.conveyal.gtfs.api;

import com.conveyal.gtfs.api.*;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Agency;
import com.conveyal.gtfs.model.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.googlecode.concurrenttrees.common.PrettyPrinter;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radix.node.util.PrettyPrintable;
import com.sun.tools.doclint.Env;
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
    public static STRtree routeIndex;
    public static STRtree stopIndex;
    public static RadixTree<Stop> stopTree;
    public static RadixTree<Route> routeTree;
    public static void main(String[] args){

        initialize();

        Routes.routes();
    }

    /** initialize the database */
    public static void initialize () {
//        Load GTFS datasets
//        GTFSFeed[] feeds = new GTFSFeed[];
        ApiMain.feeds = new ArrayList<GTFSFeed>();
        ApiMain.feedAgencies = Maps.newHashMap();
        ApiMain.feedMap = Maps.newHashMap();
        ApiMain.feed = GTFSFeed.fromFile("/Users/landon/Downloads/google_transit.zip");
        String[] listOfFeeds = new String[] {"/Users/landon/Downloads/google_transit.zip", "/Users/landon/Downloads/google_transit_staten_island.zip"};
        int count = 0;
        for (String feedPath : listOfFeeds){
            String feedId = "feed-" + String.valueOf(count);
            System.out.println("Loading feed: " + feedId);
            GTFSFeed feed = GTFSFeed.fromFile(feedPath);
            ApiMain.feeds.add(feed);
            ApiMain.feedAgencies.put(feedId, feed.agency);
            ApiMain.feedMap.put(feedId, feed);
            count++;
        }

        initIndexes();
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

    public static void initIndexes(){
        // Initialize and build route radix tree and spatial index
        routeIndex = new STRtree();
        routeTree = new ConcurrentRadixTree<Route>(new DefaultCharArrayNodeFactory());

        // spatial
        for (Pattern pattern : feed.patterns.values()){
//            Envelope routeEnvelope = pattern.geometry.getEnvelopeInternal();
            Envelope routeEnvelope = new Envelope(pattern.geometry.getCentroid().getCoordinate());
            routeIndex.insert(routeEnvelope, feed.trips.get(pattern.associatedTrips.get(0)).route);
        }
        routeIndex.build();

        // string
        for (Route route : feed.routes.values()){
            // TODO: consider concatenating short name and long name
            routeTree.put(route.route_short_name.toUpperCase(), route);
            routeTree.put(route.route_long_name.toUpperCase(), route);
        }

        // Initialize and build stop radix tree and spatial index
        stopIndex = new STRtree();
        stopTree = new ConcurrentRadixTree<Stop>(new DefaultCharArrayNodeFactory());
        for (Stop stop : feed.stops.values()){
            // spatial
            Coordinate stopCoords = new Coordinate(stop.stop_lon, stop.stop_lat);
            Envelope stopEnvelope = new Envelope(stopCoords);
            stopIndex.insert(stopEnvelope, stop);

            // string
            // TODO: consider concatenating stop_code and stop_name
            stopTree.put(stop.stop_name.toUpperCase(), stop);
            String stop_code;
            if (stop.stop_code != null){
                stop_code = stop.stop_code;
            }
            else {
                stop_code = stop.stop_id;
            }
            stopTree.put(stop_code.toUpperCase(), stop);
        }
        stopIndex.build();

        // TODO: what happens if new stops or routes need to be added to index (QuadTree)?

        // Print out stop tree results
//        PrettyPrinter.prettyPrint((PrettyPrintable) stopTree, System.out);


    }

}
