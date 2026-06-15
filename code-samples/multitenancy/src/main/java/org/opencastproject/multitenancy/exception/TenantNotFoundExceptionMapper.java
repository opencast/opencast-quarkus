package org.opencastproject.multitenancy.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

// from https://github.com/mathias82/quarkus-multitenancy
@Provider
public class TenantNotFoundExceptionMapper implements ExceptionMapper<TenantNotFoundException> {

    @Override
    public Response toResponse(TenantNotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\": \"Tenant not found: " + e.getTenantId() + "\"}")
                .type("application/json")
                .build();
    }
}