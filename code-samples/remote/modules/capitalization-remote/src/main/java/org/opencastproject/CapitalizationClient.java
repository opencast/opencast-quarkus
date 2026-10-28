package org.opencastproject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/capitalize")
@RegisterRestClient(configKey = "capitalization")
public interface CapitalizationClient {

    @GET
    String capitalize(@QueryParam("text") String text);
}
