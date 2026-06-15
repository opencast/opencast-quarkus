package org.opencastproject.multitenancy.exception;

// from https://github.com/mathias82/quarkus-multitenancy
public class TenantNotFoundException extends RuntimeException {

    private final String tenantId;

    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }
}
