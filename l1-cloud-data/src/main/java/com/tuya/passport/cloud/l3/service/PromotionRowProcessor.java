package com.tuya.passport.cloud.l3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.common.support.Hashing;
import com.tuya.passport.cloud.l2.domain.EntityType;
import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowStatus;
import com.tuya.passport.cloud.l2.repository.ImportJobRepository;
import com.tuya.passport.cloud.l2.repository.ImportRowRepository;
import com.tuya.passport.cloud.l3.domain.*;
import com.tuya.passport.cloud.l3.repository.*;
import com.tuya.passport.cloud.l3.domain.BatteryUnitEntity;
import com.tuya.passport.cloud.l3.domain.BatchEntity;
import com.tuya.passport.cloud.l3.domain.DeviceBindingEntity;
import com.tuya.passport.cloud.l3.domain.LifecycleEventEntity;
import com.tuya.passport.cloud.l3.support.TuyaSnDecoder;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionRowProcessor {
 private final ObjectMapper mapper; private final Hashing hashing; private final ImportJobRepository jobs; private final ImportRowRepository rows;
 private final BatteryModelRepository models; private final ModelRevisionRepository revisions; private final BatchRepository batches; private final BatteryUnitRepository units; private final DeviceBindingRepository bindings; private final LifecycleEventRepository events; private final SnAutoFillService snAutoFill;
 public PromotionRowProcessor(ObjectMapper mapper,Hashing hashing,ImportJobRepository jobs,ImportRowRepository rows,BatteryModelRepository models,ModelRevisionRepository revisions,BatchRepository batches,BatteryUnitRepository units,DeviceBindingRepository bindings,LifecycleEventRepository events,SnAutoFillService snAutoFill){this.mapper=mapper;this.hashing=hashing;this.jobs=jobs;this.rows=rows;this.models=models;this.revisions=revisions;this.batches=batches;this.units=units;this.bindings=bindings;this.events=events;this.snAutoFill=snAutoFill;}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public ItemOutcome process(String org,String jobId,int attempt,int rowNo){
  ImportJobEntity job=jobs.findByOrgCodeAndId(org,jobId).orElseThrow(()->ApiException.notFound("L3_NOT_FOUND","Import job not found"));
  ImportRowEntity row=rows.findByOrgCodeAndJobIdAndAttemptNo(org,jobId,attempt).stream().filter(r->r.getRowNo()==rowNo).findFirst().orElseThrow(()->ApiException.notFound("L3_NOT_FOUND","Accepted staging row not found"));
  if(row.getStatus()!=ImportRowStatus.ACCEPTED) return new ItemOutcome(PromotionItemStatus.FAILED,null,null,"L3_ROW_NOT_ACCEPTED","Only accepted rows can be promoted");
  try {JsonNode root=mapper.readTree(row.getCanonicalJson()); JsonNode data=root.path("data"); EntityType type=job.getEntityType();String source=txt(root,"sourceSystem"),recordId=txt(root,"sourceRecordId"),hash=row.getRecordHash();return switch(type){case BATTERY_MODEL->promoteModel(org,root,data,source,recordId,hash);case BATTERY_UNIT->promoteUnit(org,root,data,source,recordId,hash);case LIFECYCLE_EVENT->promoteEvent(org,root,data,source,recordId,hash);};}
  catch(ApiException ex){return new ItemOutcome(PromotionItemStatus.FAILED,null,null,ex.code(),ex.getMessage());} catch(Exception ex){return new ItemOutcome(PromotionItemStatus.FAILED,null,null,"L3_INVALID_CANONICAL_ROW","Staging row could not be promoted");}
 }
 private ItemOutcome promoteModel(String org,JsonNode root,JsonNode d,String source,String record,String hash){
  String code=txt(d,"model_code"),revisionCode=txt(d,"revision_code"),snProduct=txt(d,"sn_product_code");
  if(snProduct.isBlank())snProduct=productCodeFromName(code);
  BatteryModelEntity model=models.findByOrgCodeAndIssuerCodeAndModelCode(org,org,code).orElseGet(()->models.save(new BatteryModelEntity(org,org,code,txt(d,"manufacturer_name"),txt(d,"battery_category"))));
  if(!snProduct.isBlank()&&!snProduct.equals(model.getSnProductCode()))model.setSnProductCode(snProduct);
  ModelRevisionEntity existing=revisions.findByOrgCodeAndModelIdAndRevisionCode(org,model.getId(),revisionCode).orElse(null);
  if(existing!=null)return existing.getSourceHash().equals(hash)?new ItemOutcome(PromotionItemStatus.DUPLICATE,"BATTERY_MODEL_REVISION",existing.getId(),null,null):new ItemOutcome(PromotionItemStatus.CONFLICT,"BATTERY_MODEL_REVISION",existing.getId(),"L3_IDENTITY_CONFLICT","A different revision payload already exists");
  String attrs=hashing.json(mapper.convertValue(d,java.util.Map.class));
  ModelRevisionEntity created=revisions.save(new ModelRevisionEntity(org,model.getId(),revisionCode,txt(root,"schemaVersion"),decimal(d,"rated_capacity_kwh"),decimal(d,"rated_voltage_v"),txt(d,"chemistry"),attrs,source+":"+record,hash,date(d,"effective_from")));
  Map<String,Object> details=new LinkedHashMap<>();if(!snProduct.isBlank())details.put("snProductCode",snProduct);
  return new ItemOutcome(PromotionItemStatus.PROMOTED,"BATTERY_MODEL_REVISION",created.getId(),null,null,details);
 }
 private ItemOutcome promoteUnit(String org,JsonNode root,JsonNode d,String source,String record,String hash){
  BatteryUnitEntity existingSource=units.findByOrgCodeAndSourceSystemAndSourceRecordId(org,source,record).orElse(null);
  if(existingSource!=null)return existingSource.getSourceHash().equals(hash)?new ItemOutcome(PromotionItemStatus.DUPLICATE,"BATTERY_UNIT",existingSource.getId(),null,null):new ItemOutcome(PromotionItemStatus.CONFLICT,"BATTERY_UNIT",existingSource.getId(),"L3_IDENTITY_CONFLICT","Source record ID already exists with different content");
  String raw=txt(d,"sn");OffsetDateTime csvManufactured=offset(d,"manufactured_at");
  var resolved=snAutoFill.resolve(org,raw,txt(d,"model_code"),txt(d,"revision_code"),txt(d,"batch_code"),csvManufactured);
  BatteryUnitEntity existingSn=units.findByOrgCodeAndIssuerCodeAndSnNormalized(org,org,resolved.normalizedSn()).orElse(null);
  if(existingSn!=null)return new ItemOutcome(PromotionItemStatus.CONFLICT,"BATTERY_UNIT",existingSn.getId(),"L3_IDENTITY_CONFLICT","Serial number already belongs to another source record");
  String batchId=null;if(resolved.batchCode()!=null&&!resolved.batchCode().isBlank()){String code=resolved.batchCode();BatchEntity batch=batches.findByOrgCodeAndModelRevisionIdAndBatchCode(org,resolved.revision().getId(),code).orElseGet(()->batches.save(new BatchEntity(org,resolved.revision().getId(),code,"{}")));batchId=batch.getId();}
  Map<String,Object> attributes=new LinkedHashMap<>();Map<String,Object> userAttrs=parseAttributes(txt(d,"dpp_attributes"));if(!userAttrs.isEmpty())attributes.put("dpp",userAttrs);if(!resolved.snDerived().isEmpty())attributes.put("snDerived",resolved.snDerived());
  String attributeJson=attributes.isEmpty()?null:hashing.json(attributes);
  BatteryUnitEntity unit=units.save(new BatteryUnitEntity(org,org,raw,resolved.normalizedSn(),resolved.revision().getId(),batchId,resolved.manufacturedAt(),source,record,hash,attributeJson));
  String device=txt(d,"device_id");if(!device.isBlank())bindings.save(new DeviceBindingEntity(org,unit.getId(),device,OffsetDateTime.now(),"Initial import"));
  Map<String,Object> detail=new LinkedHashMap<>();if(!resolved.snDerived().isEmpty())detail.put("snDerived",resolved.snDerived());if(!resolved.autoFilled().isEmpty())detail.put("autoFilled",resolved.autoFilled());if(!resolved.conflicts().isEmpty()){java.util.List<Map<String,Object>> conflicts=new java.util.ArrayList<>();resolved.conflicts().forEach((field,values)->{Map<String,Object> entry=new LinkedHashMap<>();entry.put("field",field);if(values instanceof Map<?,?> m)entry.putAll((Map<String,Object>)m);conflicts.add(entry);});detail.put("conflicts",conflicts);}
  return new ItemOutcome(PromotionItemStatus.PROMOTED,"BATTERY_UNIT",unit.getId(),null,null,detail);
 }
 private ItemOutcome promoteEvent(String org,JsonNode root,JsonNode d,String source,String record,String hash){
  LifecycleEventEntity bySource=events.findByOrgCodeAndSourceSystemAndSourceRecordId(org,source,record).orElse(null);if(bySource!=null)return bySource.getSourceHash().equals(hash)?new ItemOutcome(PromotionItemStatus.DUPLICATE,"LIFECYCLE_EVENT",bySource.getId(),null,null):new ItemOutcome(PromotionItemStatus.CONFLICT,"LIFECYCLE_EVENT",bySource.getId(),"L3_IDENTITY_CONFLICT","Source record ID already exists with different content");
  String serial=Normalizer.normalize(txt(d,"sn").trim(),Normalizer.Form.NFC).toUpperCase(Locale.ROOT);BatteryUnitEntity unit=units.findByOrgCodeAndIssuerCodeAndSnNormalized(org,org,serial).orElse(null);if(unit==null)return new ItemOutcome(PromotionItemStatus.FAILED,null,null,"L3_BATTERY_UNIT_NOT_FOUND","Battery unit must be promoted before its events");
  LifecycleEventEntity event=events.save(new LifecycleEventEntity(org,unit.getId(),txt(d,"event_type"),offset(d,"event_time"),txt(d,"actor_org_code"),txt(d,"reason_code"),txt(d,"evidence_ref"),hashing.json(mapper.convertValue(d,java.util.Map.class)),source,record,hash));return new ItemOutcome(PromotionItemStatus.PROMOTED,"LIFECYCLE_EVENT",event.getId(),null,null);
 }
 private Map<String,Object> parseAttributes(String json){try{if(json==null||json.isBlank())return Map.of();return mapper.readValue(json,mapper.getTypeFactory().constructMapType(LinkedHashMap.class,String.class,Object.class));}catch(Exception ex){return Map.of();}}
 private String productCodeFromName(String model){for(var e:Map.ofEntries(Map.entry("A","CBE2000"),Map.entry("B","CBE2000S"),Map.entry("C","CBE2000 Pro"),Map.entry("D","CBE2000 AC"),Map.entry("E","CBE Expansion Battery 2000"),Map.entry("F","CBE Expansion Battery 5000"),Map.entry("G","Lyra2500 AC"),Map.entry("H","Lyra2500 Pro"),Map.entry("I","CBE5000 Pro"),Map.entry("J","Atlas6000 AC"),Map.entry("K","Nova Air EC SP"),Map.entry("L","Nova Air Battery 5000"),Map.entry("M","Nova Apex EC SP"),Map.entry("N","Nova Apex EC TP"),Map.entry("P","Nova Apex Battery 6000"),Map.entry("Q","Nova Apex Battery 8000"),Map.entry("R","PowerSync")).entrySet())if(e.getValue().equalsIgnoreCase(model.trim()))return e.getKey();return "";}
 private static String txt(JsonNode node,String field){JsonNode value=node.path(field);return value.isMissingNode()||value.isNull()?"":value.asText("").trim();}
 private static BigDecimal decimal(JsonNode d,String field){String v=txt(d,field);return v.isBlank()?null:new BigDecimal(v);}
 private static LocalDate date(JsonNode d,String field){String v=txt(d,field);return v.isBlank()?null:LocalDate.parse(v);}
 private static OffsetDateTime offset(JsonNode d,String field){String v=txt(d,field);return v.isBlank()?null:OffsetDateTime.parse(v);}
 public record ItemOutcome(PromotionItemStatus status,String targetType,String targetId,String errorCode,String errorMessage,Map<String,Object> detail){public ItemOutcome(PromotionItemStatus s,String tt,String ti,String ec,String em){this(s,tt,ti,ec,em,Map.of());}}
}
