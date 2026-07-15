package org.opencastproject.multitenancy.resolver;

import org.jboss.logging.Logger;

import java.util.Optional;

import org.opencastproject.multitenancy.exception.TenantNotFoundException;
import io.github.mathias82.quarkus.multitenancy.core.runtime.api.TenantResolutionContext;
import io.github.mathias82.quarkus.multitenancy.core.runtime.api.TenantResolver;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;


@ApplicationScoped
public class SafeTenantResolver implements TenantResolver {

    private static final Logger LOGGER = Logger.getLogger(SafeTenantResolver.class);
    private static final String HEADER_NAME = "X-Tenant";

    @Inject
    TenantDataSourceRegistry registry;

    @Override
    public Optional<String> resolve(TenantResolutionContext context) {
        Optional<ContainerRequestContext> reqOpt = context.get(ContainerRequestContext.class);
        Optional<RoutingContext> routCxt = context.get(RoutingContext.class);
        if (reqOpt.isEmpty() && routCxt.isEmpty()) {
            LOGGER.info("No request or routing context found");
            return Optional.empty();
        }

        String tenantId = null;

        if (reqOpt.isPresent()) {
            ContainerRequestContext req = reqOpt.get();
            tenantId = req.getHeaderString(HEADER_NAME);
        }
        if (routCxt.isPresent()) {
            RoutingContext routCtx = routCxt.get();
            tenantId = routCtx.request().getHeader(HEADER_NAME);
        }


        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }

        tenantId = tenantId.trim();

        if (!registry.exists(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }

        return Optional.of(tenantId);
    }
}
