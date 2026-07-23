package org.opencastproject;

/**
 * A simple greeter service.
 */
public class Greeter {

    private final String name;

    public Greeter(String name) {
        this.name = name;
    }

    public String greet() {
        return "Hello, " + name + "!";
    }
}
