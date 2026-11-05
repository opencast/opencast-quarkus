package org.opencastproject.digest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/**
 * Plays the client side of a Digest exchange (with SHA-256) against the running test application,
 * for the integration tests.
 */
final class DigestTestClient {
    static final String REALM = "Test Realm";

    private DigestTestClient() {
    }

    /**
     * Fetches a fresh challenge from {@code path} and builds the {@code Authorization} header answering it.
     */
    static String digestFor(String path, String username, String password, String method, String nc, String cnonce) {
        return digestFor(path, path, username, password, method, nc, cnonce);
    }

    /**
     * Like {@link #digestFor(String, String, String, String, String, String)},
     * but fetches the challenge from {@code challengePath}, for answering it on a {@code path} that issues none.
     */
    static String digestFor(
            String challengePath, String path, String username, String password, String method, String nc, String cnonce) {
        String nonce = freshNonce(challengePath);
        String ha1 = sha256(username + ":" + REALM + ":" + password);
        String ha2 = sha256(method + ":" + path);
        String response = sha256(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":auth:" + ha2);

        return "Digest username=\"" + username + "\", realm=\"" + REALM + "\", nonce=\"" + nonce + "\", "
                + "uri=\"" + path + "\", qop=auth, nc=" + nc + ", cnonce=\"" + cnonce + "\", "
                + "response=\"" + response + "\", algorithm=SHA-256";
    }

    private static String freshNonce(String path) {
        String challenge = given().when().get(path).then().statusCode(401).extract().header("WWW-Authenticate");
        Matcher matcher = Pattern.compile("nonce=\"([^\"]+)\"").matcher(challenge);
        if (!matcher.find()) {
            throw new IllegalStateException("No nonce in challenge: " + challenge);
        }
        return matcher.group(1);
    }

    private static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
