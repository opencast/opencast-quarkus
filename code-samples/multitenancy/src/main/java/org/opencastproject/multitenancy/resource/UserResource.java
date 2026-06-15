package org.opencastproject.multitenancy.resource;

import io.github.mathias82.quarkus.multitenancy.core.runtime.context.TenantContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/users")
public class UserResource {

  @GET
  @RolesAllowed("user")
  @Path("/me")
  public String me(@Context SecurityContext securityContext) {
    return securityContext.getUserPrincipal().getName();
  }
}
