package org.opencastproject.project.structure.admin.specific.service.impl;

import org.opencastproject.project.structure.admin.specific.service.api.AdminService;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdminSpecificServiceImpl implements AdminService {

  @Override
  public String greeting() {
    return "Hello from Admin Service!";
  }
}
