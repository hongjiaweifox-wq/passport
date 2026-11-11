package com.tuya.passport.cloud.l3.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuya.passport.cloud.common.api.ApiEnvelope;
import com.tuya.passport.cloud.common.support.ActorContext;
import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.common.support.Hashing;
import com.tuya.passport.cloud.l3.domain.*;
import com.tuya.passport.cloud.l3.repository.*;
import com.tuya.passport.cloud.l3.service.SnAutoFillService;
import com.tuya.passport.cloud.l3.support.TuyaSnDecoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/l3")
public class L3WriteController {
    private static final String SCHEMA = "dpp-schema/1.0";
    private static final List<String> EVENT_TYPES = List.of("COMMISSIONED", "REPAIRED", "REPLACED", "REPURPOSED", "REMANUFACTURED", "RECYCLED");
    private final ActorContext actors;
    private final BatteryModelRepository models;
    private final ModelRevisionRepository revisions;
    private final BatteryUnitRepository units;
    private final BatchRepository batches;
    private final DeviceBindingRepository bindings;
    private final LifecycleEventRepository events;
    private final SnAutoFillService snAutoFill;
    private final Hashing hashing;
    private final ObjectMapper mapper;

    public L3WriteController(ActorContext actors, BatteryModelRepository models, ModelRevisionRepository revisions,
            BatteryUnitRepository units, BatchRepository batches, DeviceBindingRepository bindings, LifecycleEventRepository events,
            SnAutoFillService snAutoFill, Hashing hashing, ObjectMapper mapper) {
        this.actors = actors; this.models = models; this.revisions = revisions; this.units = units; this.batches = batches;
        this.bindings = bindings; this.events = events; this.snAutoFill = snAutoFill;
        this.hashing = hashing; this.mapper = mapper;
    }

    @PostMapping("/models")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<?> createModel(@RequestHeader(value="X-Org-Code",required=false) String org,
            @RequestHeader(value="X-User",required=false) String user, @RequestHeader(value="X-Role",required=false) String role,
            @Valid @RequestBody CreateModel body, HttpServletRequest req) {
        var actor = actors.actor(org, user, role); actors.requireAnyRole(actor, "DATA_EDITOR", "COMPLIANCE_MAKER", "ADMIN");
        validateAttributes(body.dppAttributes());
        if (body.snProductCode() != null && !body.snProductCode().isBlank() && !body.snProductCode().matches("[A-R]")) throw new ApiException("L3_INVALID_SN_PRODUCT_CODE", "SN 产品型号代码必须是 A-R 单个字符", HttpStatus.BAD_REQUEST);
        if (body.ratedCapacityKwh().signum() <= 0 || (body.ratedVoltageV() != null && body.ratedVoltageV().signum() <= 0)) throw new ApiException("L3_INVALID_RANGE", "额定容量与额定电压必须大于 0", HttpStatus.BAD_REQUEST);
        if (!List.of("PORTABLE", "LMT", "EV", "INDUSTRIAL", "STATIONARY", "SLI").contains(body.batteryCategory().trim().toUpperCase(java.util.Locale.ROOT))) throw new ApiException("L3_INVALID_BATTERY_CATEGORY", "不支持的电池类别", HttpStatus.BAD_REQUEST);
        BatteryModelEntity model = models.findByOrgCodeAndIssuerCodeAndModelCode(actor.orgCode(), actor.orgCode(), body.modelCode().trim()).orElseGet(() ->
                new BatteryModelEntity(actor.orgCode(), actor.orgCode(), body.modelCode().trim(), body.manufacturerName(), body.batteryCategory()));
        String modelId = model.getId();
        ModelRevisionEntity duplicate = revisions.findByOrgCodeAndModelIdAndRevisionCode(actor.orgCode(), modelId, body.revisionCode().trim()).orElse(null);
        if (duplicate != null) throw new ApiException("L3_REVISION_EXISTS", "该型号修订编码已存在", HttpStatus.CONFLICT);
        model.update(body.manufacturerName(), body.batteryCategory());
        if (body.snProductCode() != null && !body.snProductCode().isBlank()) {
            String productCode = body.snProductCode().trim().toUpperCase(java.util.Locale.ROOT);
            models.findByOrgCodeAndSnProductCode(actor.orgCode(), productCode).stream()
                    .filter(existing -> !existing.getId().equals(modelId))
                    .findFirst()
                    .ifPresent(existing -> { throw new ApiException("L3_SN_PRODUCT_CODE_EXISTS", "SN 产品型号代码已被其他型号使用", HttpStatus.CONFLICT); });
            model.setSnProductCode(productCode);
        }
        models.save(model);
        Map<String, Object> attrs = new LinkedHashMap<>(); if (body.dppAttributes() != null && !body.dppAttributes().isEmpty()) attrs.put("dpp", body.dppAttributes());
        String attributeJson = attrs.isEmpty() ? "{}" : hashing.json(attrs);
        String sourceHash = hashing.sha256(hashing.json(body));
        ModelRevisionEntity revision = revisions.save(new ModelRevisionEntity(actor.orgCode(), model.getId(), body.revisionCode().trim(), SCHEMA,
                body.ratedCapacityKwh(), body.ratedVoltageV(), body.chemistry(), attributeJson, "manual", sourceHash, body.effectiveFrom()));
        return env(Map.of("modelId", model.getId(), "revisionId", revision.getId()), req);
    }

