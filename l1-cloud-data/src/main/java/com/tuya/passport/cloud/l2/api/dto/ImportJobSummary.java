package com.tuya.passport.cloud.l2.api.dto;

import java.time.Instant;
public record ImportJobSummary(long total, long processing, long completed, long partial, long failed) {}
