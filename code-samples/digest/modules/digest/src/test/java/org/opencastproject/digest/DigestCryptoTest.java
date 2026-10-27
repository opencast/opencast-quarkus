package org.opencastproject.digest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigestCryptoTest {

    @Test
    void hashMatchesKnownAnswers() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", DigestCrypto.hash("SHA-256", "abc"));
        assertEquals("900150983cd24fb0d6963f7d28e17f72", DigestCrypto.hash("MD5", "abc"));
    }

    @Test
    void hashOfUnknownAlgorithmThrows() {
        assertThrows(IllegalStateException.class, () -> DigestCrypto.hash("not-a-real-algorithm", "hello"));
    }

    /**
     * Test case 2 of RFC 4231.
     */
    @Test
    void hmacMatchesKnownAnswer() {
        assertEquals(
                "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                DigestCrypto.hmac("HmacSHA256", "Jefe".getBytes(), "what do ya want for nothing?"));
    }

    @Test
    void hmacOfUnknownAlgorithmThrows() {
        byte[] key = "secret".getBytes();
        assertThrows(IllegalStateException.class, () -> DigestCrypto.hmac("not-a-real-algorithm", key, "hello"));
    }

    @Test
    void constantTimeEqualsMatches() {
        assertTrue(DigestCrypto.constantTimeEquals("abc", "abc"));
        assertFalse(DigestCrypto.constantTimeEquals("abc", "abd"));
        assertFalse(DigestCrypto.constantTimeEquals("abc", "ab"));
    }
}
