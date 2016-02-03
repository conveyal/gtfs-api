package com.conveyal.gtfs.api;

import static spark.Spark.*;
import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by landon on 2/3/16.
 */
public class Routes {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void routes() {

        //  port(5678); <- Uncomment this if you want spark to listen to port 5678 in stead of the default 4567

        get("/hello", (request, response) -> "Hello yo!");

//        get("/routes", (request, response) -> "Hello yo!");
        get("/routes", (request, response) -> mapper.writeValueAsString(ApiMain.feed.routes));
        get("/routes/:id", (request, response) ->
                mapper.writeValueAsString(ApiMain.feed.routes.get(request.params("id")))
        );
        get("/stops", (request, response) -> mapper.writeValueAsString(ApiMain.feed.stops));
        get("/stops/:id", (request, response) ->
                mapper.writeValueAsString(ApiMain.feed.stops.get(request.params("id")))
        );
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
