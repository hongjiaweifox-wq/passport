package com.tuya.passport.cloud.l5.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.common.support.Hashing;
import com.tuya.passport.cloud.l3.domain.BatteryUnitEntity;
import com.tuya.passport.cloud.l3.repository.BatteryUnitRepository;
import com.tuya.passport.cloud.l3.service.SubjectSnapshotService;
import com.tuya.passport.cloud.l4.domain.CandidateStatus;
import com.tuya.passport.cloud.l4.domain.PublicationCandidateEntity;
import com.tuya.passport.cloud.l4.repository.PublicationCandidateRepository;
import com.tuya.passport.cloud.l4.service.CandidateStatusService;
import com.tuya.passport.cloud.l5.domain.PassportEntity;
import com.tuya.passport.cloud.l5.domain.PassportStatus;
import com.tuya.passport.cloud.l5.domain.PassportVersionEntity;
import com.tuya.passport.cloud.l5.repository.PassportRepository;
import com.tuya.passport.cloud.l5.repository.PassportVersionRepository;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class PassportService {
 private final PassportRepository passports;private final PassportVersionRepository versions;private final BatteryUnitRepository units;private final PublicationCandidateRepository candidates;private final SubjectSnapshotService snapshots;private final Hashing hashing;private final ObjectMapper mapper;private final CandidateStatusService candidateStatuses;private final SecureRandom random=new SecureRandom();
 public PassportService(PassportRepository passports,PassportVersionRepository versions,BatteryUnitRepository units,PublicationCandidateRepository candidates,SubjectSnapshotService snapshots,Hashing hashing,ObjectMapper mapper,CandidateStatusService candidateStatuses){this.passports=passports;this.versions=versions;this.units=units;this.candidates=candidates;this.snapshots=snapshots;this.hashing=hashing;this.mapper=mapper;this.candidateStatuses=candidateStatuses;}
 @Transactional public PassportEntity create(String org,String unitId){BatteryUnitEntity unit=units.findByOrgCodeAndId(org,unitId).orElseThrow(()->ApiException.notFound("L5_NOT_FOUND","Battery unit not found"));return passports.findByOrgCodeAndBatteryUnitId(org,unitId).orElseGet(()->passports.save(new PassportEntity(org,unitId,unit.getIssuerCode(),unit.getIssuerCode()+":"+unit.getSnNormalized(),newPublicCode())));}
 @Transactional public PublishResult publish(String org,String actor,String passportId,String key){PassportEntity passport=passports.findByOrgCodeAndId(org,passportId).orElseThrow(()->ApiException.notFound("L5_NOT_FOUND","Passport not found"));if(key!=null&&!key.isBlank()){var prior=versions.findByOrgCodeAndPassportIdAndIdempotencyKey(org,passportId,key).orElse(null);if(prior!=null){String currentHash=snapshots.currentHash(org,"BATTERY_UNIT",passport.getBatteryUnitId());if(!prior.getSourceSnapshotHash().equals(currentHash))throw ApiException.conflict("L5_PUBLISH_CONFLICT","Idempotency key was used for another snapshot");return new PublishResult(passport,prior,true);}}if(passport.getStatus()==PassportStatus.SUSPENDED||passport.getStatus()==PassportStatus.ARCHIVED)throw ApiException.conflict("L5_PASSPORT_UNAVAILABLE","Passport cannot be published in its current state");PublicationCandidateEntity candidate=candidates.findFirstByOrgCodeAndSubjectTypeAndSubjectIdAndStatusOrderByCreatedAtDesc(org,"BATTERY_UNIT",passport.getBatteryUnitId(),CandidateStatus.READY).orElseThrow(()->ApiException.conflict("L5_CANDIDATE_NOT_READY","A ready L4 publication candidate is required"));String currentHash=snapshots.currentHash(org,candidate.getSubjectType(),candidate.getSubjectId());if(!currentHash.equals(candidate.getSnapshotHash())){candidateStatuses.markStale(org,candidate.getId());throw ApiException.conflict("L5_CANDIDATE_STALE","The L3 subject changed after it was approved");}
  PassportVersionEntity current=passport.getCurrentVersion()==0?null:versions.findByOrgCodeAndPassportIdAndVersionNo(org,passportId,passport.getCurrentVersion()).orElse(null);if(current!=null&&current.getSourceSnapshotHash().equals(candidate.getSnapshotHash()))return new PublishResult(passport,current,true);
  Map<String,Object> publicData=publicProjection(candidate.getPublicJson(),passport);String publicJson=hashing.json(publicData);String publicHash=hashing.sha256(publicJson);PassportVersionEntity version=versions.save(new PassportVersionEntity(org,passportId,passport.getCurrentVersion()+1,"dpp-schema/1.0",candidate.getSnapshotHash(),publicJson,candidate.getRestrictedJson(),candidate.getRegulatorJson(),publicHash,key==null||key.isBlank()?null:key,actor));passport.publish(version.getVersionNo());candidate.consume();return new PublishResult(passports.save(passport),version,false);
 }
 @Transactional public PassportEntity suspend(String org,String id,String reason){if(reason==null||reason.isBlank())throw ApiException.badRequest("L5_VALIDATION_ERROR","Suspension reason is required");PassportEntity p=passports.findByOrgCodeAndId(org,id).orElseThrow(()->ApiException.notFound("L5_NOT_FOUND","Passport not found"));p.suspend(reason);return passports.save(p);}
 @Transactional public PassportEntity resume(String org,String id){PassportEntity p=passports.findByOrgCodeAndId(org,id).orElseThrow(()->ApiException.notFound("L5_NOT_FOUND","Passport not found"));p.resume();return passports.save(p);}
 @Transactional(readOnly=true) public PublicResult resolve(String code){PassportEntity p=passports.findByPublicCode(code).orElseThrow(()->ApiException.notFound("L5_NOT_FOUND","Passport not found"));if(p.getStatus()==PassportStatus.SUSPENDED||p.getStatus()==PassportStatus.ARCHIVED)throw new ApiException("L5_PASSPORT_UNAVAILABLE","Passport is not publicly available",HttpStatus.GONE);if(p.getStatus()!=PassportStatus.PUBLISHED||p.getCurrentVersion()==0)throw ApiException.notFound("L5_NOT_FOUND","Passport not found");PassportVersionEntity v=versions.findByOrgCodeAndPassportIdAndVersionNo(p.getOrgCode(),p.getId(),p.getCurrentVersion()).orElseThrow(()->ApiException.notFound("L5_NOT_FOUND","Passport version not found"));try{@SuppressWarnings("unchecked") Map<String,Object> data=mapper.readValue(v.getPublicJson(),Map.class);return new PublicResult(v.getSchemaVersion(),p.getStatus().name(),v.getVersionNo(),v.getCreatedAt().toString(),data,"\"sha256:"+v.getPublicHash()+"\"");}catch(Exception ex){throw new IllegalStateException(ex);}}
 private Map<String,Object> publicProjection(String json,PassportEntity passport){try{@SuppressWarnings("unchecked") Map<String,Object> data=mapper.readValue(json,Map.class);data.putIfAbsent("dpp_schema_version","dpp-schema/1.0");data.putIfAbsent("dpp_status","PUBLISHED");data.putIfAbsent("dpp_granularity","Individual battery");data.putIfAbsent("dpp_last_updated",java.time.Instant.now().toString());data.put("dpp_identifier",passport.getPublicCode());data.put("unique_battery_identifier",passport.getUniqueIdentifier());return data;}catch(Exception ex){throw new IllegalStateException("Unable to enrich DPP header",ex);}}
 private String newPublicCode(){byte[] bytes=new byte[16];random.nextBytes(bytes);return HexFormat.of().formatHex(bytes);}
 public record PublishResult(PassportEntity passport,PassportVersionEntity version,boolean reused){}
 public record PublicResult(String schemaVersion,String passportStatus,int passportVersion,String updatedAt,Map<String,Object> data,String etag){}
}
