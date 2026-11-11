package com.tuya.passport.cloud.l3.api;

import com.tuya.passport.cloud.common.api.ApiEnvelope;
import com.tuya.passport.cloud.common.api.PageResponse;
import com.tuya.passport.cloud.common.support.ActorContext;
import com.tuya.passport.cloud.l3.domain.*;
import com.tuya.passport.cloud.l3.repository.*;
import com.tuya.passport.cloud.l3.service.ImportPromotionService;
import com.tuya.passport.cloud.l3.service.SubjectSnapshotService;
import com.tuya.passport.cloud.l3.support.TuyaSnDecoder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/l3")
public class L3Controller {
 private final ActorContext actors; private final BatteryModelRepository models; private final ModelRevisionRepository revisions; private final BatteryUnitRepository units; private final DeviceBindingRepository bindings; private final LifecycleEventRepository events; private final ImportPromotionService promotions;
 public L3Controller(ActorContext actors,BatteryModelRepository models,ModelRevisionRepository revisions,BatteryUnitRepository units,DeviceBindingRepository bindings,LifecycleEventRepository events,ImportPromotionService promotions){this.actors=actors;this.models=models;this.revisions=revisions;this.units=units;this.bindings=bindings;this.events=events;this.promotions=promotions;}
 @GetMapping("/summary") public ApiEnvelope<Map<String,Long>> summary(@RequestHeader(value="X-Org-Code",required=false)String org,HttpServletRequest req){var a=actors.actor(org,null,null);return env(Map.of("models",models.countByOrgCode(a.orgCode()),"revisions",revisions.countByOrgCode(a.orgCode()),"batteryUnits",units.countByOrgCode(a.orgCode()),"bindings",bindings.countByOrgCode(a.orgCode()),"events",events.countByOrgCode(a.orgCode())),req);}
 @GetMapping("/models") public ApiEnvelope<PageResponse<ModelView>> modelList(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestParam(required=false)String q,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,HttpServletRequest req){var a=actors.actor(org,null,null);var pg=models.search(a.orgCode(),q,PageRequest.of(Math.max(0,page-1),Math.min(size,100)));return env(PageResponse.from(pg.map(m->new ModelView(m.getId(),m.getModelCode(),m.getManufacturerName(),m.getBatteryCategory(),m.getSnProductCode(),revisions.findByOrgCodeAndModelIdOrderByCreatedAtDesc(a.orgCode(),m.getId()).size()))),req);}
 @GetMapping("/models/{id}") public ApiEnvelope<ModelDetail> model(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,HttpServletRequest req){var a=actors.actor(org,null,null);var m=models.findByOrgCodeAndId(a.orgCode(),id).orElseThrow(()->com.tuya.passport.cloud.common.support.ApiException.notFound("L3_NOT_FOUND","Battery model not found"));var rv=revisions.findByOrgCodeAndModelIdOrderByCreatedAtDesc(a.orgCode(),id);return env(new ModelDetail(new ModelView(m.getId(),m.getModelCode(),m.getManufacturerName(),m.getBatteryCategory(),m.getSnProductCode(),rv.size()),rv.stream().map(r->new RevisionView(r.getId(),r.getRevisionCode(),r.getRatedCapacityKwh(),r.getRatedVoltageV(),r.getChemistryCode(),r.getEffectiveFrom(),r.getStatus().name())).toList()),req);}
 @GetMapping("/battery-units") public ApiEnvelope<PageResponse<UnitView>> unitList(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestParam(required=false)String q,@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="10")int size,HttpServletRequest req){var a=actors.actor(org,null,null);var pg=status==null||status.isBlank()?units.findByOrgCode(a.orgCode(),PageRequest.of(Math.max(0,page-1),Math.min(size,100),Sort.by(Sort.Direction.DESC,"createdAt"))):units.findByOrgCodeAndStatus(a.orgCode(),BatteryUnitStatus.valueOf(status),PageRequest.of(Math.max(0,page-1),Math.min(size,100),Sort.by(Sort.Direction.DESC,"createdAt")));return env(PageResponse.from(pg.map(u->unitView(a.orgCode(),u))),req);}
 @GetMapping("/battery-units/{id}") public ApiEnvelope<UnitDetail> unit(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String id,HttpServletRequest req){var a=actors.actor(org,null,null);var u=units.findByOrgCodeAndId(a.orgCode(),id).orElseThrow(()->com.tuya.passport.cloud.common.support.ApiException.notFound("L3_NOT_FOUND","Battery unit not found"));var revision=revisions.findByOrgCodeAndId(a.orgCode(),u.getModelRevisionId()).orElseThrow(()->com.tuya.passport.cloud.common.support.ApiException.notFound("L3_NOT_FOUND","Revision not found"));return env(new UnitDetail(unitView(a.orgCode(),u),revision.getRevisionCode(),bindings.findByOrgCodeAndBatteryUnitIdOrderByBoundAtDesc(a.orgCode(),id),events.findByOrgCodeAndBatteryUnitIdOrderByEventTimeDesc(a.orgCode(),id)),req);}
 @PostMapping("/promotions/import-jobs/{jobId}") @ResponseStatus(HttpStatus.OK) public ApiEnvelope<ImportPromotionService.PromotionResult> promote(@RequestHeader(value="X-Org-Code",required=false)String org,@RequestHeader(value="X-User",required=false)String user,@RequestHeader(value="X-Role",required=false)String role,@PathVariable String jobId,HttpServletRequest req){var a=actors.actor(org,user,role);actors.requireAnyRole(a,"DATA_EDITOR","COMPLIANCE_MAKER","ADMIN");return env(promotions.promote(a.orgCode(),a.userId(),jobId),req);}
 @GetMapping("/sn-rule") public ApiEnvelope<TuyaSnDecoder.SnRule> snRule(@RequestHeader(value="X-Org-Code",required=false)String org,HttpServletRequest req){actors.actor(org,null,null);return env(TuyaSnDecoder.rule(),req);}
 @GetMapping("/promotions/import-jobs/{jobId}") public ApiEnvelope<ImportPromotionService.PromotionResult> promotion(@RequestHeader(value="X-Org-Code",required=false)String org,@PathVariable String jobId,HttpServletRequest req){var a=actors.actor(org,null,null);return env(promotions.latest(a.orgCode(),jobId),req);}
 private UnitView unitView(String org,BatteryUnitEntity u){var rev=revisions.findByOrgCodeAndId(org,u.getModelRevisionId()).orElse(null);var model=rev==null?null:models.findByOrgCodeAndId(org,rev.getModelId()).orElse(null);return new UnitView(u.getId(),u.getSnRaw(),u.getSnNormalized(),model==null?"":model.getModelCode(),rev==null?"":rev.getRevisionCode(),u.getStatus().name(),u.getManufacturedAt(),u.getCreatedAt());}
 private static <T>ApiEnvelope<T> env(T value,HttpServletRequest req){Object trace=req.getAttribute("traceId");return new ApiEnvelope<>(value,trace==null?"":trace.toString());}
 public record ModelView(String id,String modelCode,String manufacturerName,String batteryCategory,String snProductCode,int revisionCount){}
 public record RevisionView(String id,String revisionCode,java.math.BigDecimal ratedCapacityKwh,java.math.BigDecimal ratedVoltageV,String chemistry,java.time.LocalDate effectiveFrom,String status){}
 public record ModelDetail(ModelView model,List<RevisionView> revisions){}
 public record UnitView(String id,String snRaw,String snNormalized,String modelCode,String revisionCode,String status,java.time.OffsetDateTime manufacturedAt,java.time.Instant createdAt){}
 public record UnitDetail(UnitView unit,String revisionCode,List<DeviceBindingEntity> bindings,List<LifecycleEventEntity> events){}
}
