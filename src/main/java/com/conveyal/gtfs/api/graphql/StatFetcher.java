package com.conveyal.gtfs.api.graphql;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Stop;
import com.conveyal.gtfs.stats.model.RouteStatistic;
import com.conveyal.gtfs.stats.model.StopStatistic;
import com.conveyal.gtfs.stats.model.TransferPerformanceSummary;
import graphql.schema.DataFetchingEnvironment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.conveyal.gtfs.api.util.GraphQLUtil.argumentDefined;

/**
 * Created by landon on 10/4/16.
 */
public class StatFetcher {
    public static StopStatistic fromStop(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Stop> stop = (WrappedGTFSEntity<Stop>) env.getSource();
        FeedSource fs = ApiMain.getFeedSource(stop.feedUniqueId);

        if (argumentDefined(env, "date") && argumentDefined(env, "from") && argumentDefined(env, "to")) {
            String d = (String) env.getArgument("date");
            long f = (long) env.getArgument("from");
            long t = (long) env.getArgument("to");

            LocalDate date = LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE); // 2011-12-03
            LocalTime from = LocalTime.ofSecondOfDay(f);
            LocalTime to = LocalTime.ofSecondOfDay(t);

            return new StopStatistic(fs.stats.stop, stop.entity.stop_id, date, from, to);
        }
        else {
            return null;
        }
    }

    public static RouteStatistic fromRoute(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Route> route = (WrappedGTFSEntity<Route>) env.getSource();
        FeedSource fs = ApiMain.getFeedSource(route.feedUniqueId);

        if (argumentDefined(env, "date") && argumentDefined(env, "from") && argumentDefined(env, "to")) {
            String d = (String) env.getArgument("date");
            long f = (long) env.getArgument("from");
            long t = (long) env.getArgument("to");

            LocalDate date = LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE); // 2011-12-03
            LocalTime from = LocalTime.ofSecondOfDay(f);
            LocalTime to = LocalTime.ofSecondOfDay(t);

            return new RouteStatistic(fs.stats.route, route.entity.route_id, date, from, to);
        }
        else {
            return null;
        }
    }

    public static List<TransferPerformanceSummary> getTransferPerformance(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Stop> stop = (WrappedGTFSEntity<Stop>) env.getSource();
        FeedSource fs = ApiMain.getFeedSource(stop.feedUniqueId);

        if (argumentDefined(env, "date")) {
            String d = (String) env.getArgument("date");
            LocalDate date = LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE); // 2011-12-03

            return fs.stats.stop.getTransferPerformance(stop.entity.stop_id, date);
        }
        return null;
    }
}
