package com.conveyal.gtfs.api.controllers;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.model.Agency;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by landon on 2/9/16.
 */
public class FeedController {
    public static List<Object> getFeeds(Request req, Response res){
        // return all currently registered feeds if no param specified
        if (req.params().size() == 0 && req.queryParams().size() == 0){
            return ApiMain.registeredFeedSources.stream()
                    .collect(Collectors.toList());
        }
        // get and return specific feed for single param
        if (req.params("id") != null){
            return ApiMain.getFeedSource(req.params("id")).feed.agency.values().stream()
                    .collect(Collectors.toList());
        }
        // get and return multiple specified feeds for comma separated list param
        if (req.queryParams("id") != null){
            List<String> idList = Arrays.asList(req.queryParams("id").split(","));
            ApiMain.getFeedSources(idList);
            return ApiMain.registeredFeedSources.stream()
                    .collect(Collectors.toList());
        }
        return null;
    }
}
