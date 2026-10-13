package org.opencastproject.digest;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A minimal protected endpoint that only exists to give
 * {@link DigestAuthenticationMechanismIntegrationTest} a real request to authenticate,
 * the same way a consuming application's own resources would be protected.
 */
@Path("/hello")
public class TestResource {
    @Inject
    SecurityIdentity identity;

    @GET
    @RolesAllowed("user")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello, " + identity.getPrincipal().getName() + "!";
    }
}
