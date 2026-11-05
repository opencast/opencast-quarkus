package org.opencastproject;

import io.quarkus.test.junit.QuarkusTest;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ServiceIntegrationTest {

    @Inject
    Service service;

    @Test
    void returnsDependencyValue() {
        assertEquals("value", service.value());
    }
}
