package org.opencastproject.digest;

import java.util.HexFormat;

/**
 * The key that nonces are signed with: at least {@value #MIN_BYTES} random bytes.
 * <p>
 * It is configured as hex text, which {@link #valueOf(String)} turns into the key, so that the key is real random
 * bytes and cannot be a guessable phrase. It is easily made with {@code openssl rand -hex 32}.
 * The rules for a usable key live here and nowhere else, so every {@code NonceSecret} is a valid one.
 * It never shows its value in {@link #toString()}.
 */
public final class NonceSecret {
    static final int MIN_BYTES = 32;
    private static final String MAC_ALGORITHM = "HmacSHA256";

    private final byte[] bytes;

    private NonceSecret(byte[] bytes) {
        if (bytes.length < MIN_BYTES) {
            throw new IllegalArgumentException(
                    "must be at least " + MIN_BYTES + " bytes, which is " + 2 * MIN_BYTES + " hex digits");
        }
        this.bytes = bytes;
    }

    /**
     * Also what makes SmallRye Config convert a configuration value into a {@code NonceSecret} on its own.
     * The messages do not contain the rejected value, as it may be a real key in the wrong format.
     *
     * @param hex at least {@value #MIN_BYTES} bytes as hex digits, in lower or upper case
     * @throws IllegalArgumentException if it is not hex, or too short
     */
    public static NonceSecret valueOf(String hex) {
        byte[] decoded;
        try {
            decoded = HexFormat.of().parseHex(hex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("must be hex, e.g. generated with `openssl rand -hex 32`");
        }
        return new NonceSecret(decoded);
    }

    /**
     * The key never leaves this class: it is only used to sign.
     *
     * @return the HMAC-SHA256 of the text, as lowercase hex
     */
    String sign(String data) {
        return DigestCrypto.hmac(MAC_ALGORITHM, bytes, data);
    }

    @Override
    public String toString() {
        return "NonceSecret[****]";
    }
}
