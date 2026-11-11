package com.tuya.passport.cloud.l2.api.dto;

import java.time.Instant;
public record ImportJobListItem(String id, String orgCode, String sourceCode, String sourceName, String entityType, String schemaVersion, String status,
    String fileName, long fileSize, int totalRows, int acceptedRows, int rejectedRows, int duplicateRows, int attemptNo, String failureCode, Instant createdAt, Instant updatedAt) {}
