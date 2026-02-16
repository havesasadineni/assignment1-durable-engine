package com.durable.engine;

/**
 * Minimal JSON helper for this assignment.
 * We store outputs as a JSON string literal OR primitive string.
 * For a real project you'd use Jackson/Gson, but this is enough to prove durability.
 */
public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        // store everything as a JSON string
        String s = String.valueOf(obj);
        s = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + s + "\"";
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromJson(String json) {
        if (json == null || json.equals("null")) return null;
        // expect a quoted string
        String s = json;
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        s = s.replace("\\\"", "\"").replace("\\\\", "\\");
        return (T) s;
    }
}
