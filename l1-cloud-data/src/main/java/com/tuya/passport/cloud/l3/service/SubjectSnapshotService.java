package com.tuya.passport.cloud.l3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.common.support.Hashing;
import com.tuya.passport.cloud.l3.domain.*;
import com.tuya.passport.cloud.l3.repository.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectSnapshotService {
 private final BatteryUnitRepository units; private final BatteryModelRepository models; private final ModelRevisionRepository revisions; private final DeviceBindingRepository bindings; private final LifecycleEventRepository events; private final Hashing hashing; private final ObjectMapper mapper;
 public SubjectSnapshotService(BatteryUnitRepository units,BatteryModelRepository models,ModelRevisionRepository revisions,DeviceBindingRepository bindings,LifecycleEventRepository events,Hashing hashing,ObjectMapper mapper){this.units=units;this.models=models;this.revisions=revisions;this.bindings=bindings;this.events=events;this.hashing=hashing;this.mapper=mapper.copy().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);}
 @Transactional(readOnly=true)
 public Map<String,Object> snapshot(String org,String type,String id){
  if("BATTERY_UNIT".equals(type)){
   BatteryUnitEntity unit=units.findByOrgCodeAndId(org,id).orElseThrow(()->ApiException.notFound("L3_NOT_FOUND","Battery unit not found"));
   ModelRevisionEntity revision=revisions.findByOrgCodeAndId(org,unit.getModelRevisionId()).orElseThrow(()->ApiException.notFound("L3_NOT_FOUND","Model revision not found"));
   BatteryModelEntity model=models.findByOrgCodeAndId(org,revision.getModelId()).orElseThrow(()->ApiException.notFound("L3_NOT_FOUND","Battery model not found"));
   Map<String,Object> result=new LinkedHashMap<>();result.put("subjectType",type);result.put("subjectId",id);result.put("model",modelMap(model));result.put("revision",revisionMap(revision));result.put("unit",unitMap(unit));result.put("bindings",bindings.findByOrgCodeAndBatteryUnitIdOrderByBoundAtDesc(org,id).stream().map(this::bindingMap).toList());result.put("events",events.findByOrgCodeAndBatteryUnitIdOrderByEventTimeDesc(org,id).stream().map(this::eventMap).toList());return result;
  }
  if("BATTERY_MODEL_REVISION".equals(type)){
   ModelRevisionEntity revision=revisions.findByOrgCodeAndId(org,id).orElseThrow(()->ApiException.notFound("L3_NOT_FOUND","Model revision not found"));BatteryModelEntity model=models.findByOrgCodeAndId(org,revision.getModelId()).orElseThrow(()->ApiException.notFound("L3_NOT_FOUND","Battery model not found"));Map<String,Object> result=new LinkedHashMap<>();result.put("subjectType",type);result.put("subjectId",id);result.put("model",modelMap(model));result.put("revision",revisionMap(revision));return result;
  }
  throw ApiException.badRequest("GOV_VALIDATION_ERROR","Unsupported subject type");
 }
 @Transactional(readOnly=true) public String currentHash(String org,String type,String id){return hashing.hash(snapshot(org,type,id));}
 public Map<String,Object> flatPublicFields(Map<String,Object> snapshot){Map<String,Object> out=new LinkedHashMap<>();@SuppressWarnings("unchecked") Map<String,Object> model=(Map<String,Object>)snapshot.get("model");@SuppressWarnings("unchecked") Map<String,Object> revision=(Map<String,Object>)snapshot.get("revision");@SuppressWarnings("unchecked") Map<String,Object> unit=(Map<String,Object>)snapshot.get("unit");if(model!=null)out.putAll(model);if(revision!=null)out.putAll(revision);if(unit!=null)out.putAll(unit);out.remove("id");out.remove("orgCode");out.remove("issuerCode");out.remove("modelId");out.remove("modelRevisionId");out.remove("batchId");out.remove("sourceHash");out.remove("status");return out;}
 private Map<String,Object> modelMap(BatteryModelEntity x){return mapper.convertValue(x,Map.class);}
 private Map<String,Object> revisionMap(ModelRevisionEntity x){return mapper.convertValue(x,Map.class);}
 private Map<String,Object> unitMap(BatteryUnitEntity x){return mapper.convertValue(x,Map.class);}
 private Map<String,Object> bindingMap(DeviceBindingEntity x){return mapper.convertValue(x,Map.class);}
 private Map<String,Object> eventMap(LifecycleEventEntity x){return mapper.convertValue(x,Map.class);}
}
