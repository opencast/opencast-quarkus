package org.opencastproject.digest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.opencastproject.digest.DigestTestClient.digestFor;

/**
 * Isolates the {@code stale=true} rechallenge case (RFC 7616 §3.3) against a real,
 * running application, which needs a nonce that actually expires between being
 * issued and being answered.
 */
@QuarkusTest
@TestProfile(DigestAuthenticationMechanismStaleNonceIntegrationTest.ShortNonceValidity.class)
class DigestAuthenticationMechanismStaleNonceIntegrationTest {
    private static final String PATH = "/hello";

    @Test
    void expiredNonceIsReportedAsStale() throws InterruptedException {
        String authorization = digestFor(PATH, "alice", "alice-password", "GET", "00000001", "cnonce-1");
        Thread.sleep(50);

        given()
                .header("Authorization", authorization)
                .when().get(PATH)
                .then()
                .statusCode(401)
                .header("WWW-Authenticate", containsString("stale=true"));
    }

    public static class ShortNonceValidity implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("digest.nonce-validity", "PT0.001S");
        }
    }
}
