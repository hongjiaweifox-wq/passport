package com.tuya.passport.cloud.l5.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="l5_passport",uniqueConstraints={@UniqueConstraint(name="uk_l5_passport_identity",columnNames={"org_code","issuer_code","unique_identifier"}),@UniqueConstraint(name="uk_l5_public_code",columnNames={"public_code"}),@UniqueConstraint(name="uk_l5_passport_unit",columnNames={"org_code","battery_unit_id"})})
public class PassportEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="battery_unit_id",nullable=false,length=36) private String batteryUnitId;
 @Column(name="unique_identifier",nullable=false,length=256) private String uniqueIdentifier;
 @Column(name="issuer_code",nullable=false,length=64) private String issuerCode;
 @Column(name="public_code",nullable=false,length=64) private String publicCode;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private PassportStatus status;
 @Column(name="current_version",nullable=false) private int currentVersion;
 @Column(name="status_reason",length=500) private String statusReason;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @Version @Column(nullable=false) private long version;
 protected PassportEntity(){}
 public PassportEntity(String org,String unit,String issuer,String unique,String code){id=UUID.randomUUID().toString();orgCode=org;batteryUnitId=unit;issuerCode=issuer;uniqueIdentifier=unique;publicCode=code;status=PassportStatus.DRAFT;currentVersion=0;createdAt=Instant.now();updatedAt=createdAt;}
 public void publish(int version){currentVersion=version;status=PassportStatus.PUBLISHED;statusReason=null;updatedAt=Instant.now();}
 public void suspend(String reason){status=PassportStatus.SUSPENDED;statusReason=reason;updatedAt=Instant.now();}
 public void resume(){if(status==PassportStatus.SUSPENDED){status=PassportStatus.PUBLISHED;statusReason=null;updatedAt=Instant.now();}}
 public String getId(){return id;}public String getOrgCode(){return orgCode;}public String getBatteryUnitId(){return batteryUnitId;}public String getUniqueIdentifier(){return uniqueIdentifier;}public String getIssuerCode(){return issuerCode;}public String getPublicCode(){return publicCode;}public PassportStatus getStatus(){return status;}public int getCurrentVersion(){return currentVersion;}public String getStatusReason(){return statusReason;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}
}
