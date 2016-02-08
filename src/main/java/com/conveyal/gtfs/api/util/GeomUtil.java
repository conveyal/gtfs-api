package com.conveyal.gtfs.api.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Created by landon on 2/8/16.
 */
public class GeomUtil {
    public static Envelope getBoundingBox(Coordinate coordinate, Double radius){
        Envelope boundingBox;

        double R = 6371;  // earth radius in km

        // radius argument is also in km

        double x1 = coordinate.x - Math.toDegrees(radius/R/Math.cos(Math.toRadians(coordinate.y)));

        double x2 = coordinate.x + Math.toDegrees(radius/R/Math.cos(Math.toRadians(coordinate.y)));

        double y1 = coordinate.y + Math.toDegrees(radius/R);

        double y2 = coordinate.y - Math.toDegrees(radius/R);

        boundingBox = new Envelope(x1, x2, y1, y2);

        return boundingBox;
    }
}
