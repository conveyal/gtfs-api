package com.conveyal.gtfs.api.models;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
//import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
//import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.suffix.ConcurrentSuffixTree;
import com.googlecode.concurrenttrees.suffix.SuffixTree;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by landon on 2/8/16.
 */
public class FeedSource {
    private static final Logger LOG = LoggerFactory.getLogger(FeedSource.class);

    public STRtree routeIndex;
    public STRtree stopIndex;
    public SuffixTree<Stop> stopTree;
    public SuffixTree<Route> routeTree;
    public GTFSFeed feed;

    /**
     * a unique ID for this feed source. Not a GTFS feed ID as they are not unique between versions of
     * the same feed.
     */
    public String id;

    public FeedSource (GTFSFeed feed) {
        this.feed = feed;
        // TODO this is hack to keep GTFS API working as it was before. We need actually to have unique IDs for feeds
        // independent of GTFS Feed ID as we track multiple versions of feeds.
        this.id = feed.feedId;
        initIndexes();
    }

    public void initIndexes(){
        // Initialize and build route radix tree and spatial index
        this.routeIndex = new STRtree();
        this.routeTree = new ConcurrentSuffixTree<Route>( new DefaultCharArrayNodeFactory() ) {};

        // spatial
        Set<Route> indexedRoutes = new HashSet<>();
        for (Pattern pattern : this.feed.patterns.values()){
            if (pattern.geometry == null) {
                LOG.warn("Pattern {} in feed {} has no geometry. It will not be included in indices. It has {} stops, if this is less than 2 this message is expected.",
                        pattern.pattern_id, feed.feedId, pattern.orderedStops.size());
                continue;
            }

            Route currentRoute = this.feed.routes.get(this.feed.trips.get(pattern.associatedTrips.get(0)).route_id);
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
        this.stopTree = new ConcurrentSuffixTree<Stop>( new DefaultCharArrayNodeFactory() );
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
