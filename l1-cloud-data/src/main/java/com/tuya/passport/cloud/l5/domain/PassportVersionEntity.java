package com.tuya.passport.cloud.l5.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l5_passport_version",uniqueConstraints={@UniqueConstraint(name="uk_l5_passport_version_no",columnNames={"org_code","passport_id","version_no"}),@UniqueConstraint(name="uk_l5_passport_idempotency",columnNames={"org_code","passport_id","idempotency_key"})})
public class PassportVersionEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="passport_id",nullable=false,length=36) private String passportId;
 @Column(name="version_no",nullable=false) private int versionNo;
 @Column(name="schema_version",nullable=false,length=64) private String schemaVersion;
 @Column(name="source_snapshot_hash",nullable=false,length=64) private String sourceSnapshotHash;
 @Lob @Column(name="public_json",nullable=false) private String publicJson;
 @Lob @Column(name="restricted_json",nullable=false) private String restrictedJson;
 @Lob @Column(name="regulator_json",nullable=false) private String regulatorJson;
 @Column(name="public_hash",nullable=false,length=64) private String publicHash;
 @Column(name="idempotency_key",length=128) private String idempotencyKey;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="created_by",nullable=false,length=128) private String createdBy;
 protected PassportVersionEntity(){}
 public PassportVersionEntity(String org,String passport,int version,String schema,String snapshotHash,String pub,String restricted,String regulator,String publicHash,String key,String actor){id=UUID.randomUUID().toString();orgCode=org;passportId=passport;versionNo=version;schemaVersion=schema;sourceSnapshotHash=snapshotHash;publicJson=pub;restrictedJson=restricted;regulatorJson=regulator;this.publicHash=publicHash;idempotencyKey=key;createdBy=actor;createdAt=Instant.now();}
 public String getId(){return id;}public String getOrgCode(){return orgCode;}public String getPassportId(){return passportId;}public int getVersionNo(){return versionNo;}public String getSchemaVersion(){return schemaVersion;}public String getSourceSnapshotHash(){return sourceSnapshotHash;}public String getPublicJson(){return publicJson;}public String getRestrictedJson(){return restrictedJson;}public String getRegulatorJson(){return regulatorJson;}public String getPublicHash(){return publicHash;}public String getIdempotencyKey(){return idempotencyKey;}public Instant getCreatedAt(){return createdAt;}public String getCreatedBy(){return createdBy;}
}