    @PostMapping("/models/{id}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<?> appendRevision(@RequestHeader(value="X-Org-Code",required=false) String org,
            @RequestHeader(value="X-User",required=false) String user, @RequestHeader(value="X-Role",required=false) String role,
            @PathVariable String id, @Valid @RequestBody CreateModel body, HttpServletRequest req) {
        var actor = actors.actor(org, user, role); actors.requireAnyRole(actor, "DATA_EDITOR", "COMPLIANCE_MAKER", "ADMIN");
        validateAttributes(body.dppAttributes());
        if (body.ratedCapacityKwh().signum() <= 0 || (body.ratedVoltageV() != null && body.ratedVoltageV().signum() <= 0)) throw new ApiException("L3_INVALID_RANGE", "额定容量与额定电压必须大于 0", HttpStatus.BAD_REQUEST);
        BatteryModelEntity model = models.findByOrgCodeAndId(actor.orgCode(), id).orElseThrow(() -> ApiException.notFound("L3_NOT_FOUND", "Battery model not found"));
        if (revisions.findByOrgCodeAndModelIdAndRevisionCode(actor.orgCode(), model.getId(), body.revisionCode().trim()).isPresent()) throw new ApiException("L3_REVISION_EXISTS", "该型号修订编码已存在", HttpStatus.CONFLICT);
        Map<String, Object> attrs = new LinkedHashMap<>(); if (body.dppAttributes() != null && !body.dppAttributes().isEmpty()) attrs.put("dpp", body.dppAttributes());
        String attributeJson = attrs.isEmpty() ? "{}" : hashing.json(attrs);
        String sourceHash = hashing.sha256(hashing.json(body));
        ModelRevisionEntity revision = revisions.save(new ModelRevisionEntity(actor.orgCode(), model.getId(), body.revisionCode().trim(), SCHEMA,
                body.ratedCapacityKwh(), body.ratedVoltageV(), body.chemistry(), attributeJson, "manual", sourceHash, body.effectiveFrom()));
        return env(Map.of("revisionId", revision.getId()), req);
    }

