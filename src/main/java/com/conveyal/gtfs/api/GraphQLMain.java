package com.conveyal.gtfs.api;

import com.conveyal.gtfs.api.controllers.GraphQLController;
import com.conveyal.gtfs.api.controllers.JsonTransformer;
import com.conveyal.gtfs.api.util.CorsFilter;
import com.conveyal.gtfs.storage.SqlLibrary;
import spark.ResponseTransformer;

import javax.sql.DataSource;
import java.sql.Connection;

import static spark.Spark.*;

/**
 * Test main method to set up a new-style (as of June 2017) GraphQL API
 *
 * What we're trying to provide is this:
 * The queries that analysis-ui makes are at https://github.com/conveyal/analysis-ui/blob/dev/lib/graphql/query.js ; note that feeds are wrapped in bundles in analysis-ui (we wrap the GTFS API types)

 GraphQL queries for datatools-ui are at https://github.com/catalogueglobal/datatools-ui/blob/dev/lib/gtfs/util/graphql.js.

 We also use a few of the REST endpoints in datatools-ui, including:
 stops/routes by bounding box
 stop/routes by text string search (route_long_name/route_short_name, stop_name/stop_id/stop_code)
 Feeds - to get a list of the feed_ids that have been loaded into the gtfs-api

 * Here are some sample database URLs
 * H2_FILE_URL = "jdbc:h2:file:~/test-db"; // H2 memory does not seem faster than file
 * SQLITE_FILE_URL = "jdbc:sqlite:/Users/abyrd/test-db";
 * POSTGRES_LOCAL_URL = "jdbc:postgresql://localhost/catalogue";
 */
public class GraphQLMain {

    private static final ResponseTransformer JSON_TRANSFORMER = new JsonTransformer();

    public static DataSource dataSource;

    /**
     * Here are some sample database URLs
     * H2_FILE_URL = "jdbc:h2:file:~/test-db"; // H2 memory does not seem faster than file
     * SQLITE_FILE_URL = "jdbc:sqlite:/Users/abyrd/test-db";
     * POSTGRES_LOCAL_URL = "jdbc:postgresql://localhost/catalogue";
     */
    public static void main (String[] args) {
        String databaseUrl = args[0];
        ApiMain.initialize(null, null, "/Users/abyrd/gtfs/test");
        GraphQLMain.dataSource = SqlLibrary.createDataSource(databaseUrl);
        CorsFilter.apply();
        // Can we just pass in reference objectMapper::writeValueAsString? Why the mix-ins in jsonTransformer?
        get("/graphql", GraphQLController::get, JSON_TRANSFORMER);
        post("/graphql", GraphQLController::post, JSON_TRANSFORMER);
        get("/graphql/schema", GraphQLController::getSchema, JSON_TRANSFORMER);
        post("/graphql/schema", GraphQLController::getSchema, JSON_TRANSFORMER);
    }

}

