package org.opencastproject.digest;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.Secret;
import io.smallrye.config.WithDefault;

import java.time.Duration;

/**
 * Configures {@link DigestAuthenticationMechanism}.
 * The users it authenticates come from the application's {@link DigestCredentialLookup} bean.
 */
@ConfigMapping(prefix = "digest")
public interface DigestSecurityConfig {
    /**
     * The realm advertised in the {@code WWW-Authenticate} challenge, and
     * mixed into every computed digest.
     */
    @WithDefault("Quarkus Digest Example")
    String realm();

    /**
     * How long an issued nonce remains acceptable.
     */
    @WithDefault("PT5M")
    Duration nonceValidity();

    /**
     * The key that nonces are signed with: at least 32 random bytes as 64 hex digits,
     * which is what {@code openssl rand -hex 32} prints.
     * There is no default, as a random key per start would invalidate all outstanding nonces on every restart
     * and make every instance reject the others' nonces. It is required.
     * <p>
     * It is a {@link Secret}, so it does not show up in the configuration's property names,
     * and reading it through the programmatic configuration API fails.
     * Keep it out of files that are widely readable, e.g. by setting it through the environment variable
     * {@code DIGEST_NONCE_SECRET} or a mounted secret, or by writing it as an encrypted value with SmallRye Config's
     * {@code smallrye-config-crypto} ({@code ${aes-gcm-nopadding::...}}).
     * <p>
     * Replay tracking is kept per instance, so a secret shared between instances
     * also lets a captured request be replayed against another instance.
     * <p>
     * A {@code Secret} is only converted when its value is read, and a missing one is read as {@code null}.
     * {@link DigestConfigurationCheck} reads it at startup, so that a missing or invalid value fails the application.
     */
    Secret<NonceSecret> nonceSecret();
}
