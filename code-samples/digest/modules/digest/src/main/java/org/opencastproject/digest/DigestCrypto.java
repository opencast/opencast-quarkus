package org.opencastproject.digest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Small wrappers around {@link MessageDigest} and {@link Mac} that turn their
 * checked exceptions into unchecked ones.
 * <p>
 * The algorithm names this package actually uses ({@code MD5}, {@code SHA-256}
 * and {@code HmacSHA256}) are guaranteed to be available on every conforming
 * JVM, so callers only ever see the wrapped {@link IllegalStateException} if
 * this class itself is misused with an unsupported name.
 */
final class DigestCrypto {
    private DigestCrypto() {
    }

    static String hash(String algorithm, String data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JVM does not provide the " + algorithm + " algorithm", e);
        }
        return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    static String hmac(String algorithm, byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JVM does not provide the " + algorithm + " algorithm", e);
        }
    }

    static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
