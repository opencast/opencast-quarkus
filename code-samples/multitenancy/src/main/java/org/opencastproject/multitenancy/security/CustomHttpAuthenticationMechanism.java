package org.opencastproject.multitenancy.security;

import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.Set;

import org.opencastproject.multitenancy.resolver.RoutingContextTenantResolutionContext;
import org.opencastproject.multitenancy.resolver.SafeTenantResolver;
import io.github.mathias82.quarkus.multitenancy.core.runtime.context.TenantContext;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

// TODO: Resolve Tenant from Header only here
// TODO: Resolve Auth mechanism based on config
// Inject all possible AuthenticationMechanisms?
@Alternative
@Priority(1)
@ApplicationScoped
public class CustomHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

  private static final Logger LOGGER = Logger.getLogger(CustomHttpAuthenticationMechanism.class);
  @Inject
  BasicAuthenticationMechanism basicAuthenticationMechanism;
  @Inject
  SafeTenantResolver tenantResolver;
  @Inject
  TenantContext tenantContext;

  @Override
  @ActivateRequestContext
  public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
    RoutingContextTenantResolutionContext ctx = new RoutingContextTenantResolutionContext(context);
    Optional<String> resolveTenantID = tenantResolver.resolve(ctx);
    if (resolveTenantID.isPresent()) {
      tenantContext.setTenantId(resolveTenantID.get());
    } else {
    }
    LOGGER.info("Authenticating tenant " + tenantContext.getTenantId().get());
    return basicAuthenticationMechanism.authenticate(context, identityProviderManager);
  }

  @Override
  public Uni<ChallengeData> getChallenge(RoutingContext context) {
    return basicAuthenticationMechanism.getChallenge(context);
  }

  @Override
  public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
    return basicAuthenticationMechanism.getCredentialTypes();
  }

  @Override
  public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
    return basicAuthenticationMechanism.getCredentialTransport(context);

  }
}
