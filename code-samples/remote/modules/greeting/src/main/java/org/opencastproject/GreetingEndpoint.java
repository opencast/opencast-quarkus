package org.opencastproject;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/hello")
public class GreetingEndpoint {

    @Inject
    CapitalizationService capitalizationService;

    @GET
    public String hello(@QueryParam("name") String name) {
        String effectiveName = (name == null || name.isBlank()) ? "Quarkus" : name;
        return "Hello, " + capitalizationService.capitalize(effectiveName) + "!";
    }
}
