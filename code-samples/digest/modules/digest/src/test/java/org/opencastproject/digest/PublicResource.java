package org.opencastproject.digest;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A public endpoint, to show how {@link DigestAuthenticationMechanism} treats credentials sent to resources
 * that do not require authentication.
 */
@Path("/public")
public class PublicResource {
    @GET
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String open() {
        return "public";
    }
}
