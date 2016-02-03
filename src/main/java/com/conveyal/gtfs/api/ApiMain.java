package com.conveyal.gtfs.api;

import com.conveyal.gtfs.api.*;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.*;
import java.io.IOException;

import static spark.Spark.*;

/**
 * Created by landon on 2/3/16.
 */
public class ApiMain {
    public static GTFSFeed feed;
    public static void main(String[] args){

        initialize();

        Routes.routes();
    }

    /** initialize the database */
    public static void initialize () {
//        Load GTFS datasets
        GTFSFeed feed = GTFSFeed.fromFile("/Users/landon/Downloads/google_transit.zip");
        ApiMain.feed = feed;
    }


}
