package com.tuya.passport.cloud.l4.service;

import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.common.support.Hashing;
import com.tuya.passport.cloud.l3.service.SubjectSnapshotService;
import com.tuya.passport.cloud.l4.domain.ValidationIssueEntity;
import com.tuya.passport.cloud.l4.domain.ValidationRunEntity;
import com.tuya.passport.cloud.l4.domain.ValidationStatus;
import com.tuya.passport.cloud.l4.repository.ValidationIssueRepository;
import com.tuya.passport.cloud.l4.repository.ValidationRunRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class ValidationService {
 private final SubjectSnapshotService snapshots;private final Hashing hashing;private final ValidationRunRepository runs;private final ValidationIssueRepository issues;private final FieldCoverageService coverage;
 public ValidationService(SubjectSnapshotService snapshots,Hashing hashing,ValidationRunRepository runs,ValidationIssueRepository issues,FieldCoverageService coverage){this.snapshots=snapshots;this.hashing=hashing;this.runs=runs;this.issues=issues;this.coverage=coverage;}
 @Transactional public ValidationResult validate(String org,String subjectType,String subjectId){
  Map<String,Object> snapshot=snapshots.snapshot(org,subjectType,subjectId);Map<String,Object> fields=flat(snapshot);List<Issue> found=new ArrayList<>();FieldCoverageService.CoverageView coverageView=coverage.coverage(org,subjectType,subjectId);if(coverageView.coverage()!=null&&coverageView.mandatory()-coverageView.coverage().mandatoryFilled()>0)found.add(new Issue("dpp_attributes","INFO","GOV_DPP_COVERAGE","Missing mandatory DPP values: "+(coverageView.mandatory()-coverageView.coverage().mandatoryFilled())));
  List<String> required=new ArrayList<>(List.of("modelCode","revisionCode","manufacturerName","batteryCategory","chemistry","ratedCapacityKwh","effectiveFrom"));if("BATTERY_UNIT".equals(subjectType))required.add("snRaw");
  for(String field:required)if(blank(fields.get(field)))found.add(new Issue(field,"ERROR","GOV_REQUIRED_FIELD","Required field is missing"));
  Object capacity=fields.get("ratedCapacityKwh");if(capacity!=null&&!blank(capacity))try{if(new BigDecimal(capacity.toString()).signum()<=0)found.add(new Issue("ratedCapacityKwh","ERROR","GOV_INVALID_RANGE","Rated capacity must be greater than zero"));}catch(NumberFormatException ex){found.add(new Issue("ratedCapacityKwh","ERROR","GOV_INVALID_RANGE","Rated capacity must be numeric"));}
  if(blank(fields.get("marketCode")))found.add(new Issue("marketCode","WARN","GOV_MARKET_NOT_DECLARED","Target market is not declared"));
  if(blank(fields.get("ratedVoltageV")))found.add(new Issue("ratedVoltageV","WARN","GOV_VOLTAGE_NOT_DECLARED","Rated voltage is not declared"));
  if("INDUSTRIAL".equalsIgnoreCase(String.valueOf(fields.get("batteryCategory")))&&capacity!=null)try{if(new BigDecimal(capacity.toString()).compareTo(new BigDecimal("2.0000"))>=0)found.add(new Issue("batteryCategory","INFO","GOV_APPLICABILITY_INDUSTRIAL","Industrial battery applicability requires assessment"));}catch(Exception ignored){}
  int errors=(int)found.stream().filter(x->"ERROR".equals(x.severity())).count(),warnings=(int)found.stream().filter(x->"WARN".equals(x.severity())).count();ValidationStatus status=errors>0?ValidationStatus.FAIL:warnings>0?ValidationStatus.REVIEW_REQUIRED:ValidationStatus.PASS;
  String hash=hashing.hash(snapshot);ValidationRunEntity run=runs.save(new ValidationRunEntity(org,subjectType,subjectId,hash,status,errors,warnings));List<ValidationIssueEntity> stored=found.stream().map(x->new ValidationIssueEntity(org,run.getId(),x.fieldCode(),x.severity(),x.code(),x.message())).map(issues::save).toList();return new ValidationResult(run.getId(),subjectType,subjectId,hash,status.name(),errors,warnings,stored.stream().map(x->new Issue(x.getFieldCode(),x.getSeverity(),x.getCode(),x.getMessage())).toList(),run.getFinishedAt().toString());
 }
 public Map<String,Object> fields(Map<String,Object> snapshot){return flat(snapshot);}
 private static Map<String,Object> flat(Map<String,Object> snapshot){Map<String,Object> result=new LinkedHashMap<>();merge(result,snapshot.get("model"));merge(result,snapshot.get("revision"));merge(result,snapshot.get("unit"));if(result.containsKey("chemistryCode"))result.put("chemistry",result.get("chemistryCode"));if(result.containsKey("snRaw"))result.put("sn",result.get("snRaw"));Object attrs=result.get("attributeJson");if(attrs instanceof String json)try{Map<?,?> values=new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,Map.class);Object market=values.get("market_code");if(market!=null)result.put("marketCode",market);}catch(Exception ignored){}return result;}
 private static void merge(Map<String,Object> result,Object value){if(value instanceof Map<?,?> map)map.forEach((k,v)->result.put(String.valueOf(k),v));}
 private static boolean blank(Object value){return value==null||value.toString().isBlank()||"null".equalsIgnoreCase(value.toString());}
 public record Issue(String fieldCode,String severity,String code,String message){}
 public record ValidationResult(String id,String subjectType,String subjectId,String inputHash,String status,int errorCount,int warningCount,List<Issue> issues,String finishedAt){}
}
