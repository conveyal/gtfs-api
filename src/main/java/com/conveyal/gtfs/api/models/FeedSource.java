package com.conveyal.gtfs.api.models;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by landon on 2/8/16.
 */
public class FeedSource {

    public STRtree routeIndex;
    public STRtree stopIndex;
    public RadixTree<Stop> stopTree;
    public RadixTree<Route> routeTree;
    public GTFSFeed feed;

    public FeedSource(String path){
        this.feed = GTFSFeed.fromFile(path);
        feed.findPatterns();
        initIndexes();

    }

    public void initIndexes(){
        // Initialize and build route radix tree and spatial index
        this.routeIndex = new STRtree();
        this.routeTree = new ConcurrentRadixTree<Route>(new DefaultCharArrayNodeFactory());

        // spatial
        Set<Route> indexedRoutes = new HashSet<>();
        for (Pattern pattern : this.feed.patterns.values()){
            Route currentRoute = this.feed.trips.get(pattern.associatedTrips.get(0)).route;
//          TODO: check if list of routes already contains current route
            if (!indexedRoutes.contains(currentRoute)){
                //            System.out.println(this.feed.trips.get(pattern.associatedTrips.get(0)).trip_headsign);
                Envelope routeEnvelope = pattern.geometry.getEnvelopeInternal();
//            Envelope routeEnvelope = new Envelope(pattern.geometry.getEndPoint().getCoordinate());
//            Envelope routeEnvelope = new Envelope(new Coordinate(-122.0, 37.0));
//            Envelope routeEnvelope = new Envelope(pattern.geometry.getCentroid().getCoordinate());
                this.routeIndex.insert(routeEnvelope, pattern);
                indexedRoutes.add(currentRoute);
            }
        }
        this.routeIndex.build();

        // string
        for (Route route : this.feed.routes.values()){
            // TODO: consider concatenating short name and long name
            if (route.route_short_name != null && !route.route_short_name.isEmpty())
                this.routeTree.put(route.route_short_name.toUpperCase(), route);

            if (route.route_long_name != null && !route.route_long_name.isEmpty())
                this.routeTree.put(route.route_long_name.toUpperCase(), route);
        }

        // Initialize and build stop radix tree and spatial index
        this.stopIndex = new STRtree();
        this.stopTree = new ConcurrentRadixTree<Stop>(new DefaultCharArrayNodeFactory());
        for (Stop stop : this.feed.stops.values()){
            // spatial
            Coordinate stopCoords = new Coordinate(stop.stop_lon, stop.stop_lat);
            Envelope stopEnvelope = new Envelope(stopCoords);
            this.stopIndex.insert(stopEnvelope, stop);

            // string
            // TODO: consider concatenating stop_code and stop_name
            this.stopTree.put(stop.stop_name.toUpperCase(), stop);
            String stop_code;
            if (stop.stop_code != null){
                stop_code = stop.stop_code;
            }
            else {
                stop_code = stop.stop_id;
            }
            this.stopTree.put(stop_code.toUpperCase(), stop);
        }
        this.stopIndex.build();

        // TODO: what happens if new stops or routes need to be added to index (QuadTree)?

        // Print out stop tree results
//        PrettyPrinter.prettyPrint((PrettyPrintable) stopTree, System.out);


    }
}