    @PostMapping("/battery-units")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<?> createUnit(@RequestHeader(value="X-Org-Code",required=false) String org,
            @RequestHeader(value="X-User",required=false) String user, @RequestHeader(value="X-Role",required=false) String role,
            @Valid @RequestBody CreateUnit body, HttpServletRequest req) {
        var actor = actors.actor(org, user, role); actors.requireAnyRole(actor, "DATA_EDITOR", "COMPLIANCE_MAKER", "ADMIN");
        if (body.sn() == null || body.sn().isBlank()) throw new ApiException("L3_VALIDATION_ERROR", "序列号 SN 不能为空", HttpStatus.BAD_REQUEST);
        snAutoFill.validateAttributes(body.dppAttributes());
        String modelCode = body.modelCode();
        String revisionCode = body.revisionCode();
        if (body.modelRevisionId() != null && !body.modelRevisionId().isBlank()) {
            ModelRevisionEntity specified = revisions.findByOrgCodeAndId(actor.orgCode(), body.modelRevisionId()).orElseThrow(() -> ApiException.notFound("L3_MODEL_REVISION_NOT_FOUND", "Model revision not found"));
            revisionCode = specified.getRevisionCode();
            modelCode = models.findByOrgCodeAndId(actor.orgCode(), specified.getModelId()).map(BatteryModelEntity::getModelCode).orElseThrow(() -> ApiException.notFound("L3_NOT_FOUND", "Battery model not found"));
        }
        SnAutoFillService.Resolution resolved = snAutoFill.resolve(actor.orgCode(), body.sn(), modelCode, revisionCode, body.batchCode(), body.manufacturedAt());
        if (units.findByOrgCodeAndIssuerCodeAndSnNormalized(actor.orgCode(), actor.orgCode(), resolved.normalizedSn()).isPresent()) throw new ApiException("L3_SN_EXISTS", "该序列号已存在", HttpStatus.CONFLICT);
        String batchId = null;
        if (resolved.batchCode() != null && !resolved.batchCode().isBlank()) {
            String batchCode = resolved.batchCode();
            BatchEntity batch = batches.findByOrgCodeAndModelRevisionIdAndBatchCode(actor.orgCode(), resolved.revision().getId(), batchCode)
                    .orElseGet(() -> batches.save(new BatchEntity(actor.orgCode(), resolved.revision().getId(), batchCode, "{}")));
            batchId = batch.getId();
        }
        Map<String, Object> attributes = new LinkedHashMap<>(); if (body.dppAttributes() != null && !body.dppAttributes().isEmpty()) attributes.put("dpp", body.dppAttributes());
        if (!resolved.snDerived().isEmpty()) attributes.put("snDerived", resolved.snDerived());
        String attributeJson = attributes.isEmpty() ? null : hashing.json(attributes);
        String sourceId = UUID.randomUUID().toString();
        BatteryUnitEntity unit = units.save(new BatteryUnitEntity(actor.orgCode(), actor.orgCode(), body.sn().trim(), resolved.normalizedSn(),
                resolved.revision().getId(), batchId, resolved.manufacturedAt(), "MANUAL", sourceId, hashing.sha256(sourceId + resolved.normalizedSn()), attributeJson));
        if (body.deviceId() != null && !body.deviceId().isBlank()) bindings.save(new DeviceBindingEntity(actor.orgCode(), unit.getId(), body.deviceId().trim(), OffsetDateTime.now(), "Initial manual entry"));
        return env(new SnAutoFillService.UnitCreated(unit.getId(), resolved.snDerived(), resolved.autoFilled(), resolved.conflicts()), req);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<?> createEvent(@RequestHeader(value="X-Org-Code",required=false) String org,
            @RequestHeader(value="X-User",required=false) String user, @RequestHeader(value="X-Role",required=false) String role,
            @Valid @RequestBody CreateEvent body, HttpServletRequest req) {
        var actor = actors.actor(org, user, role); actors.requireAnyRole(actor, "DATA_EDITOR", "COMPLIANCE_MAKER", "ADMIN");
        if (body.eventType() == null || body.eventType().isBlank() || body.eventTime() == null || body.actorOrgCode() == null || body.actorOrgCode().isBlank()) throw new ApiException("L3_VALIDATION_ERROR", "事件类型、事件时间和执行方组织编码为必填字段", HttpStatus.BAD_REQUEST);
        if (!EVENT_TYPES.contains(body.eventType().trim().toUpperCase(java.util.Locale.ROOT))) throw new ApiException("L3_INVALID_EVENT_TYPE", "不支持的生命周期事件类型", HttpStatus.BAD_REQUEST);
        String id = snAutoFill.createEvent(actor.orgCode(), body.sn(), body.eventType().trim().toUpperCase(java.util.Locale.ROOT), body.eventTime(), body.actorOrgCode(), body.reasonCode(), body.evidenceRef());
        return env(Map.of("id", id), req);
    }

    @GetMapping("/sn-decode")
    public ApiEnvelope<Map<String, Object>> decodeSn(@RequestHeader(value="X-Org-Code",required=false) String org, @RequestParam String sn, HttpServletRequest req) {
        var actor = actors.actor(org, null, null);
        var decoded = TuyaSnDecoder.decode(sn).orElse(null);
        if (decoded == null) return env(Map.of("valid", false), req);
        Map<String, Object> result = mapper.convertValue(decoded, mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        result.put("valid", true);
        var matches = models.findByOrgCodeAndSnProductCode(actor.orgCode(), decoded.productCode());
        Map<String, Object> modelMatch = new LinkedHashMap<>();
        if (matches.isEmpty()) {
            modelMatch.put("status", "NO_MATCH");
        } else if (matches.size() > 1) {
            modelMatch.put("status", "AMBIGUOUS");
        } else {
            var model = matches.get(0);
            modelMatch.put("modelId", model.getId());
            modelMatch.put("modelCode", model.getModelCode());
            var revision = revisions.findByOrgCodeAndModelIdOrderByCreatedAtDesc(actor.orgCode(), model.getId()).stream()
                    .filter(r -> r.getStatus() == ModelRevisionStatus.ACTIVE).findFirst().orElse(null);
            if (revision == null) {
                modelMatch.put("status", "NO_ACTIVE_REVISION");
            } else {
                modelMatch.put("status", "MATCHED");
                modelMatch.put("revisionId", revision.getId());
                modelMatch.put("revisionCode", revision.getRevisionCode());
            }
        }
        result.put("modelMatch", modelMatch);
        return env(result, req);
    }

    @PostMapping("/battery-units/{id}/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<?> bind(@RequestHeader(value="X-Org-Code",required=false) String org, @RequestHeader(value="X-User",required=false) String user,
            @RequestHeader(value="X-Role",required=false) String role, @PathVariable String id, @Valid @RequestBody BindingRequest body, HttpServletRequest req) {
        var actor = actors.actor(org, user, role); actors.requireAnyRole(actor, "DATA_EDITOR", "COMPLIANCE_MAKER", "ADMIN");
        units.findByOrgCodeAndId(actor.orgCode(), id).orElseThrow(() -> ApiException.notFound("L3_NOT_FOUND", "Battery unit not found"));
        OffsetDateTime at = body.boundAt() == null ? OffsetDateTime.now() : body.boundAt();
        var current = bindings.findFirstByOrgCodeAndBatteryUnitIdAndUnboundAtIsNullOrderByBoundAtDesc(actor.orgCode(), id).orElse(null);
        if (current != null && current.getDeviceId().equals(body.deviceId())) return env(Map.of("id", current.getId(), "unchanged", true), req);
        if (current != null) current.close(at, body.changeReason());
        var created = bindings.save(new DeviceBindingEntity(actor.orgCode(), id, body.deviceId(), at, body.changeReason())); return env(Map.of("id", created.getId()), req);
    }

    @PostMapping("/battery-units/{id}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<?> appendEvent(@RequestHeader(value="X-Org-Code",required=false) String org, @RequestHeader(value="X-User",required=false) String user,
            @RequestHeader(value="X-Role",required=false) String role, @PathVariable String id, @Valid @RequestBody EventRequest body, HttpServletRequest req) {
        var actor = actors.actor(org, user, role); actors.requireAnyRole(actor, "DATA_EDITOR", "COMPLIANCE_MAKER", "ADMIN");
        units.findByOrgCodeAndId(actor.orgCode(), id).orElseThrow(() -> ApiException.notFound("L3_NOT_FOUND", "Battery unit not found"));
        if (!EVENT_TYPES.contains(body.eventType().trim().toUpperCase(java.util.Locale.ROOT))) throw new ApiException("L3_INVALID_EVENT_TYPE", "不支持的生命周期事件类型", HttpStatus.BAD_REQUEST);
        String record = UUID.randomUUID().toString(); var event = events.save(new LifecycleEventEntity(actor.orgCode(), id, body.eventType().trim().toUpperCase(java.util.Locale.ROOT), body.eventTime(), body.actorOrgCode(), body.reasonCode(), body.evidenceRef(), "{}", "MANUAL", record, hashing.sha256(record + body.eventTime())));
        return env(Map.of("id", event.getId()), req);
    }

    private void validateAttributes(Map<String, Object> attributes) { snAutoFill.validateAttributes(attributes); }
    private static <T> ApiEnvelope<T> env(T data, HttpServletRequest req) { Object trace = req.getAttribute("traceId"); return new ApiEnvelope<>(data, trace == null ? "" : trace.toString()); }

    public record CreateModel(@NotBlank String modelCode, @NotBlank String revisionCode, @NotBlank String manufacturerName, @NotBlank String batteryCategory,
            @NotBlank String chemistry, @jakarta.validation.constraints.NotNull BigDecimal ratedCapacityKwh, BigDecimal ratedVoltageV,
            @jakarta.validation.constraints.NotNull java.time.LocalDate effectiveFrom, String sourceHash, String snProductCode, Map<String, Object> dppAttributes) {}
    public record CreateUnit(String modelRevisionId, @NotBlank String sn, String modelCode, String revisionCode, String batchCode,
            OffsetDateTime manufacturedAt, String deviceId, String sourceHash, Map<String, Object> dppAttributes) {}
    public record CreateEvent(@NotBlank String sn, @NotBlank String eventType, @jakarta.validation.constraints.NotNull OffsetDateTime eventTime,
            @NotBlank String actorOrgCode, String reasonCode, String evidenceRef) {}
    public record BindingRequest(@NotBlank String deviceId, String changeReason, OffsetDateTime boundAt) {}
    public record EventRequest(@NotBlank String eventType, @jakarta.validation.constraints.NotNull OffsetDateTime eventTime, String actorOrgCode, String reasonCode, String evidenceRef) {}
}
