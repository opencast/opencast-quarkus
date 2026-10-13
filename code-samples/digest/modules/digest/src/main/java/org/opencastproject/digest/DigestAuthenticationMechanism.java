package org.opencastproject.digest;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Adapts {@link DigestAuthenticator} to Quarkus' {@link HttpAuthenticationMechanism} SPI.
 * <p>
 * Unlike {@code BasicAuthenticationMechanism}, this mechanism fully verifies credentials
 * itself (the whole point of Digest Authentication is that the server can do so without
 * ever seeing the client's password), so it builds a {@link SecurityIdentity} directly
 * instead of delegating to an {@link IdentityProviderManager}.
 */
@ApplicationScoped
public class DigestAuthenticationMechanism implements HttpAuthenticationMechanism {
    private static final String AUTHORIZATION = "Authorization";
    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String STALE_NONCE_KEY = DigestAuthenticationMechanism.class.getName() + "#staleNonce";

    private final DigestAuthenticator authenticator;
    private final DigestCredentialLookup credentials;

    /**
     * @param credentials the application's {@link DigestCredentialLookup} bean, which is required
     */
    @Inject
    public DigestAuthenticationMechanism(DigestSecurityConfig config, DigestCredentialLookup credentials) {
        this.authenticator = new DigestAuthenticator(config.realm(), config.nonceSecret().get(), config.nonceValidity());
        this.credentials = credentials;
    }

    /**
     * Like the built-in {@code BasicAuthenticationMechanism}, this ignores requests that do not carry
     * {@code Digest} credentials, so that other mechanisms or anonymous access can handle them,
     * and fails the authentication of requests whose {@code Digest} credentials do not verify,
     * even if the requested resource is public.
     */
    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        for (String header : context.request().headers().getAll(AUTHORIZATION)) {
            DigestVerification verification =
                    authenticator.verify(header, context.request().method().name(), context.request().uri(), credentials);
            switch (verification) {
                case DigestVerification.NotDigest notDigest -> {
                    // Not ours, so on to the next header.
                }
                case DigestVerification.Authenticated(String username) -> {
                    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                            .setPrincipal(new QuarkusPrincipal(username))
                            .addRole("user")
                            .build();
                    return Uni.createFrom().item(identity);
                }
                // No record pattern on purpose: JaCoCo 0.8.15 misses a branch for primitive components.
                case DigestVerification.Unauthenticated unauthenticated -> {
                    context.put(STALE_NONCE_KEY, unauthenticated.staleNonce());
                    return Uni.createFrom().failure(new AuthenticationFailedException());
                }
            }
        }
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        boolean stale = Boolean.TRUE.equals(context.get(STALE_NONCE_KEY));
        ChallengeData challenge = new ChallengeData(401, WWW_AUTHENTICATE, authenticator.issueChallenge(stale));
        return Uni.createFrom().item(challenge);
    }
}
