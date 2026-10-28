package org.opencastproject;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CapitalizationServiceRemoteImpl implements CapitalizationService {

    @RestClient
    CapitalizationClient capitalizationClient;

    @Override
    public String capitalize(String text) {
        return capitalizationClient.capitalize(text);
    }
}
