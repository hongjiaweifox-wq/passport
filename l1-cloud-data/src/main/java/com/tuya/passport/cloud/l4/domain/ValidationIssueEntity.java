package com.tuya.passport.cloud.l4.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l4_validation_issue")
public class ValidationIssueEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="validation_run_id",nullable=false,length=36) private String validationRunId;
 @Column(name="field_code",length=96) private String fieldCode;
 @Column(nullable=false,length=12) private String severity;
 @Column(nullable=false,length=96) private String code;
 @Column(nullable=false,length=500) private String message;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected ValidationIssueEntity(){}
 public ValidationIssueEntity(String org,String run,String field,String severity,String code,String message){id=UUID.randomUUID().toString();orgCode=org;validationRunId=run;fieldCode=field;this.severity=severity;this.code=code;this.message=message;createdAt=Instant.now();}
 public String getId(){return id;}public String getOrgCode(){return orgCode;}public String getValidationRunId(){return validationRunId;}public String getFieldCode(){return fieldCode;}public String getSeverity(){return severity;}public String getCode(){return code;}public String getMessage(){return message;}public Instant getCreatedAt(){return createdAt;}
}
