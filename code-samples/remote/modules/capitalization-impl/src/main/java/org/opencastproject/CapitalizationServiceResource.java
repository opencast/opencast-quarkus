package org.opencastproject;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/capitalize")
// Machine-to-machine endpoint: the path permission in the worker's
// application.properties requires an authenticated caller and restricts
// the mechanism to mTLS (auth-mechanism=X509), i.e. only a client
// certificate is accepted. The certificate's subject (e.g. CN=oc-admin)
// becomes the SecurityIdentity principal.
@Authenticated
public class CapitalizationServiceResource {

    @Inject
    CapitalizationService capitalizationService;

    @GET
    public String capitalize(@QueryParam("text") String text) {
        return capitalizationService.capitalize(text);
    }
}
