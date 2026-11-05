package org.opencastproject.digest;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * The users of the test application, the way an application provides them to {@link DigestAuthenticationMechanism}.
 */
@ApplicationScoped
public class TestUsers implements DigestCredentialLookup {
    @Override
    public Optional<String> passwordFor(String username) {
        return "alice".equals(username) ? Optional.of("alice-password") : Optional.empty();
    }
}
