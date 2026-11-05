package org.opencastproject.digest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigestParamsTest {
    private static Map<String, String> parse(String header) {
        return assertInstanceOf(AuthParamResult.Match.class, AuthParamParser.parseAs(header, "Digest")).params();
    }

    private static final String VALID =
            "Digest username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", uri=\"/\", "
                    + "response=\"resp\", qop=auth, nc=00000001, cnonce=\"cn\"";

    @Test
    void parsesValidHeader() throws Exception {
        DigestParams params = DigestParams.from(parse(VALID)).orElseThrow();
        assertEquals("Mufasa", params.username());
        assertEquals("example.org", params.realm());
        assertEquals("abc", params.nonce());
        assertEquals("/", params.uri());
        assertEquals(DigestAlgorithm.MD5, params.algorithm());
        assertEquals("cn", params.cnonce());
        assertEquals("00000001", params.nc());
        assertEquals(1L, params.ncValue());
        assertEquals("resp", params.response());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", uri=\"/\", response=\"resp\", qop=auth, nc=00000001",
            "realm=\"example.org\", nonce=\"abc\", uri=\"/\", response=\"resp\", qop=auth, nc=00000001, cnonce=\"cn\"",
            "username=\"Mufasa\", nonce=\"abc\", uri=\"/\", response=\"resp\", qop=auth, nc=00000001, cnonce=\"cn\"",
            "username=\"Mufasa\", realm=\"example.org\", uri=\"/\", response=\"resp\", qop=auth, nc=00000001, cnonce=\"cn\"",
            "username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", response=\"resp\", qop=auth, nc=00000001, cnonce=\"cn\"",
            "username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", uri=\"/\", qop=auth, nc=00000001, cnonce=\"cn\"",
            "username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", uri=\"/\", response=\"resp\", qop=auth, cnonce=\"cn\"",
    })
    void missingRequiredFieldIsRejected(String params) throws Exception {
        assertTrue(DigestParams.from(parse("Digest " + params)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", uri=\"/\", response=\"resp\", nc=00000001, cnonce=\"cn\"",
            "username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", uri=\"/\", response=\"resp\", qop=auth-int, nc=00000001, cnonce=\"cn\"",
    })
    void nonAuthQopIsRejected(String params) throws Exception {
        assertTrue(DigestParams.from(parse("Digest " + params)).isEmpty());
    }

    @Test
    void explicitKnownAlgorithmIsAccepted() throws Exception {
        DigestParams params = DigestParams.from(parse(VALID + ", algorithm=SHA-256")).orElseThrow();
        assertEquals(DigestAlgorithm.SHA_256, params.algorithm());
    }

    @Test
    void unknownAlgorithmIsRejected() throws Exception {
        assertTrue(DigestParams.from(parse(VALID + ", algorithm=SHA-512-256")).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "000001", "0000000001"})
    void ncOfWrongLengthIsRejected(String nc) throws Exception {
        String header = "Digest username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", uri=\"/\", "
                + "response=\"resp\", qop=auth, nc=" + nc + ", cnonce=\"cn\"";
        assertTrue(DigestParams.from(parse(header)).isEmpty());
    }

    @Test
    void nonHexNcIsRejected() throws Exception {
        String header = "Digest username=\"Mufasa\", realm=\"example.org\", nonce=\"abc\", uri=\"/\", "
                + "response=\"resp\", qop=auth, nc=gggggggg, cnonce=\"cn\"";
        assertTrue(DigestParams.from(parse(header)).isEmpty());
    }

    @Test
    void constructorRejectsMalformedNc() {
        assertThrows(IllegalArgumentException.class, () ->
                new DigestParams("u", "r", "n", "/", DigestAlgorithm.MD5, "c", "+0000001", "resp"));
    }

    @Test
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class, () ->
                new DigestParams(null, "r", "n", "/", DigestAlgorithm.MD5, "c", "00000001", "resp"));
    }
}
