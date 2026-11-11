package com.tuya.passport.cloud.l3.domain;

import com.tuya.passport.cloud.l2.domain.EntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l3_promotion_item",uniqueConstraints=@UniqueConstraint(name="uk_l3_promotion_item",columnNames={"org_code","run_id","row_no"}))
public class PromotionItemEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="run_id",nullable=false,length=36) private String runId;
 @Column(name="row_no",nullable=false) private int rowNo;
 @Enumerated(EnumType.STRING) @Column(name="entity_type",nullable=false,length=32) private EntityType entityType;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private PromotionItemStatus status;
 @Column(name="target_type",length=64) private String targetType;
 @Column(name="target_id",length=36) private String targetId;
 @Column(name="error_code",length=96) private String errorCode;
 @Column(name="error_message",length=500) private String errorMessage;
 @jakarta.persistence.Lob @Column(name="detail_json") private String detailJson;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected PromotionItemEntity() {}
 public PromotionItemEntity(String org,String run,int row,EntityType type,PromotionItemStatus status,String targetType,String targetId,String code,String message){this(org,run,row,type,status,targetType,targetId,code,message,null);}
 public PromotionItemEntity(String org,String run,int row,EntityType type,PromotionItemStatus status,String targetType,String targetId,String code,String message,String detailJson){id=UUID.randomUUID().toString();orgCode=org;runId=run;rowNo=row;entityType=type;this.status=status;this.targetType=targetType;this.targetId=targetId;errorCode=code;errorMessage=message;this.detailJson=detailJson;createdAt=Instant.now();}
 public String getId(){return id;} public String getOrgCode(){return orgCode;} public String getRunId(){return runId;} public int getRowNo(){return rowNo;} public EntityType getEntityType(){return entityType;} public PromotionItemStatus getStatus(){return status;} public String getTargetType(){return targetType;} public String getTargetId(){return targetId;} public String getErrorCode(){return errorCode;} public String getErrorMessage(){return errorMessage;} public Instant getCreatedAt(){return createdAt;} public String getDetailJson(){return detailJson;}
}
