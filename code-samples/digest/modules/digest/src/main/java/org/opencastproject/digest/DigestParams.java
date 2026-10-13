package org.opencastproject.digest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The subset of the RFC 7616 §3.4.1 {@code Authorization} request-header
 * parameters this implementation understands and requires.
 * Only {@code qop=auth} is supported: neither the legacy RFC 2069 form
 * (no {@code qop} at all) nor {@code auth-int} are implemented.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7616#section-3.4.1">RFC 7616 §3.4.1 — The Authorization Request Header</a>
 */
record DigestParams(
        String username,
        String realm,
        String nonce,
        String uri,
        DigestAlgorithm algorithm,
        String cnonce,
        String nc,
        String response) {

    private static final Pattern NC_PATTERN = Pattern.compile("[0-9a-fA-F]{8}");

    /**
     * Records cannot hide their canonical constructor, so this is the narrowest access possible.
     * {@link #from(Map)} is the intended entry point.
     * The checks here are assertions of what {@code from} already guarantees.
     */
    DigestParams {
        Objects.requireNonNull(username);
        Objects.requireNonNull(realm);
        Objects.requireNonNull(nonce);
        Objects.requireNonNull(uri);
        Objects.requireNonNull(algorithm);
        Objects.requireNonNull(cnonce);
        Objects.requireNonNull(nc);
        Objects.requireNonNull(response);
        if (!NC_PATTERN.matcher(nc).matches()) {
            throw new IllegalArgumentException("nc must be 8 hex digits");
        }
    }

    /**
     * The numeric value of {@link #nc()}, as used for replay checking.
     */
    long ncValue() {
        return Long.parseLong(nc, 16);
    }

    /**
     * @param p the parameters of credentials that have already been identified as using the {@code Digest} scheme
     */
    static Optional<DigestParams> from(Map<String, String> p) {
        String username = p.get("username");
        String realm = p.get("realm");
        String nonce = p.get("nonce");
        String uri = p.get("uri");
        String response = p.get("response");
        String cnonce = p.get("cnonce");
        String nc = p.get("nc");
        if (username == null || realm == null || nonce == null || uri == null
                || response == null || cnonce == null || nc == null) {
            return Optional.empty();
        }

        if (!"auth".equals(p.get("qop"))) {
            return Optional.empty();
        }

        String algorithmToken = p.get("algorithm");
        DigestAlgorithm algorithm;
        if (algorithmToken == null) {
            algorithm = DigestAlgorithm.MD5;
        } else {
            Optional<DigestAlgorithm> maybeAlgorithm = DigestAlgorithm.fromToken(algorithmToken);
            if (maybeAlgorithm.isEmpty()) {
                return Optional.empty();
            }
            algorithm = maybeAlgorithm.get();
        }

        if (!NC_PATTERN.matcher(nc).matches()) {
            return Optional.empty();
        }

        return Optional.of(new DigestParams(username, realm, nonce, uri, algorithm, cnonce, nc, response));
    }
}
