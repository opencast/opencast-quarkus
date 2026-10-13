package org.opencastproject.digest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigestAuthenticatorTest {
    private static final String REALM = "example.org";
    private static final String USERNAME = "Mufasa";
    private static final String PASSWORD = "Circle of Life";
    private static final String METHOD = "GET";
    private static final String URI = "/dir/index.html";
    private static final NonceSecret SECRET = NonceSecret.valueOf("ab".repeat(32));
    private static final String CNONCE = "client-nonce";

    private static final DigestCredentialLookup CREDENTIALS = username ->
            USERNAME.equals(username) ? Optional.of(PASSWORD) : Optional.empty();

    private static DigestAuthenticator authenticator(Clock clock) {
        return authenticator(SECRET, clock);
    }

    private static DigestAuthenticator authenticator(NonceSecret nonceSecret, Clock clock) {
        NonceManager nonceManager = new NonceManager(nonceSecret, Duration.ofMinutes(1), clock);
        return new DigestAuthenticator(REALM, nonceManager);
    }

    private static String nonceFromChallenge(String challenge) {
        Matcher matcher = Pattern.compile("nonce=\"([^\"]+)\"").matcher(challenge);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private static String authorizationHeader(DigestAlgorithm algorithm, String username, String password, String nonce, String nc) {
        String response = DigestResponse.compute(algorithm, username, REALM, password, METHOD, URI, nonce, nc, CNONCE);
        return "Digest username=\"" + username + "\", realm=\"" + REALM + "\", nonce=\"" + nonce + "\", "
                + "uri=\"" + URI + "\", qop=auth, nc=" + nc + ", cnonce=\"" + CNONCE + "\", "
                + "response=\"" + response + "\", algorithm=" + algorithm.token();
    }

    @Test
    void publicConstructorWorks() {
        DigestAuthenticator authenticator = new DigestAuthenticator(REALM, SECRET, Duration.ofMinutes(1));

        assertTrue(authenticator.issueChallenge(false).contains("realm=\"" + REALM + "\""));
    }

    @Test
    void challengeAdvertisesSha256AndOmitsStaleByDefault() {
        String challenge = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)).issueChallenge(false);
        assertTrue(challenge.startsWith("Digest realm=\"" + REALM + "\""));
        assertTrue(challenge.contains("algorithm=" + DigestAlgorithm.SHA_256.token()));
        assertTrue(challenge.contains("qop=\"auth\""));
        assertFalse(challenge.contains("stale=true"));
    }

    @Test
    void staleChallengeSaysSo() {
        String challenge = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)).issueChallenge(true);
        assertTrue(challenge.contains("stale=true"));
    }

    @ParameterizedTest
    @EnumSource(DigestAlgorithm.class)
    void correctResponseIsAuthenticated(DigestAlgorithm algorithm) {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(auth.issueChallenge(false));
        String header = authorizationHeader(algorithm, USERNAME, PASSWORD, nonce, "00000001");

        DigestVerification result = auth.verify(header, METHOD, URI, CREDENTIALS);

        assertEquals(new DigestVerification.Authenticated(USERNAME), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " Digest x=1",
            "=",
            "Basic realm=\"example.org\"",
            "Basic dXNlcjpwYXNz",
            "Bearer eyJhbGciOi.eyJzdWIi.sig",
            "DigestFoo username=\"Mufasa\"",
            "Digest-ish x=1",
            "Digest",
            "Digest ",
            "Digest ,,",
    })
    void headersWithoutDigestCredentialsAreNotDigest(String header) {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        assertEquals(new DigestVerification.NotDigest(), auth.verify(header, METHOD, URI, CREDENTIALS));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest garbage",
            "Digest\tgarbage",
            "Digest username=\"unterminated",
            "DIGEST username=\"Mufasa\"",
    })
    void malformedCredentialsAreUnauthenticated(String header) {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(header, METHOD, URI, CREDENTIALS));
    }

    @Test
    void mismatchedRealmIsUnauthenticated() {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(auth.issueChallenge(false));
        String response = DigestResponse.compute(DigestAlgorithm.SHA_256, USERNAME, "other-realm", PASSWORD, METHOD, URI, nonce, "00000001", CNONCE);
        String header = "Digest username=\"" + USERNAME + "\", realm=\"other-realm\", nonce=\"" + nonce + "\", "
                + "uri=\"" + URI + "\", qop=auth, nc=00000001, cnonce=\"" + CNONCE + "\", response=\"" + response + "\"";

        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(header, METHOD, URI, CREDENTIALS));
    }

    @Test
    void mismatchedUriIsUnauthenticated() {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(auth.issueChallenge(false));
        String header = authorizationHeader(DigestAlgorithm.SHA_256, USERNAME, PASSWORD, nonce, "00000001");

        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(header, METHOD, "/other", CREDENTIALS));
    }

    @Test
    void malformedNonceIsUnauthenticated() {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String header = authorizationHeader(DigestAlgorithm.SHA_256, USERNAME, PASSWORD, "not-a-real-nonce", "00000001");

        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(header, METHOD, URI, CREDENTIALS));
    }

    @Test
    void expiredNonceIsReportedAsStale() {
        NonceSecret secret = SECRET;
        Instant issuedAt = Instant.EPOCH;

        DigestAuthenticator issuer = authenticator(secret, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(issuer.issueChallenge(false));
        String header = authorizationHeader(DigestAlgorithm.SHA_256, USERNAME, PASSWORD, nonce, "00000001");

        // A later verifier sharing the same nonce secret sees the very same nonce as authentic, just expired.
        DigestAuthenticator laterValidator = authenticator(secret, Clock.fixed(issuedAt.plus(Duration.ofMinutes(5)), ZoneOffset.UTC));

        DigestVerification result = laterValidator.verify(header, METHOD, URI, CREDENTIALS);
        assertInstanceOf(DigestVerification.Unauthenticated.class, result);
        assertTrue(((DigestVerification.Unauthenticated) result).staleNonce());
    }

    @Test
    void unknownUserIsUnauthenticated() {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(auth.issueChallenge(false));
        String header = authorizationHeader(DigestAlgorithm.SHA_256, "someone-else", PASSWORD, nonce, "00000001");

        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(header, METHOD, URI, CREDENTIALS));
    }

    @Test
    void wrongPasswordIsUnauthenticated() {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(auth.issueChallenge(false));
        String header = authorizationHeader(DigestAlgorithm.SHA_256, USERNAME, "wrong password", nonce, "00000001");

        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(header, METHOD, URI, CREDENTIALS));
    }

    @Test
    void replayedRequestIsUnauthenticated() {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(auth.issueChallenge(false));
        String header = authorizationHeader(DigestAlgorithm.SHA_256, USERNAME, PASSWORD, nonce, "00000001");

        assertEquals(new DigestVerification.Authenticated(USERNAME), auth.verify(header, METHOD, URI, CREDENTIALS));
        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(header, METHOD, URI, CREDENTIALS));
    }

    @Test
    void failedAttemptDoesNotUseUpTheNc() {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(auth.issueChallenge(false));
        String bad = authorizationHeader(DigestAlgorithm.SHA_256, USERNAME, "wrong password", nonce, "00000009");
        String good = authorizationHeader(DigestAlgorithm.SHA_256, USERNAME, PASSWORD, nonce, "00000001");

        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(bad, METHOD, URI, CREDENTIALS));
        assertEquals(new DigestVerification.Authenticated(USERNAME), auth.verify(good, METHOD, URI, CREDENTIALS));
    }

    @Test
    void expiredNonceWithWrongPasswordIsNotReportedAsStale() {
        NonceSecret secret = SECRET;
        DigestAuthenticator issuer = authenticator(secret, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(issuer.issueChallenge(false));
        String header = authorizationHeader(DigestAlgorithm.SHA_256, USERNAME, "wrong password", nonce, "00000001");
        DigestAuthenticator later = authenticator(secret, Clock.fixed(Instant.EPOCH.plus(Duration.ofMinutes(5)), ZoneOffset.UTC));

        assertEquals(new DigestVerification.Unauthenticated(false), later.verify(header, METHOD, URI, CREDENTIALS));
    }

    @Test
    void unknownUserCannotAuthenticateWithTheDummyPassword() {
        DigestAuthenticator auth = authenticator(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        String nonce = nonceFromChallenge(auth.issueChallenge(false));
        String header = authorizationHeader(DigestAlgorithm.SHA_256, "someone-else", "unknown-user", nonce, "00000001");

        assertEquals(new DigestVerification.Unauthenticated(false), auth.verify(header, METHOD, URI, CREDENTIALS));
    }
}
