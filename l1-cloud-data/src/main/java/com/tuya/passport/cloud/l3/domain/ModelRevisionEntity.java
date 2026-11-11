package com.tuya.passport.cloud.l3.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="l3_model_revision", indexes=@Index(name="idx_l3_revision_model",columnList="org_code,model_id"), uniqueConstraints=@UniqueConstraint(name="uk_l3_revision_code",columnNames={"org_code","model_id","revision_code"}))
public class ModelRevisionEntity {
    @Id @Column(length=36) private String id;
    @Column(name="org_code",nullable=false,length=64) private String orgCode;
    @Column(name="model_id",nullable=false,length=36) private String modelId;
    @Column(name="revision_code",nullable=false,length=128) private String revisionCode;
    @Column(name="schema_version",nullable=false,length=64) private String schemaVersion;
    @Column(name="rated_capacity_kwh",precision=16,scale=4) private BigDecimal ratedCapacityKwh;
    @Column(name="rated_voltage_v",precision=16,scale=4) private BigDecimal ratedVoltageV;
    @Column(name="chemistry_code",length=64) private String chemistryCode;
    @Lob @Column(name="attribute_json") private String attributeJson;
    @Column(name="source_ref",length=256) private String sourceRef;
    @Column(name="source_hash",nullable=false,length=64) private String sourceHash;
    @Column(name="effective_from") private LocalDate effectiveFrom;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private ModelRevisionStatus status;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected ModelRevisionEntity() {}
    public ModelRevisionEntity(String org,String modelId,String revision,String schema,BigDecimal capacity,BigDecimal voltage,String chemistry,String attributes,String sourceRef,String sourceHash,LocalDate effectiveFrom){id=UUID.randomUUID().toString();orgCode=org;this.modelId=modelId;revisionCode=revision;schemaVersion=schema;ratedCapacityKwh=capacity;ratedVoltageV=voltage;chemistryCode=chemistry;attributeJson=attributes;this.sourceRef=sourceRef;this.sourceHash=sourceHash;this.effectiveFrom=effectiveFrom;status=ModelRevisionStatus.ACTIVE;createdAt=Instant.now();}
    public String getId(){return id;} public String getOrgCode(){return orgCode;} public String getModelId(){return modelId;} public String getRevisionCode(){return revisionCode;} public String getSchemaVersion(){return schemaVersion;} public BigDecimal getRatedCapacityKwh(){return ratedCapacityKwh;} public BigDecimal getRatedVoltageV(){return ratedVoltageV;} public String getChemistryCode(){return chemistryCode;} public String getAttributeJson(){return attributeJson;} public String getSourceHash(){return sourceHash;} public LocalDate getEffectiveFrom(){return effectiveFrom;} public ModelRevisionStatus getStatus(){return status;}
}
