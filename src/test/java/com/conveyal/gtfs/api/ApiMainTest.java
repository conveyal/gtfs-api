package com.conveyal.gtfs.api;

import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * This class starts up a server and will do some very high level tests that make sure certain endpoints work.
 */
public class ApiMainTest {

    /**
     * Start up a server for testing
     */
    @BeforeClass
    public static void setUp() throws Exception {
        String[] args = {"./src/test/resources/test-application.conf"};
        ApiMain.main(args);
    }

    @Test
    public void helloRouteSaysHello() {
        String response = given().port(4567).get("/api/hello").asString();

        // assert that response has expected error message
        assertThat(response, equalTo("Hello, you!"));
    }
}
