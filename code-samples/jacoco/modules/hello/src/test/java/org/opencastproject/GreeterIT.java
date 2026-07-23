package org.opencastproject;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class GreeterIT {

    @Test
    void shouldReturnGreetingViaRest() {
        given()
            .when().get("/hello/World")
            .then()
            .statusCode(200)
            .body(is("Hello, World!"));
    }
}
