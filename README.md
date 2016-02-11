# gtfs-api [![Build Status](https://travis-ci.org/conveyal/gtfs-api.svg?branch=master)](https://travis-ci.org/conveyal/gtfs-api)

Simple spark rest api using [gtfs-lib](https://github.com/conveyal/gtfs-lib).

Build with `mvn clean package`.

## Getting Started

`cp application.conf.template application.conf`

Change `s3.feeds-bucket` to s3 bucket where feeds are stored or, to work locally, update `application.data` to local path with gtfs and switch `s3.work-offline` to `true`.

After packaging with Maven, run with `java -jar target/gtfs-api.jar`.

## API Endpoints

* `localhost:4567/feeds` - list feed IDs
* `localhost:4567/stops?feed=<FEED-ID>` - list all stops for feed(s)
* `localhost:4567/stops?name=<STRING-QUERY>&feed=<FEED-ID>` - query routes by name (best guess based on string provided) for feed(s)
* `localhost:4567/stops?max_lat=<LAT>&max_lon=<LON>&min_lat=<LAT>&min_lon=<LON>&feed=<FEED-ID>` - query stops by bounding box for feed(s)
* `localhost:4567/stops?lat=<LAT>&lon=<LON>&radius=<RADIUS_KM>&feed=<FEED-ID>` - query stops by lat/lon and radius for feed(s)
* `localhost:4567/routes?feed=<FEED-ID>` - list all routes for feed(s)
* `localhost:4567/routes?name=<STRING-QUERY>&feed=<FEED-ID>` - query routes by name (best guess based on string provided) for feed(s)
* `localhost:4567/trips?feed=<FEED-ID>` - list all trips for feed(s)
* `localhost:4567/trips?route=<ROUTE-ID>&feed=<FEED-ID>` - query trips by route_id for feed(s)