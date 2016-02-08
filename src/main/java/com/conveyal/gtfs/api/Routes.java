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
        get("/feeds", (request, response) -> mapper.writeValueAsString(ApiMain.feedAgencies));
        get("/feeds/:id", (request, response) -> mapper.writeValueAsString(ApiMain.feedAgencies.get(request.params("id"))));
        get("/feeds/:feed_id/agencies", (request, response) ->
                mapper.writeValueAsString(
                        ApiMain.feedMap.get(request.params("feed_id"))
                                .agency
                )
        );

        get("/feeds/:feed_id/agencies/:id", (request, response) -> {
            System.out.println(request.params());
            System.out.println(request.queryParams());
            System.out.println("agency request");
            return mapper.writeValueAsString(
                    ApiMain.feedMap.get(request.params("feed_id"))
                            .agency.get(request.params("id"))
            );
        });

        // Routes
        get("/routes", RoutesController::getRoutes, json);
        get("/routes/:id", RoutesController::getRoutes, json);
//        get("/routes", (request, response) -> mapper.writeValueAsString(ApiMain.feed.routes));


        // Stops
        get("/stops", StopsController::getStops, json);
        get("/stops/:id", StopsController::getStops, json);

        // TODO: Add endpoint for name search for both routes and stops.

        get("/patterns", (request, response) -> mapper.writeValueAsString(ApiMain.feed.findPatterns()));
//        get("/patterns/:id", (request, response) -> mapper.writeValueAsString(ApiMain.feed.findPatterns()));
        get("/shapes", (request, response) -> mapper.writeValueAsString(ApiMain.feed.shapes));
        get("/shapes/:id/shapepoints", (request, response) -> mapper.writeValueAsString(ApiMain.feed.shapes.get(request.params("id"))));

//        TODO:'/trips' route is causing infinite recursion between com.conveyal.gtfs.model.Service["calendar"]->com.conveyal.gtfs.model.Calendar["service"]
//        get("/trips", (request, response) -> mapper.writeValueAsString(ApiMain.feed.trips));
//        get("/trips/:id", (request, response) ->
//                mapper.writeValueAsString(ApiMain.feed.trips.get(request.params("id")))
//        );
        get("/trips/:id/stoptimes", (request, response) -> mapper.writeValueAsString(ApiMain.feed.getOrderedStopTimesForTrip(request.params("id"))));

        post("/hello", (request, response) ->
                "Hello World: " + request.body()
        );

        get("/private", (request, response) -> {
            response.status(401);
            return "Go Away!!!";
        });

        get("/users/:name", (request, response) -> "Selected user: " + request.params(":name"));

        get("/news/:section", (request, response) -> {
            response.type("text/xml");
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><news>" + request.params("section") + "</news>";
        });

        get("/protected", (request, response) -> {
            halt(403, "I don't think so!!!");
            return null;
        });

        get("/redirect", (request, response) -> {
            response.redirect("/news/world");
            return null;
        });

        get("/", (request, response) -> "GTFS Api");
    }
}
