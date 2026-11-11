package com.tuya.passport.cloud.l1.api.dto;

import java.util.Map;

public record ApiError(String code, String message, String traceId, Map<String, String> fieldErrors) {}
