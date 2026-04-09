package org.opencastproject.project.structure.worker.specific.service.impl;

import org.opencastproject.project.structure.worker.specific.service.api.WorkerService;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkerSpecificServiceImpl implements WorkerService {

  @Override
  public String greeting() {
    return "Hello from Worker Service!";
  }
}
