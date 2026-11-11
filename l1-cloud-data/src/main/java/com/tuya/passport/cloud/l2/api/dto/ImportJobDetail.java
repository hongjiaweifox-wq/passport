package com.tuya.passport.cloud.l2.api.dto;

import java.time.Instant;
import java.util.List;
public record ImportJobDetail(String id, String orgCode, String sourceCode, String sourceName, String entityType, String schemaVersion, String status,
    String fileName, long fileSize, String fileSha256, boolean scanPassed, int totalRows, int acceptedRows, int rejectedRows, int duplicateRows,
    int attemptNo, String failureCode, String failureMessage, String duplicateOfJobId, String createdBy, Instant createdAt, Instant startedAt, Instant completedAt, Instant updatedAt,
    List<ImportRowErrorDto> recentErrors) {}
