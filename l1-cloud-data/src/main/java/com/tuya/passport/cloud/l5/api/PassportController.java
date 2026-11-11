package com.tuya.passport.cloud.l5.api;

import com.tuya.passport.cloud.common.api.ApiEnvelope;
import com.tuya.passport.cloud.common.api.PageResponse;
import com.tuya.passport.cloud.common.support.ActorContext;
import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.l5.domain.PassportEntity;
import com.tuya.passport.cloud.l5.domain.PassportStatus;
import com.tuya.passport.cloud.l5.domain.PassportVersionEntity;
import com.tuya.passport.cloud.l5.repository.PassportRepository;
import com.tuya.passport.cloud.l5.repository.PassportVersionRepository;
import com.tuya.passport.cloud.l5.service.PassportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PassportController {
 private final ActorContext actors;private final PassportService service;private final PassportRepository passports;private final PassportVersionRepository versions;
 public PassportController(ActorContext actors,PassportService service,PassportRepository passports,PassportVersionRepository versions){this.actors=actors;this.service=service;this.passports=passports;this.versions=versions;}
 @GetMapping("/api/v1/l5/summary") public ApiEnvelope<Map<String,Long>> summary(@RequestHeader(value="X-Org-Code",required=false)String org,HttpServletRequest req){var a=actors.actor(org,null,null);return env(Map.of("total",passports.countByOrgCode(a.orgCode()),"draft",passports.countByOrgCodeAndStatus(a.orgCode(),PassportStatus.DRAFT),"published",passports.countByOrgCodeAndStatus(a.orgCode(),PassportStatus.PUBLISHED),"suspended",passports.countByOrgCodeAndStatus(a.orgCode(),PassportStatus.SUSPENDED)),req);}
 @GetMapping("/api/v1/l5/passports") public ApiEnvelope<PageResponse<PassportEntity>> list(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,HttpServletRequest req){var a=actors.actor(org,null,null);var request=PageRequest.of(Math.max(0,page-1),Math.min(size,100),Sort.by(Sort.Direction.DESC,"updatedAt"));return env(PageResponse.from(status==null||status.isBlank()?passports.findByOrgCode(a.orgCode(),request):passports.findByOrgCodeAndStatus(a.orgCode(),PassportStatus.valueOf(status),request)),req);}
 @PostMapping("/api/v1/l5/passports") @ResponseStatus(HttpStatus.CREATED) public ApiEnvelope<PassportEntity> create(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@Valid @RequestBody CreatePassport body,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"PASSPORT_OPERATOR","ADMIN");return env(service.create(a.orgCode(),body.batteryUnitId()),req);}
 @GetMapping("/api/v1/l5/passports/{id}") public ApiEnvelope<PassportDetail> detail(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,HttpServletRequest req){var a=actors.actor(org,null,null);PassportEntity p=passports.findByOrgCodeAndId(a.orgCode(),id).orElseThrow(()->ApiException.notFound("L5_NOT_FOUND","Passport not found"));PassportVersionEntity v=p.getCurrentVersion()==0?null:versions.findByOrgCodeAndPassportIdAndVersionNo(a.orgCode(),id,p.getCurrentVersion()).orElse(null);return env(new PassportDetail(p,v),req);}
 @GetMapping("/api/v1/l5/passports/{id}/versions") public ApiEnvelope<List<PassportVersionEntity>> versionList(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,HttpServletRequest req){var a=actors.actor(org,null,null);if(passports.findByOrgCodeAndId(a.orgCode(),id).isEmpty())throw ApiException.notFound("L5_NOT_FOUND","Passport not found");return env(versions.findByOrgCodeAndPassportIdOrderByVersionNoDesc(a.orgCode(),id),req);}
 @PostMapping("/api/v1/l5/passports/{id}:publish") public ApiEnvelope<PassportService.PublishResult> publish(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@RequestHeader(value="Idempotency-Key",required=false)String key,@PathVariable String id,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"PASSPORT_OPERATOR","ADMIN");return env(service.publish(a.orgCode(),a.userId(),id,key),req);}
 @PostMapping("/api/v1/l5/passports/{id}:suspend") public ApiEnvelope<PassportEntity> suspend(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@PathVariable String id,@Valid @RequestBody Reason body,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"PASSPORT_OPERATOR","ADMIN");return env(service.suspend(a.orgCode(),id,body.reason()),req);}
 @PostMapping("/api/v1/l5/passports/{id}:resume") public ApiEnvelope<PassportEntity> resume(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@PathVariable String id,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"PASSPORT_OPERATOR","ADMIN");return env(service.resume(a.orgCode(),id),req);}
 @GetMapping("/api/v1/public/passports/{code}") public ResponseEntity<?> publicPassport(@PathVariable String code,@RequestHeader(value=HttpHeaders.IF_NONE_MATCH,required=false)String ifNoneMatch){PassportService.PublicResult result=service.resolve(code);if(result.etag().equals(ifNoneMatch))return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(result.etag()).header(HttpHeaders.CACHE_CONTROL,"public, max-age=0, must-revalidate").build();return ResponseEntity.ok().eTag(result.etag()).header(HttpHeaders.CACHE_CONTROL,"public, max-age=0, must-revalidate").body(Map.of("data",new PublicBody(result.schemaVersion(),result.passportStatus(),result.passportVersion(),result.updatedAt(),result.data()),"traceId",""));}
 private static <T>ApiEnvelope<T> env(T data,HttpServletRequest req){Object trace=req.getAttribute("traceId");return new ApiEnvelope<>(data,trace==null?"":trace.toString());}
 public record CreatePassport(@NotBlank String batteryUnitId){}public record Reason(@NotBlank String reason){}public record PassportDetail(PassportEntity passport,PassportVersionEntity currentVersion){}public record PublicBody(String schemaVersion,String passportStatus,int passportVersion,String updatedAt,Map<String,Object> data){}
}
