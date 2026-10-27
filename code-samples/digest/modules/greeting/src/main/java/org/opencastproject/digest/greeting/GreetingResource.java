package org.opencastproject.digest.greeting;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A minimal protected endpoint used to demonstrate the {@code digest} module's
 * HTTP Digest Authentication mechanism end to end.
 */
@Path("/hello")
public class GreetingResource {
    @Inject
    SecurityIdentity identity;

    @GET
    @RolesAllowed("user")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello, " + identity.getPrincipal().getName() + "!";
    }
}
