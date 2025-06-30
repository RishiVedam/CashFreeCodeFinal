package com.example.payment.utils;

import org.slf4j.MDC;

import java.util.UUID;

public class TraceUtil {
    public static void ensureTraceContext() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
            MDC.put("spanId", UUID.randomUUID().toString().substring(0, 16));
        }
    }

    public static void clear() {
        MDC.clear();
    }
}
