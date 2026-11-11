package com.tuya.passport.cloud.l3.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="l3_device_binding",indexes=@Index(name="idx_l3_binding_unit_active",columnList="org_code,battery_unit_id,unbound_at"))
public class DeviceBindingEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="battery_unit_id",nullable=false,length=36) private String batteryUnitId;
 @Column(name="device_id",nullable=false,length=128) private String deviceId;
 @Column(name="bound_at",nullable=false) private OffsetDateTime boundAt;
 @Column(name="unbound_at") private OffsetDateTime unboundAt;
 @Column(name="change_reason",length=500) private String changeReason;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected DeviceBindingEntity() {}
 public DeviceBindingEntity(String org,String unit,String device,OffsetDateTime bound,String reason){id=UUID.randomUUID().toString();orgCode=org;batteryUnitId=unit;deviceId=device;boundAt=bound;changeReason=reason;createdAt=Instant.now();}
 public void close(OffsetDateTime at,String reason){unboundAt=at;changeReason=reason;}
 public String getId(){return id;} public String getOrgCode(){return orgCode;} public String getBatteryUnitId(){return batteryUnitId;} public String getDeviceId(){return deviceId;} public OffsetDateTime getBoundAt(){return boundAt;} public OffsetDateTime getUnboundAt(){return unboundAt;} public String getChangeReason(){return changeReason;}
}
