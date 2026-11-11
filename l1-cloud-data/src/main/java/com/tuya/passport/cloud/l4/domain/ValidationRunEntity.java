package com.tuya.passport.cloud.l4.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l4_validation_run")
public class ValidationRunEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="subject_type",nullable=false,length=32) private String subjectType;
 @Column(name="subject_id",nullable=false,length=36) private String subjectId;
 @Column(name="rule_set_id",nullable=false,length=64) private String ruleSetId;
 @Column(name="rule_version",nullable=false,length=32) private String ruleVersion;
 @Column(name="input_hash",nullable=false,length=64) private String inputHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private ValidationStatus status;
 @Column(name="error_count",nullable=false) private int errorCount;
 @Column(name="warning_count",nullable=false) private int warningCount;
 @Column(name="started_at",nullable=false) private Instant startedAt;
 @Column(name="finished_at",nullable=false) private Instant finishedAt;
 protected ValidationRunEntity(){}
 public ValidationRunEntity(String org,String type,String id,String hash,ValidationStatus status,int errors,int warnings){this.id=UUID.randomUUID().toString();orgCode=org;subjectType=type;subjectId=id;ruleSetId="EU-BATTERY-2027";ruleVersion="1";inputHash=hash;this.status=status;errorCount=errors;warningCount=warnings;startedAt=Instant.now();finishedAt=startedAt;}
 public String getId(){return id;}public String getOrgCode(){return orgCode;}public String getSubjectType(){return subjectType;}public String getSubjectId(){return subjectId;}public String getInputHash(){return inputHash;}public ValidationStatus getStatus(){return status;}public int getErrorCount(){return errorCount;}public int getWarningCount(){return warningCount;}public Instant getFinishedAt(){return finishedAt;}
}
