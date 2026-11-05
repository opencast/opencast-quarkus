package org.opencastproject.digest;

import java.util.Optional;

/**
 * Resolves the password a {@link DigestAuthenticator} should expect for a
 * given username.
 * <p>
 * Passwords are looked up in plain text (rather than as a precomputed
 * {@code H(A1)}) so that a single lookup can serve requests using either
 * of the supported {@link DigestAlgorithm}s.
 */
@FunctionalInterface
public interface DigestCredentialLookup {
    /**
     * @return the user's password, or {@link Optional#empty()} if no such user exists
     */
    Optional<String> passwordFor(String username);
}
