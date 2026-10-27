package org.opencastproject.digest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Verifies {@link DigestResponse} against the worked example from
 * RFC 7616 §3.9.1, independently reproduced with Python's {@code hashlib}.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7616#section-3.9.1">RFC 7616 §3.9.1 — Example with SHA-256 and MD5</a>
 */
class DigestResponseTest {
    private static final String USERNAME = "Mufasa";
    private static final String PASSWORD = "Circle of Life";
    private static final String REALM = "http-auth@example.org";
    private static final String NONCE = "7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v";
    private static final String NC = "00000001";
    private static final String CNONCE = "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ";
    private static final String URI = "/dir/index.html";
    private static final String METHOD = "GET";

    @Test
    void sha256() {
        assertEquals(
                "753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1",
                DigestResponse.compute(DigestAlgorithm.SHA_256, USERNAME, REALM, PASSWORD, METHOD, URI, NONCE, NC, CNONCE));
    }

    @Test
    void md5() {
        assertEquals(
                "8ca523f5e9506fed4657c9700eebdbec",
                DigestResponse.compute(DigestAlgorithm.MD5, USERNAME, REALM, PASSWORD, METHOD, URI, NONCE, NC, CNONCE));
    }

    @Test
    void differentAlgorithmsProduceDifferentResponsesOfTheirOwnLength() {
        String sha256 = DigestResponse.compute(DigestAlgorithm.SHA_256, USERNAME, REALM, PASSWORD, METHOD, URI, NONCE, NC, CNONCE);
        String md5 = DigestResponse.compute(DigestAlgorithm.MD5, USERNAME, REALM, PASSWORD, METHOD, URI, NONCE, NC, CNONCE);
        assertNotEquals(sha256, md5);
        assertEquals(64, sha256.length());
        assertEquals(32, md5.length());
    }
}
