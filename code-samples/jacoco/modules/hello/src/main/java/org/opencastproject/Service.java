package org.opencastproject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Service {

    private final String value;

    @Inject
    public Service(Dependency dependency) {
        this.value = dependency.value();
    }

    public String value() {
        return value;
    }
}
