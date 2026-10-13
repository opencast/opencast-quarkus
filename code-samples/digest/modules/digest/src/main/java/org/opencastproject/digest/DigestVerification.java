package org.opencastproject.digest;

/**
 * The outcome of {@link DigestAuthenticator#verify}.
 */
public sealed interface DigestVerification {
    /**
     * The header does not present {@code Digest} credentials at all:
     * it uses another scheme, or the {@code Digest} scheme without any parameters.
     * Such a header is neither accepted nor rejected,
     * so that a caller can leave it to someone else.
     */
    record NotDigest() implements DigestVerification {
    }

    /**
     * The request carried a valid set of Digest credentials for {@code username}.
     */
    record Authenticated(String username) implements DigestVerification {
    }

    /**
     * The request did not carry valid Digest credentials.
     *
     * @param staleNonce whether the only problem was that an otherwise well-formed
     *                    and correctly answered nonce had simply expired, in which
     *                    case RFC 7616 §3.3 recommends re-challenging with a fresh
     *                    nonce and {@code stale=true} instead of asking the user to
     *                    re-enter their credentials.
     */
    record Unauthenticated(boolean staleNonce) implements DigestVerification {
    }
}
