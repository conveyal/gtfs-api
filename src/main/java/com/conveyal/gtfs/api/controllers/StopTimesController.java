package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.StopTime;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static spark.Spark.halt;

/**
 * Created by matthewc on 2/11/16.
 */
public class StopTimesController {
    public static Collection<StopTime> getStopTimes (Request req, Response res) {
        if (req.queryParams("feed") == null || req.params("id") == null) {
            halt(400, "Please specify a feed and trip");
        }

        FeedSource feed = ApiMain.feedSources.get(req.queryParams("feed"));

        if (feed == null) halt(404, "Feed not found");

        if (!feed.feed.trips.containsKey(req.params("id"))) halt(404, "Trip not found!");

        List<StopTime> ret = new ArrayList<>();
        feed.feed.getOrderedStopTimesForTrip(req.params("id")).forEach(ret::add);
        return ret;
    }
}
