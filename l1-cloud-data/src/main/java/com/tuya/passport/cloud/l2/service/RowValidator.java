package com.tuya.passport.cloud.l2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuya.passport.cloud.l2.domain.EntityType;
import com.tuya.passport.cloud.l2.domain.ImportRowEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowErrorEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowStatus;
import com.tuya.passport.cloud.l3.support.TuyaSnDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class RowValidator {
    private final ObjectMapper mapper;
    private final DppAttributeCatalog catalog;
    public RowValidator(CanonicalRecordFactory canonicalRecordFactory, DppAttributeCatalog catalog) { this.mapper = canonicalRecordFactory.mapper(); this.catalog = catalog; }
    public ValidatedRow validate(String org, String sourceCode, EntityType type, String schemaVersion, int rowNo, CSVRecord record) {
        Map<String,String> cells = new LinkedHashMap<>();
        List<String> headers = type == null ? List.of() : java.util.Arrays.asList(type.headers());
        for (int i=0;i<headers.size() && i<record.size();i++) cells.put(headers.get(i), record.get(i));
        String recordId = value(cells,"source_record_id");
        List<FieldError> issues = new ArrayList<>();
        check(cells,"schema_version",schemaVersion,issues); checkIgnoreCase(cells,"source_system",sourceCode,issues); checkIgnoreCase(cells,"org_code",org,issues);
        required(cells,"source_record_id",issues); max(cells,"source_record_id",128,issues);
        for (var entry : cells.entrySet()) if (entry.getValue() != null && entry.getValue().length() > 2000 && !"dpp_attributes".equals(entry.getKey())) issues.add(err(entry.getKey(),"INGEST_FIELD_TOO_LONG","字段长度超过 2000",entry.getValue()));
        switch(type) {
            case BATTERY_MODEL -> {
                for (String key : List.of("model_code","revision_code","manufacturer_name","chemistry","market_code","effective_from")) required(cells,key,issues);
                enumValue(cells,"battery_category",List.of("PORTABLE","LMT","EV","INDUSTRIAL","STATIONARY","SLI"),issues);
                String productCode=value(cells,"sn_product_code"); if(!productCode.isBlank()&&!productCode.matches("[A-R]")) issues.add(err("sn_product_code","INGEST_INVALID_ENUM","SN 产品型号代码必须是 A-R 单个字符",productCode));
                decimal(cells,"rated_capacity_kwh",true,issues); decimal(cells,"rated_voltage_v",false,issues);
                validateDppAttributes(cells,issues);
                if (!value(cells,"effective_from").isBlank()) try { LocalDate.parse(value(cells,"effective_from")); } catch (Exception ex) { issues.add(err("effective_from","INGEST_INVALID_DATE","日期必须是 ISO 格式 YYYY-MM-DD",value(cells,"effective_from"))); }
            }
            case BATTERY_UNIT -> {
                required(cells,"sn",issues);
                if(value(cells,"model_code").isBlank()&&value(cells,"revision_code").isBlank()&&TuyaSnDecoder.decode(value(cells,"sn")).isEmpty()) required(cells,"model_code",issues);
                String date = value(cells,"manufactured_at"); if (!date.isBlank()) try { OffsetDateTime.parse(date); } catch (Exception ex) { issues.add(err("manufactured_at","INGEST_INVALID_DATETIME","时间必须为 ISO-8601 UTC 时间",date)); }
                validateDppAttributes(cells,issues);
            }
            case LIFECYCLE_EVENT -> {
                for (String key : List.of("sn","event_type","event_time","actor_org_code")) required(cells,key,issues);
                enumValue(cells,"event_type",List.of("COMMISSIONED","REPAIRED","REPLACED","REPURPOSED","REMANUFACTURED","RECYCLED"),issues);
                String time = value(cells,"event_time"); if (!time.isBlank()) try { OffsetDateTime.parse(time); } catch (Exception ex) { issues.add(err("event_time","INGEST_INVALID_DATETIME","时间必须为 ISO-8601 UTC 时间",time)); }
            }
        }
        try {
            Map<String,Object> data = new LinkedHashMap<>();
            for (String header : headers.subList(4, Math.min(headers.size(), record.size()))) data.put(header, value(cells,header));
            Map<String,Object> canonical = new LinkedHashMap<>(); canonical.put("entityType",type.name()); canonical.put("schemaVersion",schemaVersion);
            canonical.put("sourceSystem",sourceCode); canonical.put("sourceRecordId",recordId); canonical.put("orgCode",org); canonical.put("data",data);
            String json = mapper.writeValueAsString(canonical); String hash = sha256(json);
            return new ValidatedRow(rowNo, recordId, hash, json, issues);
        } catch (Exception ex) { throw new IllegalStateException("Unable to canonicalize row",ex); }
    }
    private void validateDppAttributes(Map<String,String> cells,List<FieldError> issues) {
        String raw=value(cells,"dpp_attributes"); if(raw.isBlank())return;
        if(raw.length()>20000){issues.add(err("dpp_attributes","INGEST_ATTRIBUTE_TOO_LONG","DPP 扩展属性不能超过 20000 字符",raw));return;}
        try {
            com.fasterxml.jackson.core.JsonFactory factory=com.fasterxml.jackson.core.JsonFactory.builder().enable(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
            com.fasterxml.jackson.databind.ObjectMapper strict=new com.fasterxml.jackson.databind.ObjectMapper(factory);
            com.fasterxml.jackson.databind.JsonNode node=strict.readTree(raw);
            if(node==null||!node.isObject()){issues.add(err("dpp_attributes","INGEST_INVALID_ATTRIBUTE_JSON","DPP 扩展属性必须是 JSON 对象",raw));return;}
            var it=node.fields();while(it.hasNext()){var e=it.next();String key=e.getKey();if(!key.matches("[a-z][a-z0-9_]{0,95}")){issues.add(err("dpp_attributes","INGEST_INVALID_ATTRIBUTE_KEY","DPP 属性编码必须为 snake_case",key));continue;}if(catalog.isReserved(key)){issues.add(err("dpp_attributes","INGEST_RESERVED_ATTRIBUTE","该属性已有专用字段或由系统生成",key));continue;}if(!catalog.contains(key))issues.add(err("dpp_attributes","INGEST_UNKNOWN_ATTRIBUTE","扩展属性编码不在 DPP 字段字典中",key));}
        }catch(com.fasterxml.jackson.core.JsonProcessingException ex){issues.add(err("dpp_attributes","INGEST_INVALID_ATTRIBUTE_JSON","DPP 扩展属性 JSON 格式无效",raw));}
    }
    private static void required(Map<String,String> cells,String field,List<FieldError> errors) { if (value(cells,field).isBlank()) errors.add(err(field,"INGEST_REQUIRED_FIELD","必填字段不能为空",value(cells,field))); }
    private static void max(Map<String,String> cells,String field,int limit,List<FieldError> errors) { if (value(cells,field).length()>limit) errors.add(err(field,"INGEST_FIELD_TOO_LONG","字段长度超过 "+limit,value(cells,field))); }
    private static void check(Map<String,String> cells,String field,String expected,List<FieldError> errors) { if (!value(cells,field).equals(expected)) errors.add(err(field,"INGEST_VALUE_MISMATCH","字段值与任务元数据不一致",value(cells,field))); }
    private static void checkIgnoreCase(Map<String,String> cells,String field,String expected,List<FieldError> errors) { if (!value(cells,field).equalsIgnoreCase(expected)) errors.add(err(field,"INGEST_VALUE_MISMATCH","字段值与任务元数据不一致",value(cells,field))); }
    private static void enumValue(Map<String,String> cells,String field,List<String> options,List<FieldError> errors) { String v=value(cells,field); if(!v.isBlank()&&!options.contains(v.toUpperCase(Locale.ROOT))) errors.add(err(field,"INGEST_INVALID_ENUM","字段值不属于允许的枚举",v)); }
    private static void decimal(Map<String,String> cells,String field,boolean required,List<FieldError> errors) { String v=value(cells,field); if(v.isBlank()){if(required) required(cells,field,errors);return;} try { if(new java.math.BigDecimal(v).signum()<=0) throw new NumberFormatException(); } catch(Exception ex) { errors.add(err(field,"INGEST_INVALID_DECIMAL","字段必须是大于 0 的数字",v)); } }
    private static FieldError err(String field,String code,String message,String raw) { return new FieldError(field,code,message,sha256(raw)); }
    private static String value(Map<String,String> cells,String key) { String v=cells.get(key); return v==null?"":v; }
    private static String sha256(String text) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); } catch(Exception ex) { throw new IllegalStateException(ex); } }
    public record FieldError(String fieldCode,String errorCode,String safeMessage,String rawValueHash) {}
    public record ValidatedRow(int rowNo,String sourceRecordId,String recordHash,String canonicalJson,List<FieldError> errors) { public boolean valid(){return errors.isEmpty();} }
}
