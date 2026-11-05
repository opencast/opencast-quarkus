package org.opencastproject.digest;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Issues and validates the {@code nonce} values used to prevent replay of
 * HTTP Digest Authentication (RFC 7616) credentials.
 * <p>
 * Nonces are self-contained (a timestamp plus an HMAC over that timestamp),
 * so no server-side state is needed to check their authenticity or freshness.
 * RFC 7616 §5.4 and §5.5 discuss rejecting replayed requests; here this is done by
 * rejecting a non-increasing {@code nc} for a given nonce.
 * A small, size-bounded map of "highest {@code nc} seen" per nonce is kept for that purpose.
 * That map is only written by {@link VerifiedNonce#consume}, which callers must invoke only
 * after the client has authenticated, so unauthenticated requests cannot evict
 * other clients' entries.
 * Everything before that, {@link #verify}, only involves the nonce and the secret and does not touch the map.
 * <p>
 * Whenever tracked state is lost (an entry is evicted, or this instance has just started),
 * a nonce that is no longer in the map could have been used already.
 * To stay safe, every nonce issued before the {@code untrustedBefore} bound
 * that is not in the map is reported as {@link Outcome#STALE},
 * which makes the client fetch a fresh one instead of risking a replay.
 * The bound starts at this instance's creation time (covering restarts with a persistent secret)
 * and moves forward past every evicted nonce's timestamp.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7616#section-3.3">RFC 7616 §3.3 — The WWW-Authenticate Response Header Field</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7616#section-5.4">RFC 7616 §5.4 — Limited-Use Nonce Values</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7616#section-5.5">RFC 7616 §5.5 — Replay Attacks</a>
 */
final class NonceManager {
    private static final int MAX_TRACKED_NONCES = 10_000;

    private final NonceSecret secret;
    private final Duration validity;
    private final Clock clock;
    private final Map<String, TrackedNonce> seenNonces;
    private long untrustedBefore;
    private final AtomicLong sequence = new AtomicLong();

    private record TrackedNonce(long nc, long timestamp) {
    }

    NonceManager(NonceSecret secret, Duration validity) {
        this(secret, validity, Clock.systemUTC());
    }

    NonceManager(NonceSecret secret, Duration validity, Clock clock) {
        this(secret, validity, clock, MAX_TRACKED_NONCES);
    }

    NonceManager(NonceSecret secret, Duration validity, Clock clock, int maxTrackedNonces) {
        this.secret = secret;
        this.validity = validity;
        this.clock = clock;
        this.untrustedBefore = clock.millis();
        // Deliberately insertion order (accessOrder = false), not LRU:
        // 1. Every eviction moves untrustedBefore up to the evicted nonce's timestamp,
        //    which makes all unused nonces issued before it untrusted.
        //    Insertion order is first-use order, so it approximates timestamp order and evicts
        //    the oldest timestamps first (often already expired ones), keeping that bound as low as possible.
        //    LRU could evict a recently issued but idle nonce, jumping the bound forward
        //    and forcing many honest clients to fetch a new nonce.
        // 2. Reads must not change the order: check() runs before the client is authenticated,
        //    and in access order mode even get() would let unauthenticated requests reorder evictions.
        this.seenNonces = new LinkedHashMap<>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TrackedNonce> eldest) {
                boolean evict = size() > maxTrackedNonces;
                if (evict) {
                    // Eviction follows first-use order, not issue order, so only ever move the bound forward.
                    untrustedBefore = Math.max(untrustedBefore, eldest.getValue().timestamp() + 1);
                }
                return evict;
            }
        };
    }

    /**
     * The nonce is {@code timestamp:sequence:mac}.
     * The sequence number makes every nonce issued by this instance unique,
     * even when several are issued within the same millisecond,
     * so no two clients ever share (and thereby disturb) one {@code nc} counter.
     */
    String issue() {
        String signed = clock.millis() + ":" + sequence.getAndIncrement();
        String raw = signed + ":" + secret.sign(signed);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A nonce that this instance's secret signed, so that it is genuine and not one made up by a client.
     * It can only be obtained from {@link #verify}, and it can only be used with the instance that verified it.
     * It says nothing about whether the nonce is still fresh or has been used,
     * which is up to {@link VerifiedNonce#consume}.
     */
    final class VerifiedNonce {
        /**
         * The part of the nonce that the signature covers.
         * Unlike the nonce as the client wrote it, which has several spellings (Base64 accepts
         * leftover bits that are not zero), there is exactly one string for each nonce that was issued.
         */
        private final String key;
        private final long timestamp;

        private VerifiedNonce(String key, long timestamp) {
            this.key = key;
            this.timestamp = timestamp;
        }

        /**
         * Checks the nonce's freshness and {@code nc} against what has been used,
         * and if they are fine, records {@code nc} as the highest one used,
         * so that it (and any lower one) cannot be used again.
         * The check and the recording are one step, so concurrent requests with the same nonce and {@code nc}
         * cannot both be accepted.
         * Must only be called once the client has proven knowledge of its credentials,
         * so that unauthenticated requests cannot fill the replay-tracking map.
         */
        Outcome consume(long nc) {
            return use(key, timestamp, nc);
        }
    }

    /**
     * Checks that the nonce is one this instance issued, which is all it takes to know that it is not garbage.
     * It involves only the nonce and the secret: no state is read or written, and no lock is needed.
     *
     * @return the nonce as verified, or empty if it is malformed or was not signed with this instance's secret
     */
    Optional<VerifiedNonce> verify(String nonce) {
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(nonce), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        String[] parts = raw.split(":", -1);
        if (parts.length != 3) {
            return Optional.empty();
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (!DigestCrypto.constantTimeEquals(secret.sign(parts[0] + ":" + parts[1]), parts[2])) {
            return Optional.empty();
        }
        return Optional.of(new VerifiedNonce(parts[0] + ":" + parts[1], timestamp));
    }

    enum Outcome {
        /**
         * The nonce is fresh and its {@code nc} is new: it has been recorded as the highest one used.
         */
        ACCEPTED,
        /**
         * The nonce is too old, or may have been used already but its tracking state was lost,
         * see the class documentation. The client should get a fresh one.
         */
        STALE,
        /**
         * The nonce is fresh, but its {@code nc} is not higher than the highest one used with it.
         */
        REPLAYED
    }

    private synchronized Outcome use(String key, long timestamp, long nc) {
        if (clock.millis() - timestamp > validity.toMillis()) {
            seenNonces.remove(key);
            return Outcome.STALE;
        }

        TrackedNonce previous = seenNonces.get(key);
        if (previous == null) {
            if (timestamp < untrustedBefore) {
                return Outcome.STALE;
            }
        } else if (nc <= previous.nc()) {
            return Outcome.REPLAYED;
        }
        seenNonces.put(key, new TrackedNonce(nc, timestamp));
        return Outcome.ACCEPTED;
    }

    /**
     * For tests: how many nonces are currently tracked.
     */
    synchronized int trackedNonceCount() {
        return seenNonces.size();
    }
}
