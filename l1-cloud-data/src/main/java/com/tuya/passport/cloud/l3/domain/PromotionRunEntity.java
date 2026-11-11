package com.tuya.passport.cloud.l3.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l3_promotion_run",uniqueConstraints=@UniqueConstraint(name="uk_l3_promotion_run_no",columnNames={"org_code","job_id","attempt_no","run_no"}))
public class PromotionRunEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="job_id",nullable=false,length=36) private String jobId;
 @Column(name="attempt_no",nullable=false) private int attemptNo;
 @Column(name="run_no",nullable=false) private int runNo;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private PromotionRunStatus status;
 @Column(name="total_rows",nullable=false) private int totalRows;
 @Column(nullable=false) private int promoted;
 @Column(nullable=false) private int duplicated;
 @Column(nullable=false) private int conflicted;
 @Column(nullable=false) private int failed;
 @Column(name="started_at",nullable=false) private Instant startedAt;
 @Column(name="finished_at") private Instant finishedAt;
 @Column(name="created_by",nullable=false,length=128) private String createdBy;
 protected PromotionRunEntity() {}
 public PromotionRunEntity(String org,String job,int attempt,int run,String actor){id=UUID.randomUUID().toString();orgCode=org;jobId=job;attemptNo=attempt;runNo=run;status=PromotionRunStatus.RUNNING;startedAt=Instant.now();createdBy=actor;}
 public void finish(int total,int promoted,int duplicated,int conflicted,int failed){totalRows=total;this.promoted=promoted;this.duplicated=duplicated;this.conflicted=conflicted;this.failed=failed;status=failed+conflicted==0?PromotionRunStatus.COMPLETED:(promoted+duplicated==0?PromotionRunStatus.FAILED:PromotionRunStatus.PARTIAL);finishedAt=Instant.now();}
 public String getId(){return id;} public String getOrgCode(){return orgCode;} public String getJobId(){return jobId;} public int getAttemptNo(){return attemptNo;} public int getRunNo(){return runNo;} public PromotionRunStatus getStatus(){return status;} public int getTotalRows(){return totalRows;} public int getPromoted(){return promoted;} public int getDuplicated(){return duplicated;} public int getConflicted(){return conflicted;} public int getFailed(){return failed;} public Instant getStartedAt(){return startedAt;} public Instant getFinishedAt(){return finishedAt;} public String getCreatedBy(){return createdBy;}
}
