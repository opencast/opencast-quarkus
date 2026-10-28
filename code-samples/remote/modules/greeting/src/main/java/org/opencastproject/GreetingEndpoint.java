package org.opencastproject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/hello")
public class GreetingEndpoint {

    @GET
    public String hello(@QueryParam("name") String name) {
        String effectiveName = (name == null || name.isBlank()) ? "Quarkus" : name;
        String capitalized = Character.toUpperCase(effectiveName.charAt(0)) + effectiveName.substring(1);
        return "Hello, " + capitalized + "!";
    }
}
