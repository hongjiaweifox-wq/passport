package com.tuya.passport.cloud.l1.api.dto;

public record ApiEnvelope<T>(T data, String traceId) {}
