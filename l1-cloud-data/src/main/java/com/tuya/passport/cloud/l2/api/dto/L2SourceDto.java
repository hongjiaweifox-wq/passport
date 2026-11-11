package com.tuya.passport.cloud.l2.api.dto;

public record L2SourceDto(String id, String orgCode, String sourceCode, String sourceName, String sourceType, String status, java.util.Set<String> templateTypes) {}
