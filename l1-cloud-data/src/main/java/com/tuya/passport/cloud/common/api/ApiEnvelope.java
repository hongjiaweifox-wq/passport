package com.tuya.passport.cloud.common.api;

public record ApiEnvelope<T>(T data, String traceId) {}
