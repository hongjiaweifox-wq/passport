package com.tuya.passport.cloud.l1.api.dto;

import com.tuya.passport.cloud.l1.domain.DataSourceEntity;
import java.time.Instant;
import java.util.Set;

public record DataSourceResponse(
    String id, String orgCode, String sourceCode, String sourceName, String sourceType,
    String ownerTeam, String contact, Set<String> dataDomains, String deliveryMethod,
    String frequency, String authMethod, Set<String> templateTypes, String status,
    String description, long version, Instant createdAt, Instant updatedAt
) {
    public static DataSourceResponse from(DataSourceEntity entity) {
        return new DataSourceResponse(entity.getId(), entity.getOrgCode(), entity.getSourceCode(),
            entity.getSourceName(), entity.getSourceType().name(), entity.getOwnerTeam(), entity.getContact(),
            entity.getDataDomains().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()),
            entity.getDeliveryMethod().name(), entity.getFrequency().name(), entity.getAuthMethod().name(),
            entity.getTemplateTypes().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()),
            entity.getStatus().name(), entity.getDescription(), entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
