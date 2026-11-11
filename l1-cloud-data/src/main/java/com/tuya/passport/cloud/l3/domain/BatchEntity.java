package com.tuya.passport.cloud.l3.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l3_batch",uniqueConstraints=@UniqueConstraint(name="uk_l3_batch_revision_code",columnNames={"org_code","model_revision_id","batch_code"}))
public class BatchEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="model_revision_id",nullable=false,length=36) private String modelRevisionId;
 @Column(name="batch_code",nullable=false,length=128) private String batchCode;
 @Column(name="attribute_json",length=4000) private String attributeJson;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected BatchEntity() {}
 public BatchEntity(String org,String revision,String code,String attrs){id=UUID.randomUUID().toString();orgCode=org;modelRevisionId=revision;batchCode=code;attributeJson=attrs;createdAt=Instant.now();}
 public String getId(){return id;} public String getOrgCode(){return orgCode;} public String getModelRevisionId(){return modelRevisionId;} public String getBatchCode(){return batchCode;}
}
