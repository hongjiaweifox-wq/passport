package com.tuya.passport.cloud.l1.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "l1_data_source", uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_source_code", columnNames = {"org_code", "source_code"})
})
public class DataSourceEntity {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "org_code", nullable = false, length = 64, updatable = false)
    private String orgCode;

    @NotBlank
    @Size(max = 64)
    @Column(name = "source_code", nullable = false, length = 64)
    private String sourceCode;

    @NotBlank
    @Size(max = 120)
    @Column(name = "source_name", nullable = false, length = 120)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private SourceType sourceType;

    @NotBlank
    @Size(max = 120)
    @Column(name = "owner_team", nullable = false, length = 120)
    private String ownerTeam;

    @Email
    @Size(max = 160)
    @Column(length = 160)
    private String contact;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "l1_source_data_domain", joinColumns = @JoinColumn(name = "source_id"))
    @OrderColumn(name = "item_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "data_domain", nullable = false, length = 32)
    private Set<DataDomain> dataDomains = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 32)
    private DeliveryMethod deliveryMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 24)
    private DataFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false, length = 32)
    private AuthMethod authMethod;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "l1_source_template_type", joinColumns = @JoinColumn(name = "source_id"))
    @OrderColumn(name = "item_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 32)
    private Set<TemplateType> templateTypes = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceStatus status = SourceStatus.DRAFT;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DataSourceEntity() {}

    public DataSourceEntity(String orgCode, String sourceCode, String sourceName, SourceType sourceType,
                            String ownerTeam, String contact, Set<DataDomain> dataDomains,
                            DeliveryMethod deliveryMethod, DataFrequency frequency, AuthMethod authMethod,
                            Set<TemplateType> templateTypes, SourceStatus status, String description) {
        this.id = UUID.randomUUID().toString();
        this.orgCode = orgCode;
        update(sourceCode, sourceName, sourceType, ownerTeam, contact, dataDomains,
            deliveryMethod, frequency, authMethod, templateTypes, status, description);
    }

    public void update(String sourceCode, String sourceName, SourceType sourceType, String ownerTeam,
                       String contact, Set<DataDomain> dataDomains, DeliveryMethod deliveryMethod,
                       DataFrequency frequency, AuthMethod authMethod, Set<TemplateType> templateTypes,
                       SourceStatus status, String description) {
        this.sourceCode = sourceCode.trim().toUpperCase();
        this.sourceName = sourceName.trim();
        this.sourceType = sourceType;
        this.ownerTeam = ownerTeam.trim();
        this.contact = contact == null || contact.isBlank() ? null : contact.trim();
        this.dataDomains = new LinkedHashSet<>(dataDomains == null ? Set.of() : dataDomains);
        this.deliveryMethod = deliveryMethod;
        this.frequency = frequency;
        this.authMethod = authMethod;
        this.templateTypes = new LinkedHashSet<>(templateTypes == null ? Set.of() : templateTypes);
        this.status = status == null ? SourceStatus.DRAFT : status;
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.updatedAt = Instant.now();
    }

    public void changeStatus(SourceStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    @jakarta.persistence.PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    public String getId() { return id; }
    public String getOrgCode() { return orgCode; }
    public String getSourceCode() { return sourceCode; }
    public String getSourceName() { return sourceName; }
    public SourceType getSourceType() { return sourceType; }
    public String getOwnerTeam() { return ownerTeam; }
    public String getContact() { return contact; }
    public Set<DataDomain> getDataDomains() { return Set.copyOf(dataDomains); }
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public DataFrequency getFrequency() { return frequency; }
    public AuthMethod getAuthMethod() { return authMethod; }
    public Set<TemplateType> getTemplateTypes() { return Set.copyOf(templateTypes); }
    public SourceStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
