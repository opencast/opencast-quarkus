package org.opencastproject.digest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.opencastproject.digest.DigestTestClient.digestFor;

/**
 * The application starts, and works, with the nonce secret stored encrypted, as in the demo application.
 * The configuration is the same as there: the plain value is
 * {@code 0d1e2f3a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f9a0b1c2d3e4f50}, encrypted with the key
 * {@code demo-encryption-key} by SmallRye Config's {@code encryptor.java}
 * (<a href="https://quarkus.io/guides/config-secrets">Secrets in configuration</a>).
 * <p>
 * The nonce secret is only used to sign nonces, so the application only works if it was decrypted to a usable key:
 * a wrong encryption key or a corrupted value would make it fail at startup, see {@link DigestConfigurationCheck}.
 */
@QuarkusTest
@TestProfile(DigestAuthenticationMechanismEncryptedSecretIntegrationTest.WithEncryptedSecret.class)
class DigestAuthenticationMechanismEncryptedSecretIntegrationTest {
    private static final String PATH = "/hello";

    @Test
    void anEncryptedNonceSecretIsDecryptedAndUsed() {
        given()
                .header("Authorization", digestFor(PATH, "alice", "alice-password", "GET", "00000001", "cnonce"))
                .when().get(PATH)
                .then()
                .statusCode(200)
                .body(equalTo("Hello, alice!"));
    }

    public static class WithEncryptedSecret implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key", "demo-encryption-key",
                    "digest.nonce-secret", "${aes-gcm-nopadding::DI67TgVYYwyJlnvdkTdIp6wq7_fCgAY-gTwW25QGPKy2p6EfxPy2CZTLUOwvBBpP0J1Oz67thXIw54b1dqkTl2dp8dQMZ_KLCvTQL0kXFNZ-InmQpYc7HLKqaptV}");
        }
    }
}
