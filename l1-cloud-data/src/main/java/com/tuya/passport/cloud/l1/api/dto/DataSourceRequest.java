package com.tuya.passport.cloud.l1.api.dto;

import com.tuya.passport.cloud.l1.domain.AuthMethod;
import com.tuya.passport.cloud.l1.domain.DataDomain;
import com.tuya.passport.cloud.l1.domain.DataFrequency;
import com.tuya.passport.cloud.l1.domain.DeliveryMethod;
import com.tuya.passport.cloud.l1.domain.SourceStatus;
import com.tuya.passport.cloud.l1.domain.SourceType;
import com.tuya.passport.cloud.l1.domain.TemplateType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record DataSourceRequest(
    @NotBlank @Size(max = 64) String sourceCode,
    @NotBlank @Size(max = 120) String sourceName,
    @NotNull SourceType sourceType,
    @NotBlank @Size(max = 120) String ownerTeam,
    @Email @Size(max = 160) String contact,
    @NotEmpty Set<DataDomain> dataDomains,
    @NotNull DeliveryMethod deliveryMethod,
    @NotNull DataFrequency frequency,
    @NotNull AuthMethod authMethod,
    Set<TemplateType> templateTypes,
    SourceStatus status,
    @Size(max = 1000) String description,
    Long version
) {}
