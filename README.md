# gtfs-api [![Build Status](https://travis-ci.org/conveyal/gtfs-api.svg?branch=master)](https://travis-ci.org/conveyal/gtfs-api)

Simple spark rest api using [gtfs-lib](https://github.com/conveyal/gtfs-lib).

Build with `mvn clean package`.

## Getting Started

`cp application.conf.template application.conf`

Change `s3.feeds-bucket` to s3 bucket where feeds are stored or, to work locally, update `application.data` to local path with gtfs and switch `s3.work-offline` to `true`.

After packaging with Maven, run with `java -jar target/gtfs-api.jar`.

## Feed Loading

GTFS feeds will be auto-loaded upon request by feed ID. The best way to get started is to dump a feed 
into your s3 bucket and navigate to `http://localhost:4567/routes?feed=<FEED-ID>`. (Note: `FEED-ID` 
will be the zip filename by default with special characters replaced by hyphens, e.g., `trimet_feed.zip` 
becomes `trimet-feed-zip`.)

## GraphQL

To use the GraphQL endpoint, install the GraphiQL app

`brew cask install graphiql`

or use the [ChromeiQL extension](https://chrome.google.com/webstore/detail/chromeiql/fkkiamalmpiidkljmicmjfbieiclmeij).

### The basics

For the unitiated, GraphQL is a flexible alternative to REST, whereby clients can request only the data elements
they need. It also permits requests clients to request data that is nested according to relationships between types.

As an example in the context of GTFS, if we're looking for all of the routes that serve a specific stop,
we might construct the following query:

```
query routesForStopQuery($feedId: [String], $stopId: [String]) {
  stops(feed_id: $feedId, stop_id: $stopId) {
    stop_name
    stop_id
    stop_lat
    stop_lon
    routes {
      route_id
      route_short_name
      patterns {
        name
        pattern_id
      }
    }
  }
}
```

The above request specifies a couple of variables (`feedId`, which is always required, and `stopId` for the specific stop(s))
as well as a list of fields the client would like back (including `stop_name`, `stop_id`, and `stop_lat`). The last item
at this level in the query is `routes`, which will return all of the routes that serve the containing stop. Underneath routes
are `patterns`, which in this context means all of the various patterns (unique stop sequences) for that route. We could
even travel one (or more) level deeper and ask for all of the stops for each pattern.
 
The various permutations of these fields and types are all documented in [schema.graphql](src/docs/schema.graphql).
Visiting the root GraphQL endpoint for `gtfs-api` (e.g., [http://localhost:4567](http://localhost:4567)) will return
the schema for the active version of `gtfs-api`.

### Sample GraphQL queries

You can find some sample GraphQL queries [here](https://github.com/conveyal/scenario-editor/blob/master/lib/graphql/query.js) or in [examples](src/examples).

## REST Endpoints

REST endpoints also work out of the box.

* `localhost:4567/feeds` - list feed IDs
* `localhost:4567/stops?feed=<FEED-ID>` - list all stops for feed(s)
* `localhost:4567/stops?name=<STRING-QUERY>&feed=<FEED-ID>` - query routes by name (best guess based on string provided) for feed(s)
* `localhost:4567/stops?max_lat=<LAT>&max_lon=<LON>&min_lat=<LAT>&min_lon=<LON>&feed=<FEED-ID>` - query stops by bounding box for feed(s)
* `localhost:4567/stops?lat=<LAT>&lon=<LON>&radius=<RADIUS_KM>&feed=<FEED-ID>` - query stops by lat/lon and radius for feed(s)
* `localhost:4567/routes?feed=<FEED-ID>` - list all routes for feed(s)
* `localhost:4567/routes?name=<STRING-QUERY>&feed=<FEED-ID>` - query routes by name (best guess based on string provided) for feed(s)
* `localhost:4567/trips?feed=<FEED-ID>` - list all trips for feed(s)
* `localhost:4567/trips?route=<ROUTE-ID>&feed=<FEED-ID>` - query trips by route_id for feed(s)
