package org.opencastproject;

import io.quarkus.security.Authenticated;
import io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/capitalize")
// Machine-to-machine endpoint: callers must be authenticated via a mutual TLS
// client certificate (@MTLSAuthentication selects the mTLS mechanism;
// @Authenticated rejects requests without one). The certificate's subject
// (e.g. CN=oc-admin) becomes the SecurityIdentity principal.
@MTLSAuthentication
@Authenticated
public class CapitalizationServiceResource {

    @Inject
    CapitalizationService capitalizationService;

    @GET
    public String capitalize(@QueryParam("text") String text) {
        return capitalizationService.capitalize(text);
    }
}
