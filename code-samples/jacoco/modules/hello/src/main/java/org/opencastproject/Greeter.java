package org.opencastproject;

/**
 * A simple greeter service.
 */
public class Greeter {

    private final String name;

    public Greeter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name may not be null");
        }
        this.name = name;
    }

    public String greet() {
        assert name != null;
        return "Hello, " + name + "!";
    }
}
