package com.tuya.passport.cloud.l2.api.dto;

public record ImportJobCreated(String jobId, String status, String statusUrl, boolean reused, String duplicateOfJobId) {}
