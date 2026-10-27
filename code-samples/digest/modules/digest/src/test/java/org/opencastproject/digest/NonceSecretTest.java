package org.opencastproject.digest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NonceSecretTest {
    private static final String HEX = "5f2c8e1b7a9d4c3e0b6a1f8d2e7c4b9a3d5e0f1c8b7a6d9e2f4c1b3a5d7e9f08";

    /**
     * Test cases 6 and 7 of RFC 4231, whose key is 131 bytes of {@code 0xaa}.
     */
    @Test
    void signsWithHmacSha256() {
        NonceSecret secret = NonceSecret.valueOf("aa".repeat(131));

        assertEquals("60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
                secret.sign("Test Using Larger Than Block-Size Key - Hash Key First"));
        assertEquals("9b09ffa71b942fcb27635fbcd5b0e944bfdc63644f0713938a7f51535c3a35e2",
                secret.sign("This is a test using a larger than block-size key and a larger than block-size data. "
                        + "The key needs to be hashed before being used by the HMAC algorithm."));
    }

    @Test
    void hexIsReadInLowerAndUpperCase() {
        assertEquals(NonceSecret.valueOf(HEX).sign("data"), NonceSecret.valueOf(HEX.toUpperCase()).sign("data"));
    }

    @Test
    void differentKeysSignDifferently() {
        assertNotEquals(NonceSecret.valueOf("ab".repeat(32)).sign("data"), NonceSecret.valueOf("cd".repeat(32)).sign("data"));
    }

    @Test
    void acceptsMoreThanTheMinimum() {
        assertEquals(64, NonceSecret.valueOf(HEX + "00").sign("data").length());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "5f2c8e1b",
            "5f2c8e1b7a9d4c3e0b6a1f8d2e7c4b9a",
            "correct horse battery staple, correct horse battery staple",
            HEX + "0",
            HEX + "zz",
            " " + HEX,
    })
    void rejectsAnythingElseWithoutRevealingIt(String value) {
        var e = assertThrows(IllegalArgumentException.class, () -> NonceSecret.valueOf(value));
        assertFalse(!value.isBlank() && e.getMessage().contains(value.strip()));
    }

    @Test
    void sayWhatIsWrong() {
        assertEquals("must be hex, e.g. generated with `openssl rand -hex 32`",
                assertThrows(IllegalArgumentException.class, () -> NonceSecret.valueOf("not hex")).getMessage());
        assertEquals("must be at least 32 bytes, which is 64 hex digits",
                assertThrows(IllegalArgumentException.class, () -> NonceSecret.valueOf("5f2c")).getMessage());
    }

    @Test
    void toStringDoesNotShowTheKey() {
        assertEquals("NonceSecret[****]", NonceSecret.valueOf(HEX).toString());
    }
}
