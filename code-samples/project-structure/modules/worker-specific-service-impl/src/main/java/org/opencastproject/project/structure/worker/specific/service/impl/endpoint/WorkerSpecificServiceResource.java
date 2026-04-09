package org.opencastproject.project.structure.worker.specific.service.impl.endpoint;

import org.opencastproject.project.structure.worker.specific.service.api.WorkerService;

import java.util.Collections;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("worker")
public class WorkerSpecificServiceResource {

  @Inject
  WorkerService service;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getGreeting() {
    return Collections.singletonMap("message", service.greeting());
  }
}
