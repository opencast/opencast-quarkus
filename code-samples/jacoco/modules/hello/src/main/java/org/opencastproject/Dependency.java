package org.opencastproject;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Dependency {

    public String value() {
        return "value";
    }
}
