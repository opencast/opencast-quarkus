package org.opencastproject.project.structure.admin.specific.service.impl.endpoint;

import org.opencastproject.project.structure.admin.specific.service.api.AdminService;

import java.util.Collections;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("admin")
public class AdminSpecificServiceResource {

  @Inject
  AdminService service;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getGreeting() {
    return Collections.singletonMap("message", service.greeting());
  }
}
