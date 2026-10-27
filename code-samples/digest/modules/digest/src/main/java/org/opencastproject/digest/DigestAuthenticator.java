package org.opencastproject.digest;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Implements the server side of HTTP Digest Access Authentication as specified
 * by RFC 7616, restricted to {@code qop=auth} with the {@code MD5} and
 * {@code SHA-256} algorithms (no {@code -sess} variants, no {@code auth-int}).
 * <p>
 * Limitations to be aware of:
 * <ul>
 *   <li>Replay tracking is kept in memory per instance. Run a single instance,
 *       or give each instance its own nonce secret and route clients to the same instance;
 *       sharing a secret between instances lets captured requests be replayed against another one.</li>
 *   <li>Only the method and request target are covered by the digest. The request body and
 *       all other headers are not authenticated, so use TLS for anything beyond a demo.</li>
 *   <li>There is no rate limiting or lockout, so online password guessing must be limited elsewhere.</li>
 * </ul>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7616">RFC 7616 — HTTP Digest Access Authentication</a>
 */
public final class DigestAuthenticator {
    private static final String SCHEME = "Digest";
    private static final String UNKNOWN_USER_PASSWORD = "unknown-user";

    private final String realm;
    private final NonceManager nonceManager;

    public DigestAuthenticator(String realm, NonceSecret nonceSecret, Duration nonceValidity) {
        this(realm, new NonceManager(nonceSecret, nonceValidity));
    }

    DigestAuthenticator(String realm, NonceManager nonceManager) {
        this.realm = realm;
        this.nonceManager = nonceManager;
    }

    /**
     * Builds the value of a {@code WWW-Authenticate} response header (RFC 7616 §3.3)
     * challenging the client to authenticate.
     *
     * @param stale whether the challenge is being reissued only because the client's
     *              previous nonce expired, see {@link DigestVerification.Unauthenticated#staleNonce()}
     */
    public String issueChallenge(boolean stale) {
        StringBuilder challenge = new StringBuilder("Digest realm=\"").append(realm).append('"')
                .append(", qop=\"auth\"")
                .append(", algorithm=").append(DigestAlgorithm.SHA_256.token())
                .append(", nonce=\"").append(nonceManager.issue()).append('"');
        if (stale) {
            challenge.append(", stale=true");
        }
        return challenge.toString();
    }

    /**
     * Verifies the value of an {@code Authorization} request header (RFC 7616 §3.4)
     * against the given credential store.
     *
     * @param authorizationHeader the raw header value, as sent by the client.
     *                            Anything that does not present {@code Digest} credentials,
     *                            because it uses another scheme or is the {@code Digest} scheme alone,
     *                            is reported as {@link DigestVerification.NotDigest},
     *                            while anything that does is either accepted or rejected
     * @param method              the HTTP method of the request being authenticated
     * @param requestUri          the request-target of the request being authenticated,
     *                            which must match the header's {@code uri} parameter exactly
     */
    public DigestVerification verify(
            String authorizationHeader, String method, String requestUri, DigestCredentialLookup credentials) {
        AuthParamResult parsed = AuthParamParser.parseAs(authorizationHeader, SCHEME);
        if (parsed instanceof AuthParamResult.NoMatch) {
            return new DigestVerification.NotDigest();
        }
        if (!(parsed instanceof AuthParamResult.Match(Map<String, String> rawParams))) {
            return new DigestVerification.Unauthenticated(false);
        }
        if (rawParams.isEmpty()) {
            // The scheme alone presents no credentials, so there is nothing to authenticate or to reject.
            return new DigestVerification.NotDigest();
        }

        Optional<DigestParams> maybeParams = DigestParams.from(rawParams);
        if (maybeParams.isEmpty()) {
            return new DigestVerification.Unauthenticated(false);
        }
        DigestParams params = maybeParams.get();

        if (!realm.equals(params.realm()) || !requestUri.equals(params.uri())) {
            return new DigestVerification.Unauthenticated(false);
        }

        Optional<NonceManager.VerifiedNonce> nonce = nonceManager.verify(params.nonce());
        if (nonce.isEmpty()) {
            return new DigestVerification.Unauthenticated(false);
        }

        // The response is computed even for unknown users (against a dummy password),
        // so that response times do not reveal which usernames exist.
        Optional<String> password = credentials.passwordFor(params.username());
        String expectedResponse = DigestResponse.compute(
                params.algorithm(), params.username(), realm, password.orElse(UNKNOWN_USER_PASSWORD),
                method, params.uri(), params.nonce(), params.nc(), params.cnonce());
        boolean responseMatches = DigestCrypto.constantTimeEquals(expectedResponse, params.response());
        if (password.isEmpty() || !responseMatches) {
            return new DigestVerification.Unauthenticated(false);
        }

        // Only now that the client has proven its credentials may the nonce be used up or reported as stale.
        return switch (nonce.get().consume(params.ncValue())) {
            case ACCEPTED -> new DigestVerification.Authenticated(params.username());
            case STALE -> new DigestVerification.Unauthenticated(true);
            case REPLAYED -> new DigestVerification.Unauthenticated(false);
        };
    }
}
