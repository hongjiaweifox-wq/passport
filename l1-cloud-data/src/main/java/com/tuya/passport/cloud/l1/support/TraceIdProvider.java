package com.tuya.passport.cloud.l1.support;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TraceIdProvider {
    public String current() {
        String traceId = MDC.get("traceId");
        return traceId == null ? UUID.randomUUID().toString() : traceId;
    }
}
