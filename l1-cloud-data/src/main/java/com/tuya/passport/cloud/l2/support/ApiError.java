package com.tuya.passport.cloud.l2.support;

import java.util.Map;
public record ApiError(String code, String message, String traceId, Map<String, String> fieldErrors) {}
