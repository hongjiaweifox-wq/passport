package com.tuya.passport.cloud.l1.api.dto;

import com.tuya.passport.cloud.l1.domain.SourceStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull SourceStatus status, @NotNull Long version) {}
