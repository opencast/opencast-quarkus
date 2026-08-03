package org.opencastproject.digest;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A pretty direct recursive descent parser implementation
 * for RFC 9110 HTTP Auth Parameters.
 * Some liberties were taken because of perceived contradictions in the grammar.
 * These only make the parser more permissive,
 * and should not introduce any security risks.
 */
public final class AuthParamParser {
    private static final String TOKEN_CHARS = "!#$%&'*+-.^_`|~";

    private final String s;
    private int pos;
    private final String scheme;
    private final Map<String, String> params = new HashMap<>();

    /**
     * Parses an HTTP Authorization header value
     * into its scheme and parameters.
     * Parameter names are stored in lowercase.
     * Duplicate parameters are rejected.
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-11.4">RFC 9110 §11.4 — Authorization</a>
     */
    public static AuthParamResult parse(String header) throws AuthParamParseException {
        AuthParamParser authParamParser = new AuthParamParser(header);
        return new AuthParamResult(authParamParser.scheme, authParamParser.params);
    }

    /**
     * Partially implements the {@code credentials} production
     * from RFC 9110 §11.4:
     * <pre>
     * credentials = auth-scheme [ 1*SP [ [ auth-param ] *( OWS "," OWS [ auth-param ] ) ]
     * </pre>
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-11.4">RFC 9110 §11.4 — Authorization</a>
     */
    private AuthParamParser(String s) throws AuthParamParseException {
        this.s = s;

        if (s == null) {
            throw new AuthParamParseException("header must not be null");
        }
        if (s.isEmpty() || !isTokenChar()) {
            throw new AuthParamParseException("expected token at position 0");
        }

        scheme = parseToken();
        if (pos >= s.length()) {
            return;
        }
        expect(' ');
        skipSP();
        if (pos >= s.length()) {
            return;
        }

        if (isTokenChar()) {
            parseParam();
        }

        while (pos < s.length()) {
            skipOWS();
            expect(',');
            skipOWS();

            if (pos >= s.length()) {
                return;
            }

            if (isTokenChar()) {
                parseParam();
            }
        }
    }

    /**
     * Parses a {@code token} (RFC 9110 §5.6.2):
     * <pre>
     * token = 1*tchar
     * </pre>
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-5.6.2">RFC 9110 §5.6.2 — Token</a>
     */
    private String parseToken() {
        assert isTokenChar();
        int start = pos;
        do {
            ++pos;
        } while (pos < s.length() && isTokenChar());
        return s.substring(start, pos);
    }

    /**
     * Checks whether the character at the current position is a valid
     * {@code tchar} (RFC 9110 §5.6.2):
     * <pre>
     * tchar = "!" / "#" / "$" / "%" / "&" / "'" / "*"
     *       / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
     *       / DIGIT / ALPHA
     * </pre>
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-5.6.2">RFC 9110 §5.6.2 — Token</a>
     */
    private boolean isTokenChar() {
        assert pos < s.length();
        char c = s.charAt(pos);
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') ||
                TOKEN_CHARS.indexOf(c) >= 0;
    }

    /**
     * Parses a single {@code auth-param} (RFC 9110 §11.4.2):
     * <pre>
     * auth-param = token BWS "=" BWS ( token / quoted-string )
     * </pre>
     * where BWS is interpreted as OWS per the RFC's own recommendation.
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-11.4.2">RFC 9110 §11.4.2 — Authentication Parameter</a>
     */
    private void parseParam() throws AuthParamParseException {
        int keyStart = pos;
        String key = parseToken().toLowerCase(Locale.ROOT);

        if (params.containsKey(key)) {
            throw new AuthParamParseException("duplicate parameter '" + key + "' at position " + keyStart);
        }

        skipOWS();
        expect('=');
        skipOWS();

        if (pos >= s.length()) {
            throw new AuthParamParseException("unexpected end of input");
        }
        String value;
        if (s.charAt(pos) == '"') {
            value = parseQuoted();
        } else if (isTokenChar()) {
            value = parseToken();
        } else {
            throw new AuthParamParseException("expected token or quoted string at position " + pos);
        }

        params.put(key, value);
    }

    /**
     * Parses a {@code quoted-string} (RFC 9110 §5.6.4):
     * <pre>
     * quoted-string = DQUOTE *( qdtext / quoted-pair ) DQUOTE
     * quoted-pair   = "\" ( HTAB / SP / VCHAR / obs-text )
     * </pre>
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-5.6.4">RFC 9110 §5.6.4 — Quoted String</a>
     */
    private String parseQuoted() throws AuthParamParseException {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                ++pos;
                if (pos >= s.length()) {
                    throw new AuthParamParseException("unexpected end of input");
                }
                c = s.charAt(pos);
            }
            if (!isQuotedChar()) {
                throw new AuthParamParseException("invalid character '" + c + "' at position " + pos);
            }
            ++pos;
            sb.append(c);
        }
        expect('"');
        return sb.toString();
    }

    /**
     * Checks whether the character at the current position is valid inside
     * a quoted string's {@code qdtext} (RFC 9110 §5.6.4):
     * <pre>
     * qdtext = HTAB / SP / %x21 / %x23-5B / %x5D-7E / obs-text
     * VCHAR  = %x21-7E
     * obs-text = %x80-FF
     * </pre>
     * Note: {@code "} and {@code \} are valid VCHAR but handled specially
     * by the caller (as quoted-pair delimiters).
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-5.6.4">RFC 9110 §5.6.4 — Quoted String</a>
     */
    private boolean isQuotedChar() {
        assert pos < s.length();
        char c = s.charAt(pos);
        return c == '\t' ||
                c == ' ' ||
                (c >= '!' && c <= '~') ||
                c >= 0x80;
    }

    /**
     * Skips one or more spaces (RFC 9110 §5.6.3):
     * <pre>
     * 1*SP
     * SP = %x20
     * </pre>
     * Used after the scheme where exactly one space is required by the
     * {@code credentials} production before any parameters.
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-5.6.3">RFC 9110 §5.6.3 — Whitespace</a>
     */
    private void skipSP() {
        while (pos < s.length() && s.charAt(pos) == ' ') {
            ++pos;
        }
    }

    /**
     * Skips optional whitespace (RFC 9110 §5.6.3):
     * <pre>
     * OWS = *( SP / HTAB )
     * </pre>
     *
     * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-5.6.3">RFC 9110 §5.6.3 — Whitespace</a>
     */
    private void skipOWS() {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c != ' ' && c != '\t') {
                break;
            }
            ++pos;
        }
    }

    /**
     * Advances past the expected literal character.
     *
     * @throws AuthParamParseException if the current character does not match
     *         or the input is exhausted
     */
    private void expect(char expected) throws AuthParamParseException {
        if (pos >= s.length()) {
            throw new AuthParamParseException("unexpected end of input");
        }
        if (s.charAt(pos) != expected) {
            throw new AuthParamParseException("expected '" + expected + "' at position " + pos + " but found '" + s.charAt(pos) + "'");
        }
        ++pos;
    }
}
