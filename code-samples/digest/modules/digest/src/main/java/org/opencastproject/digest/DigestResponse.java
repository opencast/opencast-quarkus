package org.opencastproject.digest;

/**
 * Computes the {@code response} value of RFC 7616 §3.4.3 for {@code qop=auth}
 * (the {@code auth-int} variant, which also hashes the request's entity body,
 * is not implemented).
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7616#section-3.4.3">RFC 7616 §3.4.3 — Response</a>
 */
final class DigestResponse {
    private DigestResponse() {
    }

    static String compute(
            DigestAlgorithm algorithm,
            String username, String realm, String password,
            String method, String uri,
            String nonce, String nc, String cnonce) {
        String ha1 = algorithm.hash(username + ":" + realm + ":" + password);
        String ha2 = algorithm.hash(method + ":" + uri);
        return algorithm.hash(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" + ha2);
    }
}
