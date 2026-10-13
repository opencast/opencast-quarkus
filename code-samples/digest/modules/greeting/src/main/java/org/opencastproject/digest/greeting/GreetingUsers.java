package org.opencastproject.digest.greeting;

import jakarta.enterprise.context.ApplicationScoped;
import org.opencastproject.digest.DigestCredentialLookup;

import java.util.Map;
import java.util.Optional;

/**
 * The users of the demo, as an application provides them to the {@code digest} module.
 * A real application would look them up in its own user store instead.
 */
@ApplicationScoped
public class GreetingUsers implements DigestCredentialLookup {
    private final Map<String, String> users;

    GreetingUsers(GreetingConfig config) {
        this.users = config.users();
    }

    @Override
    public Optional<String> passwordFor(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
