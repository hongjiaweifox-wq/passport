package com.tuya.passport.cloud.l2.support;

public record ApiEnvelope<T>(T data, String traceId) {}
