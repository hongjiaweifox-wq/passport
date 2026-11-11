package com.tuya.passport.cloud.l4.service;

import com.tuya.passport.cloud.l4.domain.FieldDefinitionEntity;
import com.tuya.passport.cloud.l4.domain.VisibilityClass;
import com.tuya.passport.cloud.l4.repository.FieldDefinitionRepository;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class FieldDefinitionService {
 private static final String SCHEMA="dpp-schema/1.0";
 private static final Map<String,String> PROTOTYPE_EXAMPLES=Map.ofEntries(
  Map.entry("revision_code","R01"),
  Map.entry("market_code","EU"),
  Map.entry("effective_from","2026-01-01"),
  Map.entry("rated_capacity_kwh","72.5"),
  Map.entry("batch_code","BATCH-2026-03"),
  Map.entry("device_id","DEV-0001"),
  Map.entry("actor_org_code","DEMO-ORG"),
  Map.entry("reason_code","INITIAL_IMPORT"),
  Map.entry("evidence_ref","EV-001"),
  Map.entry("event_type","MANUFACTURED"),
  Map.entry("event_time","2026-03-15T09:00:00Z"),
  Map.entry("source_system","DEMO_MES"),
  Map.entry("source_record_id","SRC-0001"));
 private final FieldDefinitionRepository definitions;
 private volatile List<CatalogSeed> catalog;
 public FieldDefinitionService(FieldDefinitionRepository definitions){this.definitions=definitions;}
 @Transactional public List<FieldDefinitionEntity> ensureDefaults(String org){
  List<CatalogSeed> seeds=catalog();
  for(CatalogSeed seed:seeds){
   FieldDefinitionEntity entity=definitions.findByOrgCodeAndFieldCodeAndSchemaVersion(org,seed.code(),SCHEMA).orElseGet(()->new FieldDefinitionEntity(org,seed.code(),seed.type(),seed.unit(),seed.visibility(),SCHEMA,seed.legal()));
   entity.applyCatalog(seed.no(),seed.category(),seed.subCategory(),seed.nameEn(),seed.nameZh(),seed.granularity(),seed.mandatory(),seed.staticDynamic(),seed.target(),seed.implementation(),seed.type(),seed.unit(),seed.visibility(),seed.legal(),seed.definition(),seed.exampleValue(),seed.requirementNotes());
   definitions.save(entity);
  }
  // Preserve prototype-specific fields that are not part of the 92-attribute legal catalog.
  for(String field:List.of("revision_code","market_code","effective_from","rated_capacity_kwh","batch_code","device_id","actor_org_code","reason_code","evidence_ref","event_type","event_time","source_system","source_record_id")){
   if(seeds.stream().anyMatch(x->x.code().equals(field)))continue;
   FieldDefinitionEntity existing=definitions.findByOrgCodeAndFieldCodeAndSchemaVersion(org,field,SCHEMA).orElse(null);
   if(existing==null){
    VisibilityClass visibility=List.of("actor_org_code","reason_code","evidence_ref","event_type","event_time").contains(field)?VisibilityClass.REGULATOR:List.of("source_system","source_record_id").contains(field)?VisibilityClass.INTERNAL:VisibilityClass.PUBLIC;
    String type=field.equals("rated_capacity_kwh")||field.equals("rated_voltage_v")?"DECIMAL":"STRING";
    String unit=field.equals("rated_capacity_kwh")?"kWh":field.equals("rated_voltage_v")?"V":null;
    FieldDefinitionEntity entity=new FieldDefinitionEntity(org,field,type,unit,visibility,SCHEMA,"Prototype extension");
    entity.applyExampleValue(PROTOTYPE_EXAMPLES.get(field));
    definitions.save(entity);
   }else if(existing.getExampleValue()==null){
    existing.applyExampleValue(PROTOTYPE_EXAMPLES.get(field));
    definitions.save(existing);
   }
  }
  // Retire legacy aliases now represented by canonical DPP entries.
  for(String legacy:List.of("sn","model_code","manufacturer_name","battery_category","chemistry","manufactured_at","rated_voltage_v")){
   String canonical=switch(legacy){case "sn"->"battery_serial_number";case "model_code"->"battery_model_identifier";case "manufacturer_name"->"manufacturer_information";case "battery_category"->"battery_category";case "chemistry"->"materials_battery_chemistry";case "manufactured_at"->"manufacturing_date";case "rated_voltage_v"->"performance_nominal_voltage";default->legacy;};
   definitions.findByOrgCodeAndFieldCodeAndSchemaVersion(org,legacy,SCHEMA).filter(x->!legacy.equals(canonical)).ifPresent(x->{x.markSuperseded("{\"supersededBy\":\""+canonical+"\"}");definitions.save(x);});
  }
  return definitions.findByOrgCodeAndSchemaVersionAndStatusOrderByFieldCode(org,SCHEMA,"ACTIVE");
 }
 @Transactional public FieldDefinitionEntity updateVisibility(String org,String id,VisibilityClass visibility){
  FieldDefinitionEntity entity=definitions.findById(id).filter(x->x.getOrgCode().equals(org)).orElseThrow(()->com.tuya.passport.cloud.common.support.ApiException.notFound("GOV_NOT_FOUND","Field definition not found"));
  entity.updateVisibility(visibility);
  return definitions.save(entity);
 }
 public List<CatalogSeed> catalog(){List<CatalogSeed> current=catalog;if(current!=null)return current;synchronized(this){if(catalog!=null)return catalog;try(var reader=new InputStreamReader(new ClassPathResource("dpp/dpp-attributes-v2.0.csv").getInputStream(),StandardCharsets.UTF_8);CSVParser parser=CSVFormat.DEFAULT.builder().setHeader("dpp_no","category","sub_category","field_code","display_name_en","display_name_zh","value_type","unit_code","visibility_class","granularity","mandatory_stationary","static_dynamic","storage_target","implementation_status","legal_reference","short_definition","example_value","requirement_notes").setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build().parse(reader)){List<CatalogSeed> out=new ArrayList<>();for(var r:parser){int no=Integer.parseInt(r.get("dpp_no").replace("﻿","").trim());out.add(new CatalogSeed(no,r.get("field_code"),r.get("category"),r.get("sub_category"),r.get("display_name_en"),r.get("display_name_zh"),r.get("value_type"),blankNull(r.get("unit_code")),VisibilityClass.valueOf(r.get("visibility_class")),r.get("granularity"),Boolean.parseBoolean(r.get("mandatory_stationary")),r.get("static_dynamic"),r.get("storage_target"),r.get("implementation_status"),r.get("legal_reference"),r.get("short_definition"),blankNull(r.get("example_value")),blankNull(r.get("requirement_notes"))));}if(out.size()!=92)throw new IllegalStateException("Expected 92 DPP catalog rows, got "+out.size());catalog=List.copyOf(out);return catalog;}catch(Exception ex){throw new IllegalStateException("Unable to load DPP attribute catalog",ex);}}}
 private static String blankNull(String x){return x==null||x.isBlank()?null:x;}
 public record CatalogSeed(int no,String code,String category,String subCategory,String nameEn,String nameZh,String type,String unit,VisibilityClass visibility,String granularity,boolean mandatory,String staticDynamic,String target,String implementation,String legal,String definition,String exampleValue,String requirementNotes){}
}
