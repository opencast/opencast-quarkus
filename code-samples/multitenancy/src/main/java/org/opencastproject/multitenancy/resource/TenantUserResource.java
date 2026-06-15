package org.opencastproject.multitenancy.resource;

import java.util.List;
import java.util.Optional;

import org.opencastproject.multitenancy.entity.User;
import io.github.mathias82.quarkus.multitenancy.core.runtime.context.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


@Path("/api/tenant_users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TenantUserResource {

  @Inject
  TenantContext tenantContext;

  @GET
  public List<User> listAll() {
    return User.listAll();
  }

  @GET
  @Path("/tenant")
  public Optional<String> tenant() {
    return tenantContext.getTenantId();
  }
}
