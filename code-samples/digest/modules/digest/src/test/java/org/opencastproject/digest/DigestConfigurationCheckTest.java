package org.opencastproject.digest;

import io.smallrye.config.Secret;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DigestConfigurationCheckTest {
    private static DigestSecurityConfig config(Secret<NonceSecret> nonceSecret) {
        return new DigestSecurityConfig() {
            @Override
            public String realm() {
                return "Test Realm";
            }

            @Override
            public Duration nonceValidity() {
                return Duration.ofMinutes(1);
            }

            @Override
            public Secret<NonceSecret> nonceSecret() {
                return nonceSecret;
            }
        };
    }

    @Test
    void acceptsAConfiguredSecret() {
        NonceSecret secret = NonceSecret.valueOf("ab".repeat(32));
        assertDoesNotThrow(() -> new DigestConfigurationCheck(config(() -> secret)));
    }

    @Test
    void failsWithAHelpfulMessageIfThereIsNoSecret() {
        // This is what a Secret that is not configured looks like: it is there, but has no value.
        var e = assertThrows(IllegalStateException.class, () -> new DigestConfigurationCheck(config(() -> null)));
        assertEquals("digest.nonce-secret must be set to 64 hex digits, e.g. generated with `openssl rand -hex 32`",
                e.getMessage());
    }
}
