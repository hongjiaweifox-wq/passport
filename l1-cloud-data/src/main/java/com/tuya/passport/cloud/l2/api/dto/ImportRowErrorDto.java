package com.tuya.passport.cloud.l2.api.dto;

public record ImportRowErrorDto(int rowNo, String sourceRecordId, String fieldCode, String errorCode, String safeMessage, String rawValueHash) {}
