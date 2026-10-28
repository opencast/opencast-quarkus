package org.opencastproject;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/capitalize")
public class CapitalizationServiceResource {

    @Inject
    CapitalizationService capitalizationService;

    @GET
    public String capitalize(@QueryParam("text") String text) {
        return capitalizationService.capitalize(text);
    }
}
