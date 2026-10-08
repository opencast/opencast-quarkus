package org.opencastproject.digest;

import java.util.Map;

public record AuthParamResult(String scheme, Map<String, String> params) {
}
