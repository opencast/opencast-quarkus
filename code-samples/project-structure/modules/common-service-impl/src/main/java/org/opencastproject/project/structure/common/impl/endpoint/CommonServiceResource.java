package org.opencastproject.project.structure.common.impl.endpoint;

import org.opencastproject.project.structure.common.impl.api.CommonService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("common")
public class CommonServiceResource {

  @Inject
  CommonService service;

  @GET
  @Path("profiles")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, List<String>> getQuarkusProfiles() {
    return Collections.singletonMap("profiles", service.getQuarkusProfiles());
  }

  @GET
  @Path("profiles/active")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, List<String>> getQuarkusActiveProfiles() {
    return Collections.singletonMap("profiles", service.getQuarkusActiveProfiles());
  }
}
