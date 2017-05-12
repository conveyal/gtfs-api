package com.conveyal.gtfs.api;

import static spark.Spark.*;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.controllers.*;
import com.conveyal.gtfs.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.conveyal.gtfs.api.controllers.JsonTransformer;


/**
 * Created by landon on 2/3/16.
 */
public class Routes {

    private static final JsonTransformer json = new JsonTransformer();
    public static void routes() {
        routes("");
    }
    public static void routes(String prefix) {
        // prepend prefix with slash if it doesn't already have one and if it's not blank
        if (!prefix.startsWith("/") && !prefix.equals("")){
            prefix = "/" + prefix;
        }
        //  port(5678); // <- Uncomment this if you want spark to listen to port 5678 in stead of the default 4567

        get(prefix + "/hello", (request, response) -> "Hello, you!");
        get(prefix + "/feeds", FeedController::getFeeds, json);
        get(prefix + "/feeds/:id", FeedController::getFeeds, json);

        // Routes
        get(prefix + "/routes", RoutesController::getRoutes, json);
        get(prefix + "/routes/:id", RoutesController::getRoutes, json);


        // Stops
        get(prefix + "/stops", StopsController::getStops, json);
        get(prefix + "/stops/:id", StopsController::getStops, json);

//        TODO:'/trips' route is causing infinite recursion between com.conveyal.gtfs.model.Service["calendar"]->com.conveyal.gtfs.model.Calendar["service"]
        get(prefix + "/trips", TripsController::getTrips, json);

        get(prefix + "/trips/:id/stoptimes", StopTimesController::getStopTimes, json);

        get(prefix + "/patterns", PatternsController::getPatterns, json);


        get(prefix + "/", (request, response) -> "GTFS Api");

        get(prefix + "/graphql", GraphQLController::get, json);
        post(prefix + "/graphql", GraphQLController::get, json);

        get(prefix + "/graphql/schema", GraphQLController::getSchema, json);
        post(prefix + "/graphql/schema", GraphQLController::getSchema, json);


    }
}
