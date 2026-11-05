package org.opencastproject.digest;

import java.util.Arrays;
import java.util.Optional;

/**
 * The hash algorithms usable with HTTP Digest Authentication (RFC 7616 §3.4.2).
 * <p>
 * Only the base algorithms are implemented, not their {@code -sess} variants:
 * {@link NonceManager} already binds every nonce to a short validity window
 * and rejects a non-increasing {@code nc}, which covers the extra replay
 * protection the {@code -sess} variants exist to provide.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7616#section-3.4.2">RFC 7616 §3.4.2 — A1</a>
 */
enum DigestAlgorithm {
    MD5("MD5"),
    SHA_256("SHA-256");

    private final String token;

    DigestAlgorithm(String token) {
        this.token = token;
    }

    String token() {
        return token;
    }

    static Optional<DigestAlgorithm> fromToken(String token) {
        return Arrays.stream(values())
                .filter(algorithm -> algorithm.token.equalsIgnoreCase(token))
                .findFirst();
    }

    String hash(String data) {
        return DigestCrypto.hash(token, data);
    }
}
