package org.opencastproject.project.structure.presentation.specific.service.impl;

import org.opencastproject.project.structure.admin.specific.service.api.PresentationService;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PresentationSpecificServiceImpl implements PresentationService {

  @Override
  public String greeting() {
    return "Hello from Presentation Service!";
  }
}
