package org.opencastproject.digest;

import java.util.Map;

/**
 * The result of {@link AuthParamParser#parseAs}.
 */
public sealed interface AuthParamResult {
    /**
     * The header uses the wanted scheme and its parameters are well-formed.
     *
     * @param params the parameters, with lowercase names
     */
    record Match(Map<String, String> params) implements AuthParamResult {
        public Match {
            params = Map.copyOf(params);
        }
    }

    /**
     * The header is not meant for the wanted scheme: it uses another one, or does not start with a scheme at all.
     * The rest of the header was not looked at.
     */
    record NoMatch() implements AuthParamResult {
    }

    /**
     * The header uses the wanted scheme, but its parameters are malformed.
     *
     * @param reason what is wrong with them
     */
    record Malformed(String reason) implements AuthParamResult {
    }
}
