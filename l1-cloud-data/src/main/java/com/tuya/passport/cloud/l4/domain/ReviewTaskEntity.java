package com.tuya.passport.cloud.l4.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l4_review_task",indexes=@Index(name="idx_l4_review_queue",columnList="org_code,status,created_at"))
public class ReviewTaskEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="subject_type",nullable=false,length=32) private String subjectType;
 @Column(name="subject_id",nullable=false,length=36) private String subjectId;
 @Column(name="snapshot_hash",nullable=false,length=64) private String snapshotHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private ReviewStatus status;
 @Column(name="maker_id",nullable=false,length=128) private String makerId;
 @Column(name="checker_id",length=128) private String checkerId;
 @Column(length=500) private String reason;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="decided_at") private Instant decidedAt;
 protected ReviewTaskEntity(){}
 public ReviewTaskEntity(String org,String type,String subject,String hash,String maker){id=UUID.randomUUID().toString();orgCode=org;subjectType=type;subjectId=subject;snapshotHash=hash;makerId=maker;status=ReviewStatus.PENDING_REVIEW;createdAt=Instant.now();}
 public void decide(String checker,String reason,boolean approve){checkerId=checker;this.reason=reason;status=approve?ReviewStatus.APPROVED:ReviewStatus.CHANGES_REQUESTED;decidedAt=Instant.now();}
 public String getId(){return id;}public String getOrgCode(){return orgCode;}public String getSubjectType(){return subjectType;}public String getSubjectId(){return subjectId;}public String getSnapshotHash(){return snapshotHash;}public ReviewStatus getStatus(){return status;}public String getMakerId(){return makerId;}public String getCheckerId(){return checkerId;}public String getReason(){return reason;}public Instant getCreatedAt(){return createdAt;}public Instant getDecidedAt(){return decidedAt;}
}
