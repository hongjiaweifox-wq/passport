package com.tuya.passport.cloud.l3.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="l3_battery_unit",indexes=@Index(name="idx_l3_unit_org_sn",columnList="org_code,sn_normalized"),uniqueConstraints={@UniqueConstraint(name="uk_l3_unit_sn",columnNames={"org_code","issuer_code","sn_normalized"}),@UniqueConstraint(name="uk_l3_unit_source",columnNames={"org_code","source_system","source_record_id"})})
public class BatteryUnitEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="issuer_code",nullable=false,length=64) private String issuerCode;
 @Column(name="sn_raw",nullable=false,length=128) private String snRaw;
 @Column(name="sn_normalized",nullable=false,length=128) private String snNormalized;
 @Column(name="model_revision_id",nullable=false,length=36) private String modelRevisionId;
 @Column(name="batch_id",length=36) private String batchId;
 @Column(name="manufactured_at") private OffsetDateTime manufacturedAt;
 @Column(name="source_system",nullable=false,length=64) private String sourceSystem;
 @Column(name="source_record_id",nullable=false,length=128) private String sourceRecordId;
 @Column(name="source_hash",nullable=false,length=64) private String sourceHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private BatteryUnitStatus status;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @jakarta.persistence.Lob @jakarta.persistence.Column(name="attribute_json") @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) private String attributeJson;
 @Version @Column(nullable=false) private long version;
 protected BatteryUnitEntity() {}
 public BatteryUnitEntity(String org,String issuer,String raw,String normalized,String revisionId,String batchId,OffsetDateTime manufactured,String sourceSystem,String sourceRecordId,String sourceHash){this(org,issuer,raw,normalized,revisionId,batchId,manufactured,sourceSystem,sourceRecordId,sourceHash,null);}
 public BatteryUnitEntity(String org,String issuer,String raw,String normalized,String revisionId,String batchId,OffsetDateTime manufactured,String sourceSystem,String sourceRecordId,String sourceHash,String attributeJson){id=UUID.randomUUID().toString();orgCode=org;issuerCode=issuer;snRaw=raw;snNormalized=normalized;modelRevisionId=revisionId;this.batchId=batchId;manufacturedAt=manufactured;this.sourceSystem=sourceSystem;this.sourceRecordId=sourceRecordId;this.sourceHash=sourceHash;this.attributeJson=attributeJson;status=BatteryUnitStatus.ACTIVE;createdAt=Instant.now();}
 public String getId(){return id;} public String getOrgCode(){return orgCode;} public String getIssuerCode(){return issuerCode;} public String getSnRaw(){return snRaw;} public String getSnNormalized(){return snNormalized;} public String getModelRevisionId(){return modelRevisionId;} public String getBatchId(){return batchId;} public OffsetDateTime getManufacturedAt(){return manufacturedAt;} public String getSourceSystem(){return sourceSystem;} public String getSourceRecordId(){return sourceRecordId;} public String getSourceHash(){return sourceHash;} public BatteryUnitStatus getStatus(){return status;} public Instant getCreatedAt(){return createdAt;} public String getAttributeJson(){return attributeJson;}
}
