package com.tuya.passport.cloud.l4.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l4_publication_candidate",indexes=@Index(name="idx_l4_candidate_ready",columnList="org_code,subject_type,subject_id,status,created_at"),uniqueConstraints=@UniqueConstraint(name="uk_l4_candidate_hash",columnNames={"org_code","subject_type","subject_id","snapshot_hash"}))
public class PublicationCandidateEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="subject_type",nullable=false,length=32) private String subjectType;
 @Column(name="subject_id",nullable=false,length=36) private String subjectId;
 @Column(name="snapshot_hash",nullable=false,length=64) private String snapshotHash;
 @Lob @Column(name="public_json",nullable=false) private String publicJson;
 @Lob @Column(name="restricted_json",nullable=false) private String restrictedJson;
 @Lob @Column(name="regulator_json",nullable=false) private String regulatorJson;
 @Column(name="validation_run_id",nullable=false,length=36) private String validationRunId;
 @Column(name="review_task_id",nullable=false,length=36) private String reviewTaskId;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private CandidateStatus status;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected PublicationCandidateEntity(){}
 public PublicationCandidateEntity(String org,String type,String subject,String hash,String pub,String restricted,String regulator,String run,String review){id=UUID.randomUUID().toString();orgCode=org;subjectType=type;subjectId=subject;snapshotHash=hash;publicJson=pub;restrictedJson=restricted;regulatorJson=regulator;validationRunId=run;reviewTaskId=review;status=CandidateStatus.READY;createdAt=Instant.now();}
 public void consume(){status=CandidateStatus.CONSUMED;}public void stale(){status=CandidateStatus.STALE;}
 public String getId(){return id;}public String getOrgCode(){return orgCode;}public String getSubjectType(){return subjectType;}public String getSubjectId(){return subjectId;}public String getSnapshotHash(){return snapshotHash;}public String getPublicJson(){return publicJson;}public String getRestrictedJson(){return restrictedJson;}public String getRegulatorJson(){return regulatorJson;}public String getValidationRunId(){return validationRunId;}public String getReviewTaskId(){return reviewTaskId;}public CandidateStatus getStatus(){return status;}public Instant getCreatedAt(){return createdAt;}
}
