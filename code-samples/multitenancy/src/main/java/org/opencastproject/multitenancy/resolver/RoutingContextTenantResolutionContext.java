package org.opencastproject.multitenancy.resolver;

import java.util.Optional;
import org.jboss.logging.Logger;
import io.github.mathias82.quarkus.multitenancy.core.runtime.api.TenantResolutionContext;
import io.vertx.ext.web.RoutingContext;

public class RoutingContextTenantResolutionContext implements TenantResolutionContext {
  private final RoutingContext routingContext;
  private static final Logger LOG = Logger.getLogger(RoutingContextTenantResolutionContext.class);

  public RoutingContextTenantResolutionContext(RoutingContext routingContext) {
    this.routingContext = routingContext;
  }

  @Override
  public <T> Optional<T> get(Class<T> type) {
    if (type.isInstance(routingContext)) {
      return Optional.of(type.cast(routingContext));
    }

    if (type.isAssignableFrom(routingContext.request().getClass())) {
      return Optional.of(type.cast(routingContext.request()));
    }

    return Optional.empty();

  }


}
