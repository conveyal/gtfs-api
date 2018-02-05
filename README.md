# gtfs-api [![Build Status](https://travis-ci.org/conveyal/gtfs-api.svg?branch=master)](https://travis-ci.org/conveyal/gtfs-api)

# NOTE this project is no longer maintained or used by Conveyal. The API definition has been merged into gtfs-lib. The GraphQL documentation should be copied over to gtfs-lib.

Simple spark rest api using [gtfs-lib](https://github.com/conveyal/gtfs-lib).

Build with `mvn clean package`.

## Getting Started

`cp application.conf.template application.conf`

Change `s3.feeds-bucket` to s3 bucket where feeds are stored or, to work
locally, update `application.data` to local path with gtfs, delete
`s3.feeds-bucket` and `s3.bucket-folder`, and switch `s3.work-offline`
to `true`.

After packaging with Maven, run with `java -jar target/gtfs-api.jar`.

## Feed Loading

GTFS feeds will be auto-loaded upon request by feed ID. The best way to
get started is to dump a feed into your s3 bucket and navigate to
`http://localhost:4567/api/routes?feed=<FEED-ID>`. (Note: `FEED-ID`
will be the zip filename by default.)

## GraphQL

To use the GraphQL endpoint, install the GraphiQL app

`brew cask install graphiql`

or use the [ChromeiQL extension](https://chrome.google.com/webstore/detail/chromeiql/fkkiamalmpiidkljmicmjfbieiclmeij).

### The basics

For the unitiated, GraphQL is a flexible alternative to REST, whereby
clients can request only the data elements they need. It also permits
requests clients to request data that is nested according to
relationships between types.

As an example in the context of GTFS, if we're looking for all of the
patterns (and their stops) that run on a specific route, we might
construct the following query:

```
query ($namespace: String, $route_id: String) {
  feed(namespace: $namespace) {
    feed_id
    feed_version
    filename
    routes (route_id: [$route_id]) {
      route_id
      route_name
      route_type
      patterns {
        pattern_id
        stops {
          stop_id
        }
      }
    }
  }
}
```

The above request specifies a couple of variables (`namespace`, which is
always required, and `route_id` for the specific route(s)) as well as a
list of fields the client would like back (including `route_id`,
`route_name`, and `route_type`). The last item at this level in the query is
`routes`, which will return all of the routes that serve the containing
stop. Underneath routes are `patterns`, which in this context means all
of the various patterns (unique stop sequences) for that route. We then
travel one more level deeper to ask for all of the stops for
each pattern.
 
The various permutations of these fields and types are all documented
in [schema.graphql](src/docs/schema.graphql). Visiting the root GraphQL
endpoint for `gtfs-api` (e.g., [http://localhost:4567](http://localhost:4567))
will return the schema for the active version of `gtfs-api`.

### Sample GraphQL queries

Below are some sample GraphQL queries for fetching GTFS entities and
load/validation result information for particular GTFS feeds.

When fetching sets of entities or validation errors, a default `limit` of
`50` (with `offset=0`) is applied to all queries unless otherwise
specified. Here is a list of additional optional params for each category:

- Validation errors: `namespace`, `error_type`
- GTFS entities
 - Routes: `route_id`
 - Stops: `stop_id`
 - Trips: `trip_id` and `route_id`
 - Stop Times: (none)
 - Services: `service_id`

#### Request a pattern for a feed with its stops and trips (and the trips' stop_times).
```
query ($namespace: String, $pattern_id: String) {
  feed(namespace: $namespace) {
    feed_id
    feed_version
    filename
    patterns (pattern_id: [$pattern_id]) {
      pattern_id
      route_id
      stops {
        stop_id
      }
      trips {
        trip_id
        pattern_id
        stop_times {
          stop_id
          trip_id
        }
      }
    }
  }
}
```

#### Request a route for a feed with its trips.
```
query ($namespace: String, $route_id: String) {
  feed(namespace: $namespace) {
    feed_id
    feed_version
    filename
    routes (route_id: [$route_id]) {
      route_id
      route_type
      trips {
        trip_id
        route_id
      }
    }
  }
}
```

#### Request a route for a feed with its patterns and the patterns' trips.
```
query ($namespace: String, $route_id: String) {
  feed(namespace: $namespace) {
    feed_id
    feed_version
    filename
    routes (route_id: [$route_id]) {
      route_id
      route_type
      patterns {
        pattern_id
        route_id
        trips {
          trip_id
          pattern_id
        }
      }
    }
  }
}
```

### Request all routes for a feed.
```
query ($namespace: String) {
  feed(namespace: $namespace) {
    feed_id
    feed_version
    filename
    routes {
      route_id
      route_type
    }
  }
}
```

### Request validation errors
Note: a list of the validation error types and their English language
descriptions can be found in the [`NewGtfsErrorType.java`](https://github.com/conveyal/gtfs-lib/blob/master/src/main/java/com/conveyal/gtfs/error/NewGTFSErrorType.java)
class in [conveyal/gtfs-lib](https://github.com/conveyal/gtfs-lib).
```
query ($namespace: String) {
  feed(namespace: $namespace) {
    feed_id
    feed_version
    filename
    ## Optional params for errors are namespace, error_type, limit, and offset
    errors {
      error_id
      error_type
      entity_type
      line_number
      entity_id
      entity_sequence
      bad_value
    }
  }
}
```

### Request tables and errors count
Note: a list of the validation error types and their English language
descriptions can be found in the [`NewGtfsErrorType.java`](https://github.com/conveyal/gtfs-lib/blob/master/src/main/java/com/conveyal/gtfs/error/NewGTFSErrorType.java)
class in [conveyal/gtfs-lib](https://github.com/conveyal/gtfs-lib).
```
query ($namespace: String) {
  feed(namespace: $namespace) {
    feed_id
    feed_version
    filename
    row_counts {
      stops
      stop_times
      trips
      routes
      calendar
      calendar_dates
      errors
    }
    error_counts {
      type
      count
    }
  }
}
```

## REST Endpoints

REST endpoints also work out of the box.

* `localhost:4567/api/feeds` - list feed IDs
* `localhost:4567/api/stops?feed=<FEED-ID>` - list all stops for feed(s)
* `localhost:4567/api/stops?name=<STRING-QUERY>&feed=<FEED-ID>` - query routes by name (best guess based on string provided) for feed(s)
* `localhost:4567/api/stops?max_lat=<LAT>&max_lon=<LON>&min_lat=<LAT>&min_lon=<LON>&feed=<FEED-ID>` - query stops by bounding box for feed(s)
* `localhost:4567/api/stops?lat=<LAT>&lon=<LON>&radius=<RADIUS_KM>&feed=<FEED-ID>` - query stops by lat/lon and radius for feed(s)
* `localhost:4567/api/routes?feed=<FEED-ID>` - list all routes for feed(s)
* `localhost:4567/api/routes?name=<STRING-QUERY>&feed=<FEED-ID>` - query routes by name (best guess based on string provided) for feed(s)
* `localhost:4567/api/trips?feed=<FEED-ID>` - list all trips for feed(s)
* `localhost:4567/api/trips?route=<ROUTE-ID>&feed=<FEED-ID>` - query trips by route_id for feed(s)
