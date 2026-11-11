package com.tuya.passport.cloud.common.api;

import java.util.Map;

public record ApiError(String code, String message, String traceId, Map<String, String> fieldErrors) {}
