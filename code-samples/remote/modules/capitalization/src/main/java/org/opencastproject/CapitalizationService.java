package org.opencastproject;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CapitalizationService {

    public String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
