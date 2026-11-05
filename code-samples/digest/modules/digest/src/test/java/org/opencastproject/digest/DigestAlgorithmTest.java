package org.opencastproject.digest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigestAlgorithmTest {

    @ParameterizedTest
    @ValueSource(strings = {"MD5", "md5", "Md5"})
    void fromTokenFindsMd5(String token) {
        assertEquals(DigestAlgorithm.MD5, DigestAlgorithm.fromToken(token).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SHA-256", "sha-256"})
    void fromTokenFindsSha256(String token) {
        assertEquals(DigestAlgorithm.SHA_256, DigestAlgorithm.fromToken(token).orElseThrow());
    }

    @Test
    void fromTokenRejectsUnknown() {
        assertTrue(DigestAlgorithm.fromToken("SHA-512-256").isEmpty());
    }

    @Test
    void hashDelegatesToDigestCrypto() {
        assertEquals(DigestCrypto.hash("SHA-256", "hello"), DigestAlgorithm.SHA_256.hash("hello"));
    }
}
