package org.opencastproject;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GreeterTest {

    @Test
    void shouldReturnGreeting() {
        Greeter greeter = new Greeter("World");
        assertEquals("Hello, World!", greeter.greet());
    }

    @Test
    void shouldThrowWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new Greeter(null));
    }

    @Test
    void shouldAssertWhenNameIsNull() throws Exception {
        Greeter greeter = new Greeter("dummy");
        Field nameField = Greeter.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(greeter, null);
        assertThrows(AssertionError.class, greeter::greet);
    }
}
