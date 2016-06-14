package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.api.ApiMain;
import spark.Request;
import spark.Response;

/**
 * Created by landon on 2/9/16.
 */
public class FeedController {
    public static Object getFeeds(Request req, Response res){
        if (req.params().size() == 0 && req.queryParams().size() == 0){
            return ApiMain.registeredFeedSources;
        }
        if (req.params("id") != null){
            return ApiMain.getFeedSource(req.params("id")).feed.agency.values();
        }
        return null;
    }
}
