package com.campustrade.util;

import java.util.UUID;

public class TraceIdUtil {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    public static String generate() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        TRACE_ID.set(traceId);
        return traceId;
    }

    public static String get() {
        String traceId = TRACE_ID.get();
        if (traceId == null) {
            return generate();
        }
        return traceId;
    }

    public static void set(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static void remove() {
        TRACE_ID.remove();
    }
}