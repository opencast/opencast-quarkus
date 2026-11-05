package org.opencastproject.digest;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NonceManagerTest {
    private static final NonceSecret SECRET = NonceSecret.valueOf("ab".repeat(32));
    private static final Duration VALIDITY = Duration.ofMinutes(1);

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /**
     * Base64 accepts leftover bits that are not zero, so a nonce has several spellings that decode to the same bytes.
     */
    private static String aliasOf(String nonce) {
        String prefix = nonce.substring(0, nonce.length() - 1);
        byte[] decoded = Base64.getUrlDecoder().decode(nonce);
        for (char c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()) {
            String candidate = prefix + c;
            if (!candidate.equals(nonce) && Arrays.equals(decoded, Base64.getUrlDecoder().decode(candidate))) {
                return candidate;
            }
        }
        throw new AssertionError("no other spelling of " + nonce);
    }

    private static NonceManager.Outcome use(NonceManager manager, String nonce, long nc) {
        return manager.verify(nonce).orElseThrow().consume(nc);
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void issuedNonceIsValid() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, manager.issue(), 1));
    }

    @Test
    void increasingNcStaysValid() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        String nonce = manager.issue();
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, nonce, 1));
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, nonce, 2));
    }

    @Test
    void repeatedNcIsReplayed() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        String nonce = manager.issue();
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, nonce, 2));
        assertEquals(NonceManager.Outcome.REPLAYED, use(manager, nonce, 2));
        assertEquals(NonceManager.Outcome.REPLAYED, use(manager, nonce, 1));
    }

    @Test
    void expiredNonceIsRejected() {
        Instant issuedAt = Instant.EPOCH;
        NonceManager issuer = new NonceManager(SECRET, VALIDITY, clockAt(issuedAt));
        String nonce = issuer.issue();

        NonceManager laterValidator = new NonceManager(SECRET, VALIDITY, clockAt(issuedAt.plus(VALIDITY).plusMillis(1)));
        assertEquals(NonceManager.Outcome.STALE, use(laterValidator, nonce, 1));
    }

    @Test
    void nonBase64NonceIsMalformed() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        assertTrue(manager.verify("not valid base64!!").isEmpty());
    }

    @Test
    void nonceWithoutColonIsMalformed() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        assertTrue(manager.verify(encode("no-colon-here")).isEmpty());
    }

    @Test
    void nonceWithNonNumericTimestampIsMalformed() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        assertTrue(manager.verify(encode("not-a-number:1:deadbeef")).isEmpty());
    }

    @Test
    void tamperedMacIsMalformed() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        assertTrue(manager.verify(encode("0:1:deadbeef")).isEmpty());
    }

    @Test
    void evictedNonceCannotBeReplayed() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH), 1);
        String first = manager.issue();
        String second = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH.plusMillis(1))).issue();

        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, first, 5));
        // Tracking a second nonce evicts the first one's `nc`.
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, second, 1));
        // Its history is gone, so it must not be mistaken for an unused nonce.
        assertEquals(NonceManager.Outcome.STALE, use(manager, first, 6));
        assertEquals(NonceManager.Outcome.STALE, use(manager, first, 1));
        // Nonces issued after the evicted one are unaffected.
        assertEquals(NonceManager.Outcome.REPLAYED, use(manager, second, 1));
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, second, 2));
    }

    @Test
    void unusedNonceIssuedAfterEvictedOneStaysValid() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH), 1);
        String first = manager.issue();
        String second = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH.plusMillis(1))).issue();
        String third = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH.plusMillis(2))).issue();

        use(manager, first, 1);
        use(manager, second, 1);
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, third, 1));
    }

    @Test
    void nonceIssuedBeforeRestartCannotBeReplayed() {
        NonceManager beforeRestart = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        String nonce = beforeRestart.issue();
        assertEquals(NonceManager.Outcome.ACCEPTED, use(beforeRestart, nonce, 1));

        // Same (persistent) secret, empty replay map, still within the nonce's validity.
        NonceManager afterRestart = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH.plusSeconds(10)));
        assertEquals(NonceManager.Outcome.STALE, use(afterRestart, nonce, 1));
        assertEquals(NonceManager.Outcome.ACCEPTED, use(afterRestart, afterRestart.issue(), 1));
    }

    @Test
    void verifyDoesNotRecordAnything() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        String nonce = manager.issue();
        assertTrue(manager.verify(nonce).isPresent());
        assertTrue(manager.verify(nonce).isPresent());
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, nonce, 5));
        assertEquals(NonceManager.Outcome.REPLAYED, use(manager, nonce, 5));
    }

    @Test
    void verifyCannotEvictTrackedNonces() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH), 1);
        String tracked = manager.issue();
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, tracked, 1));

        for (int i = 0; i < 10; i++) {
            assertTrue(manager.verify(manager.issue()).isPresent());
        }

        assertEquals(NonceManager.Outcome.REPLAYED, use(manager, tracked, 1));
    }

    @Test
    void verifyDoesNotJudgeFreshness() {
        NonceManager issuer = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        String nonce = issuer.issue();
        NonceManager later = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH.plus(VALIDITY).plusMillis(1)));
        assertTrue(later.verify(nonce).isPresent());
        assertEquals(NonceManager.Outcome.STALE, use(later, nonce, 1));
    }

    @Test
    void nonceFromAnotherSecretIsNotVerified() {
        NonceSecret other = NonceSecret.valueOf("cd".repeat(32));
        NonceManager issuer = new NonceManager(other, VALIDITY, clockAt(Instant.EPOCH));
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        assertTrue(manager.verify(issuer.issue()).isEmpty());
    }

    @Test
    void anotherSpellingOfANonceSharesItsNcHistory() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        String nonce = manager.issue();
        String alias = aliasOf(nonce);

        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, nonce, 1));
        assertEquals(NonceManager.Outcome.REPLAYED, use(manager, alias, 1));
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, alias, 2));
        assertEquals(NonceManager.Outcome.REPLAYED, use(manager, nonce, 2));
        assertEquals(1, manager.trackedNonceCount());
    }

    @Test
    void trackedNonceThatExpiredIsStaleNotReplayed() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clock);
        String nonce = manager.issue();
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, nonce, 5));

        clock.advance(VALIDITY.plusMillis(1));

        assertEquals(NonceManager.Outcome.STALE, use(manager, nonce, 1));
    }

    @Test
    void expiredNonceIsNoLongerTracked() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clock);
        String nonce = manager.issue();
        use(manager, nonce, 1);
        assertEquals(1, manager.trackedNonceCount());

        clock.advance(VALIDITY.plusMillis(1));
        use(manager, nonce, 2);

        assertEquals(0, manager.trackedNonceCount());
    }

    @Test
    void nonceWithTooManyPartsIsMalformed() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        assertTrue(manager.verify(encode("0:1:deadbeef:extra")).isEmpty());
    }

    @Test
    void noncesIssuedInTheSameMillisecondDiffer() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        String first = manager.issue();
        String second = manager.issue();

        assertNotEquals(first, second);
        // Independent counters, so using one must not affect the other.
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, first, 1));
        assertEquals(NonceManager.Outcome.ACCEPTED, use(manager, second, 1));
    }

    @Test
    void tamperedSequenceIsMalformed() {
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        String[] parts = new String(Base64.getUrlDecoder().decode(manager.issue()), StandardCharsets.UTF_8).split(":");
        String tampered = encode(parts[0] + ":" + (Long.parseLong(parts[1]) + 1) + ":" + parts[2]);

        assertTrue(manager.verify(tampered).isEmpty());
    }

    @Test
    void concurrentConsumesOfTheSameNonceAndNcSucceedExactlyOnce() throws Exception {
        int threads = 16;
        int rounds = 200;
        NonceManager manager = new NonceManager(SECRET, VALIDITY, clockAt(Instant.EPOCH));
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int round = 0; round < rounds; round++) {
                String nonce = manager.issue();
                CyclicBarrier barrier = new CyclicBarrier(threads);
                List<Future<NonceManager.Outcome>> results = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    results.add(executor.submit(() -> {
                        barrier.await();
                        return use(manager, nonce, 1);
                    }));
                }
                long valid = 0;
                for (Future<NonceManager.Outcome> result : results) {
                    if (result.get() == NonceManager.Outcome.ACCEPTED) {
                        valid++;
                    }
                }
                assertEquals(1, valid, "round " + round);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
