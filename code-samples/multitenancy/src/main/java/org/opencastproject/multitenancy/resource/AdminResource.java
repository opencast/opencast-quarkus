package org.opencastproject.multitenancy.resource;

import io.github.mathias82.quarkus.multitenancy.core.runtime.context.TenantContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/admin")
public class AdminResource {
  @Inject
  TenantContext tenantContext;

  @GET
  @RolesAllowed("admin")
  @Produces(MediaType.TEXT_PLAIN)
  public String adminResource() {
    return tenantContext.getTenantId().get() + " admin";
  }
}
