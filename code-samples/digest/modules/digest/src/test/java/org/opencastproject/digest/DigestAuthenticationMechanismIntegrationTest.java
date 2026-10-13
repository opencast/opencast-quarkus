package org.opencastproject.digest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;


import static io.restassured.RestAssured.given;
import static org.opencastproject.digest.DigestTestClient.digestFor;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.containsString;

/**
 * Exercises {@link DigestAuthenticationMechanism} wired up in a real, running Quarkus
 * application against {@link TestResource}, a genuine {@code @RolesAllowed} endpoint.
 */
@QuarkusTest
class DigestAuthenticationMechanismIntegrationTest {
    private static final String PATH = "/hello";
    private static final String PUBLIC_PATH = "/public";

    @Test
    void unauthenticatedRequestIsChallenged() {
        given()
                .when().get(PATH)
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", startsWith("Digest realm=\"" + DigestTestClient.REALM + "\""));
    }

    @Test
    void correctDigestResponseIsAccepted() {
        String authorization = digestFor(PATH, "alice", "alice-password", "GET", "00000001", "cnonce-1");

        given()
                .header("Authorization", authorization)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body(equalTo("Hello, alice!"));
    }

    @Test
    void wrongPasswordIsRejected() {
        String authorization = digestFor(PATH, "alice", "wrong-password", "GET", "00000001", "cnonce-2");

        given()
                .header("Authorization", authorization)
                .when().get(PATH)
                .then()
                .statusCode(401);
    }

    @Test
    void failedAuthenticationIsChallengedAgain() {
        String authorization = digestFor(PATH, "alice", "wrong-password", "GET", "00000001", "cnonce-4");

        given()
                .header("Authorization", authorization)
                .when().get(PATH)
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", startsWith("Digest realm=\"" + DigestTestClient.REALM + "\""));
    }

    @Test
    void publicEndpointWithoutCredentialsIsServed() {
        given().when().get(PUBLIC_PATH).then().statusCode(200).body(equalTo("public"));
    }

    @Test
    void publicEndpointRejectsDigestCredentialsWithWrongPassword() {
        String authorization = digestFor(PATH, PUBLIC_PATH, "alice", "wrong-password", "GET", "00000001", "cnonce-5");

        given()
                .header("Authorization", authorization)
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    void publicEndpointRejectsMalformedDigestCredentials() {
        given()
                .header("Authorization", "Digest garbage")
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    void publicEndpointIgnoresBareDigestScheme() {
        given()
                .header("Authorization", "Digest")
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(200);
    }

    @Test
    void publicEndpointIgnoresAuthorizationHeaderWithoutScheme() {
        given()
                .header("Authorization", "=")
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(200);
    }

    @Test
    void publicEndpointIgnoresSchemesThatMerelyStartWithDigest() {
        given()
                .header("Authorization", "DigestFoo garbage")
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(200);
    }

    @Test
    void publicEndpointAcceptsCorrectDigestCredentials() {
        String authorization = digestFor(PATH, PUBLIC_PATH, "alice", "alice-password", "GET", "00000001", "cnonce-6");

        given()
                .header("Authorization", authorization)
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(200)
                .body(equalTo("public"));
    }

    @Test
    void credentialsOfOtherSchemesAreIgnored() {
        given()
                .header("Authorization", "Bearer some-token")
                .when().get(PUBLIC_PATH)
                .then()
                .statusCode(200);
    }

    @Test
    void digestCredentialsAreFoundAmongSeveralAuthorizationHeaders() {
        String authorization = digestFor(PATH, "alice", "alice-password", "GET", "00000001", "cnonce-7");

        given()
                .header("Authorization", "Bearer some-token")
                .header("Authorization", authorization)
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body(equalTo("Hello, alice!"));
    }

    @Test
    void tamperedNonceIsRejectedWithoutStaleFlag() {
        String authorization = digestFor(PATH, "alice", "alice-password", "GET", "00000001", "cnonce-3")
                .replace("nonce=\"", "nonce=\"tampered-");

        given()
                .header("Authorization", authorization)
                .when().get(PATH)
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", not(containsString("stale=true")));
    }
}
