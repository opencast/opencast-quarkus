package org.opencastproject.digest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import static org.junit.jupiter.api.Assertions.*;

class AuthParamParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "  ", " \t"})
    void blankHeader(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    // # schema

    @ParameterizedTest
    @ValueSource(strings = {
            "Basic",
            "Digest",
            "dIgEsT",
            "Digest.v2",
            "!#$%&'*+-"
    })
    void scheme(String input) throws Exception {
        var p = AuthParamParser.parse(input);
        assertEquals(input, p.scheme());
        assertTrue(p.params().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest ",
            "Digest  "
    })
    void trailingSpaceStrippedFromScheme(String input) throws Exception {
        var p = AuthParamParser.parse(input);
        assertEquals("Digest", p.scheme());
        assertTrue(p.params().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " Digest",
            "\tDigest",
            "Digest\t",
            "Digest \t"
    })
    void illegalWhitespaceAroundScheme(String input) throws Exception {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "(Digest",
            "Di@gest",
            "Digest\\"
    })
    void nonTokenCharInScheme(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    // # parameters

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest qop=auth",
            "Digest  qop=auth",
            "Digest qop =auth",
            "Digest qop\t=auth",
            "Digest qop  =auth",
            "Digest qop= auth",
            "Digest QoP=auth"
    })
    void singleTokenParam(String input) throws Exception {
        var p = AuthParamParser.parse(input);
        assertEquals("auth", p.params().get("qop"));
        assertEquals(1, p.params().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest \tqop=auth",
            "Digest qop=auth ",
            "Digest qop=auth\t",
            "Digest qop=auth  "
    })
    void invalidSpaces(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    // ## relation to schema

    @Test
    void noScheme() {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse("qop=auth"));
    }

    // ## token params

    @ParameterizedTest
    @ValueSource(strings = {"qop.v2", "!#$%&'*+-.^_`|~"})
    void keyContainsTokenChars(String input) throws Exception {
        var p = AuthParamParser.parse(
                "Digest " + input + "=token");
        assertEquals("token", p.params().get(input));
        assertEquals(1, p.params().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"user_1", "!#$%&'*+-.^_`|~"})
    void valueContainsTokenChars(String input) throws Exception {
        var p = AuthParamParser.parse(
                "Digest username=" + input);
        assertEquals(input, p.params().get("username"));
        assertEquals(1, p.params().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest @user=value",
            "Digest u/ser=value",
            "Digest user(=value",
            "Digest key=@value",
            "Digest key=v<lue",
            "Digest key=value)",
    })
    void nonTokenInParam(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest =",
            "Digest =auth",
            "Digest qop=",
            "Digest qop",
            "Digest qop auth",
            "Digest qop==auth",
            "Digest =qop=auth",
            "Digest qop=auth=",
            "Digest realm=qup=auth"
    })
    void unbalancedParam(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    // ## quoted params

    @ParameterizedTest
    @ValueSource(strings = {
            "user",
            "",
            "example@domain",
            "foo bar",
            "\tfoo",
            "bar\u0080"
    })
    void singleQuotedParam(String value) throws Exception {
        var p = AuthParamParser.parse("Digest username=\"" + value + "\"");
        assertEquals(value, p.params().get("username"));
        assertEquals(1, p.params().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest \"user\"=token",
            "Digest realm=\"example",
            "Digest realm=example\"",
            "Digest realm=\"exam\"ple\"",
            "Digest realm=\"example\\\"",
            "Digest realm=\"hello\\",
            "Digest realm=us\"er\"rest",
    })
    void invalidQuotes(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    @ParameterizedTest
    @MethodSource("escapeCases")
    void escaping(String quotedValue, String expected) throws Exception {
        var p = AuthParamParser.parse("Digest realm=\"" + quotedValue + "\"");
        assertEquals(expected, p.params().get("realm"));
        assertEquals(1, p.params().size());
    }

    static Stream<Arguments> escapeCases() {
        return Stream.of(
                arguments("hello\\\"world", "hello\"world"),
                arguments("\\\"hello world", "\"hello world"),
                arguments("hello world\\\"", "hello world\""),
                arguments("hello\\\\world", "hello\\world"),
                arguments("a\\\\\\\\b", "a\\\\b"),
                arguments("hello\\nworld", "hellonworld"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest realm=\"\u0000\"",
            "Digest realm=\"a\u0007b\"",
            "Digest realm=\"\u001Bb\"",
            "Digest realm=\"a\u007Fb\"",
            "Digest realm=\"\\\u0000\""
    })
    void invalidCharsInQuotedParam(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    // ## multiple parameters

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest username=\"user\", qop=auth",
            "Digest qop=auth, username=\"user\"",
            "Digest username=\"user\",\tqop=auth",
            "Digest username=\"user\" ,qop=auth",
            "Digest username=\"user\",, qop=auth",
            "Digest username=\"user\", , qop=auth",
            "Digest , username=\"user\", qop=auth",
            "Digest \t, username=\"user\", qop=auth",
            "Digest username=\"user\", qop=auth,",
            "Digest username=\"user\", qop=auth, ",
    })
    void multipleParams(String input) throws Exception {
        var p = AuthParamParser.parse(input);
        assertEquals("user", p.params().get("username"));
        assertEquals("auth", p.params().get("qop"));
        assertEquals(2, p.params().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest ,",
            "Digest ,,",
            "Digest , ,",
            "Digest , , ",
    })
    void degenerateCommas(String input) throws Exception {
        var p = AuthParamParser.parse(input);
        assertTrue(p.params().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest realm=first, realm=second",
            "Digest realm=first, realm=\"second\"",
            "Digest realm=first, qop=auth, realm=second",
            "Digest realm=first, realm=second, qop=auth",
            "Digest realm=first, ReAlM=second",
            "Digest rEaLm=first, realm=second",
            "Digest qop=auth, realm=first, realm=second, qop=auth-int"
    })
    void duplicateKeysRejected(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Digest,realm=exampleqop=auth",
            "Digest realm=exampleqop=auth",
            "Digest realm=example qop=auth",
            "Digest realm=example\tqop=auth",
            "Digest realm= qop=auth",
            "Digest realm=example =auth",
            "Digest realm=,qop=auth",
            "Digest realm=example,=auth",
            "Digest realm= qop=auth",
            "Digest realm=example =auth"
    })
    void missingParts(String input) {
        assertThrows(AuthParamParseException.class,
                () -> AuthParamParser.parse(input));
    }

    // ## assert precondition checks

    @Test
    void parseTokenAssertFiresOnNonTokenChar() throws Exception {
        AuthParamParser parser = createParser("Digest realm=foo");
        setPos(parser, 6);
        Method parseToken = AuthParamParser.class.getDeclaredMethod("parseToken");
        parseToken.setAccessible(true);
        var ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> parseToken.invoke(parser));
        assertInstanceOf(AssertionError.class, ex.getCause());
    }

    @Test
    void isTokenCharAssertFiresBeyondEnd() throws Exception {
        AuthParamParser parser = createParser("Digest");
        setPos(parser, 6);
        Method isTokenChar = AuthParamParser.class.getDeclaredMethod("isTokenChar");
        isTokenChar.setAccessible(true);
        var ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> isTokenChar.invoke(parser));
        assertInstanceOf(AssertionError.class, ex.getCause());
    }

    @Test
    void isQuotedCharAssertFiresBeyondEnd() throws Exception {
        AuthParamParser parser = createParser("Digest");
        setPos(parser, 6);
        Method isQuotedChar = AuthParamParser.class.getDeclaredMethod("isQuotedChar");
        isQuotedChar.setAccessible(true);
        var ex = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> isQuotedChar.invoke(parser));
        assertInstanceOf(AssertionError.class, ex.getCause());
    }

    private static AuthParamParser createParser(String input) throws Exception {
        Constructor<AuthParamParser> ctor = AuthParamParser.class.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(input);
    }

    private static void setPos(AuthParamParser parser, int pos) throws Exception {
        Field posField = AuthParamParser.class.getDeclaredField("pos");
        posField.setAccessible(true);
        posField.setInt(parser, pos);
    }
}
