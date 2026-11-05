package org.opencastproject.digest.greeting;

import io.smallrye.config.ConfigMapping;

import java.util.Map;

@ConfigMapping(prefix = "greeting")
public interface GreetingConfig {
    /**
     * Username/password pairs, e.g. {@code greeting.users.alice=secret}.
     * Meant for the demo only, as the passwords are plaintext in the configuration.
     */
    Map<String, String> users();
}
