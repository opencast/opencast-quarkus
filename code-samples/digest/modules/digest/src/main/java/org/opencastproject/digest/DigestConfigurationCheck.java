package org.opencastproject.digest;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Makes an unusable {@link DigestSecurityConfig} fail the application at startup.
 * <p>
 * This is needed because of the {@link io.smallrye.config.Secret} that the nonce secret is wrapped in:
 * Quarkus validates a plain configuration property when it starts, but a {@code Secret} is only converted
 * when its value is read, and a missing one is read as {@code null}.
 * {@link DigestAuthenticationMechanism} reads it, but Quarkus only creates it on the first request,
 * so without this bean a missing or invalid secret would not show until then.
 * {@link Startup} makes Quarkus create this bean during startup, and with it read the secret.
 */
@Startup
@ApplicationScoped
class DigestConfigurationCheck {
    DigestConfigurationCheck(DigestSecurityConfig config) {
        if (config.nonceSecret().get() == null) {
            throw new IllegalStateException(
                    "digest.nonce-secret must be set to 64 hex digits, e.g. generated with `openssl rand -hex 32`");
        }
    }
}
