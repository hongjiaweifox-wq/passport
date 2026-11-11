package com.tuya.passport.cloud.l3.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="l3_lifecycle_event",indexes=@Index(name="idx_l3_event_unit_time",columnList="org_code,battery_unit_id,event_time"),uniqueConstraints=@UniqueConstraint(name="uk_l3_event_source",columnNames={"org_code","source_system","source_record_id"}))
public class LifecycleEventEntity {
 @Id @Column(length=36) private String id;
 @Column(name="org_code",nullable=false,length=64) private String orgCode;
 @Column(name="battery_unit_id",nullable=false,length=36) private String batteryUnitId;
 @Column(name="event_type",nullable=false,length=64) private String eventType;
 @Column(name="event_time",nullable=false) private OffsetDateTime eventTime;
 @Column(name="actor_org_code",length=64) private String actorOrgCode;
 @Column(name="reason_code",length=64) private String reasonCode;
 @Column(name="evidence_ref",length=512) private String evidenceRef;
 @Lob @Column(name="payload_json") private String payloadJson;
 @Column(name="source_system",nullable=false,length=64) private String sourceSystem;
 @Column(name="source_record_id",nullable=false,length=128) private String sourceRecordId;
 @Column(name="source_hash",nullable=false,length=64) private String sourceHash;
 @Column(name="created_at",nullable=false) private Instant createdAt;
 protected LifecycleEventEntity() {}
 public LifecycleEventEntity(String org,String unit,String type,OffsetDateTime time,String actorOrg,String reason,String evidence,String payload,String sourceSystem,String sourceId,String hash){id=UUID.randomUUID().toString();orgCode=org;batteryUnitId=unit;eventType=type;eventTime=time;actorOrgCode=actorOrg;reasonCode=reason;evidenceRef=evidence;payloadJson=payload;this.sourceSystem=sourceSystem;sourceRecordId=sourceId;sourceHash=hash;createdAt=Instant.now();}
 public String getId(){return id;} public String getOrgCode(){return orgCode;} public String getBatteryUnitId(){return batteryUnitId;} public String getEventType(){return eventType;} public OffsetDateTime getEventTime(){return eventTime;} public String getActorOrgCode(){return actorOrgCode;} public String getReasonCode(){return reasonCode;} public String getEvidenceRef(){return evidenceRef;} public String getPayloadJson(){return payloadJson;} public String getSourceSystem(){return sourceSystem;} public String getSourceRecordId(){return sourceRecordId;} public String getSourceHash(){return sourceHash;}
}
