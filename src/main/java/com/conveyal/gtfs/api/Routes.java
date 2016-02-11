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

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final JsonTransformer json = new JsonTransformer();

    public static void routes() {

        //  port(5678); <- Uncomment this if you want spark to listen to port 5678 in stead of the default 4567

        get("/hello", (request, response) -> "Hello, you!");
        get("/feeds", FeedController::getFeeds, json);
        get("/feeds/:id", FeedController::getFeeds, json);
//        get("/feeds/:feed_id/agencies", FeedController::getFeeds, json);

//        get("/feeds/:feed_id/agencies/:id", (request, response) -> {
//            System.out.println(request.params());
//            System.out.println(request.queryParams());
//            System.out.println("agency request");
//            return mapper.writeValueAsString(
//                    ApiMain.feedMap.get(request.params("feed_id"))
//                            .agency.get(request.params("id"))
//            );
//        });

        // Routes
        get("/routes", RoutesController::getRoutes, json);
        get("/routes/:id", RoutesController::getRoutes, json);


        // Stops
        get("/stops", StopsController::getStops, json);
        get("/stops/:id", StopsController::getStops, json);

        // TODO: Add endpoint for name search for both routes and stops.

//        get("/patterns", (request, response) -> mapper.writeValueAsString(ApiMain.feed.findPatterns()));
//        get("/patterns/:id", (request, response) -> mapper.writeValueAsString(ApiMain.feed.findPatterns()));
//        get("/shapes", (request, response) -> mapper.writeValueAsString(ApiMain.feed.shapes));
//        get("/shapes/:id/shapepoints", (request, response) -> mapper.writeValueAsString(ApiMain.feed.shapes.get(request.params("id"))));

//        TODO:'/trips' route is causing infinite recursion between com.conveyal.gtfs.model.Service["calendar"]->com.conveyal.gtfs.model.Calendar["service"]
//        get("/trips", (request, response) -> mapper.writeValueAsString(ApiMain.feed.trips));
        get("/trips", TripsController::getTrips, json);
        get("/trips/:id", TripsController::getTrips, json);
//                mapper.writeValueAsString(ApiMain.feedSources.get().trips.get(request.params("id")))
//        );
//        get("/trips/:id/stoptimes", (request, response) -> mapper.writeValueAsString(ApiMain.feed.getOrderedStopTimesForTrip(request.params("id"))));


        get("/", (request, response) -> "GTFS Api");
    }
}
