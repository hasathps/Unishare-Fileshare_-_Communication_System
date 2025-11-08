package com.unishare.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON helpers for simple request and response payloads.
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    public static Map<String, String> parseObject(String json) {
        if (json == null) {
            return Collections.emptyMap();
        }

        String trimmed = json.trim();
        if (trimmed.isEmpty() || trimmed.equals("{}")) {
            return Collections.emptyMap();
        }

        if (trimmed.startsWith("{")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("}")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        Map<String, String> values = new LinkedHashMap<>();
        String[] pairs = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (keyValue.length != 2) {
                continue;
            }

            String key = clean(keyValue[0]);
            String value = clean(keyValue[1]);
            values.put(key, value);
        }

        return values;
    }

    public static String toJson(Map<String, ?> map) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) {
                builder.append(",");
            }
            builder.append("\"").append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                builder.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                builder.append(value.toString());
            } else {
                builder.append("\"").append(escape(value.toString())).append("\"");
            }
            first = false;
        }
        builder.append("}");
        return builder.toString();
    }

    private static String clean(String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.replace("\\\"", "\"");
    }

    private static String escape(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

