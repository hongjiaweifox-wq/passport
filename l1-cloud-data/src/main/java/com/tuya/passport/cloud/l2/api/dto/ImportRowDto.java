package com.tuya.passport.cloud.l2.api.dto;

public record ImportRowDto(int rowNo, String sourceRecordId, String recordHash, String canonicalJson, String status) {}
