package com.tuya.passport.cloud.l3.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.common.support.Hashing;
import com.tuya.passport.cloud.l2.service.DppAttributeCatalog;
import com.tuya.passport.cloud.l3.domain.BatchEntity;
import com.tuya.passport.cloud.l3.domain.BatteryModelEntity;
import com.tuya.passport.cloud.l3.domain.BatteryUnitEntity;
import com.tuya.passport.cloud.l3.domain.DeviceBindingEntity;
import com.tuya.passport.cloud.l3.domain.LifecycleEventEntity;
import com.tuya.passport.cloud.l3.domain.ModelRevisionEntity;
import com.tuya.passport.cloud.l3.repository.BatchRepository;
import com.tuya.passport.cloud.l3.repository.BatteryModelRepository;
import com.tuya.passport.cloud.l3.repository.BatteryUnitRepository;
import com.tuya.passport.cloud.l3.repository.DeviceBindingRepository;
import com.tuya.passport.cloud.l3.repository.LifecycleEventRepository;
import com.tuya.passport.cloud.l3.repository.ModelRevisionRepository;
import com.tuya.passport.cloud.l3.support.TuyaSnDecoder;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SnAutoFillService {
    private final BatteryModelRepository models;
    private final ModelRevisionRepository revisions;
    private final BatchRepository batches;
    private final BatteryUnitRepository units;
    private final DeviceBindingRepository bindings;
    private final LifecycleEventRepository events;
    private final DppAttributeCatalog catalog;
    private final Hashing hashing;

    public SnAutoFillService(BatteryModelRepository models, ModelRevisionRepository revisions, BatchRepository batches,
            BatteryUnitRepository units, DeviceBindingRepository bindings, LifecycleEventRepository events,
            DppAttributeCatalog catalog, Hashing hashing) {
        this.models = models; this.revisions = revisions; this.batches = batches; this.units = units;
        this.bindings = bindings; this.events = events; this.catalog = catalog; this.hashing = hashing;
    }

    @Transactional
    public Resolution resolve(String org, String rawSn, String modelCode, String revisionCode, String batchCode, OffsetDateTime manufacturedAt) {
        if (rawSn == null || rawSn.isBlank()) throw bad("L3_VALIDATION_ERROR", "序列号 SN 不能为空");
        String normalized = Normalizer.normalize(rawSn.trim(), Normalizer.Form.NFC).toUpperCase(Locale.ROOT);
        var decoded = TuyaSnDecoder.decode(normalized).orElse(null);
        BatteryModelEntity model = null;
        if (modelCode != null && !modelCode.isBlank()) model = models.findByOrgCodeAndIssuerCodeAndModelCode(org, org, modelCode.trim()).orElse(null);
        else if (decoded != null) {
            var matches = models.findByOrgCodeAndSnProductCode(org, decoded.productCode());
            if (matches.size() > 1) throw bad("L3_MODEL_AMBIGUOUS", "More than one battery model uses SN product code " + decoded.productCode());
            if (matches.size() == 1) model = matches.get(0);
        }
        if (model == null) throw bad(decoded == null ? "L3_MODEL_REVISION_NOT_FOUND" : "L3_MODEL_NOT_FOUND", "A model matching the SN product code must be created first, or provide modelCode");
        if (decoded != null && modelCode != null && !modelCode.isBlank()
                && model.getSnProductCode() != null && !model.getSnProductCode().isBlank()
                && !decoded.productCode().equals(model.getSnProductCode())) {
            throw bad("L3_SN_MODEL_MISMATCH", "所选型号未绑定该 SN 产品型号代码");
        }
        ModelRevisionEntity revision;
        if (revisionCode != null && !revisionCode.isBlank()) revision = revisions.findByOrgCodeAndModelIdAndRevisionCode(org, model.getId(), revisionCode.trim()).orElse(null);
        else revision = revisions.findByOrgCodeAndModelIdOrderByCreatedAtDesc(org, model.getId()).stream().filter(r -> r.getStatus() == com.tuya.passport.cloud.l3.domain.ModelRevisionStatus.ACTIVE).findFirst().orElse(null);
        if (revision == null) throw bad("L3_MODEL_REVISION_NOT_FOUND", "An active model revision must be created before its units");

        Map<String, Object> autoFilled = new LinkedHashMap<>();
        Map<String, Object> conflicts = new LinkedHashMap<>();
        Map<String, Object> snDerived = new LinkedHashMap<>();
        String resolvedBatch = batchCode == null ? "" : batchCode.trim();
        OffsetDateTime resolvedManufactured = manufacturedAt;
        if (decoded != null) {
            String snDate = decoded.manufacturedDate().toString();
            if (resolvedManufactured != null && !resolvedManufactured.toLocalDate().equals(decoded.manufacturedDate())) {
                conflicts.put("manufactured_at", Map.of("csvValue", resolvedManufactured.toString(), "snValue", snDate));
                resolvedManufactured = decoded.manufacturedDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            } else if (resolvedManufactured == null) {
                resolvedManufactured = decoded.manufacturedDate().atStartOfDay().atOffset(ZoneOffset.UTC);
                autoFilled.put("manufactured_at", resolvedManufactured.toString());
            }
            if (resolvedBatch.isBlank()) { resolvedBatch = decoded.batchCode(); autoFilled.put("batch_code", resolvedBatch); }
            else if (!resolvedBatch.equals(decoded.batchCode())) { conflicts.put("batch_code", Map.of("csvValue", resolvedBatch, "snValue", decoded.batchCode())); resolvedBatch = decoded.batchCode(); }
            autoFilled.put("sn_product_code", decoded.productCode());
            autoFilled.put("sn_factory_code", decoded.factoryCode());
            autoFilled.put("sn_brand_code", decoded.brandCode());
            autoFilled.put("sn_cell_brand_code", decoded.cellBrandCode());
            snDerived.put("schemeCode", decoded.schemeCode()); snDerived.put("schemeName", decoded.schemeName());
            snDerived.put("productCode", decoded.productCode()); snDerived.put("productName", decoded.productName());
            snDerived.put("factoryCode", decoded.factoryCode()); snDerived.put("factoryName", decoded.factoryName());
            snDerived.put("brandCode", decoded.brandCode()); snDerived.put("brandName", decoded.brandName());
            snDerived.put("cellBrandCode", decoded.cellBrandCode()); snDerived.put("cellBrandName", decoded.cellBrandName());
            snDerived.put("manufacturedDate", snDate); snDerived.put("serial", decoded.serial());
        }
        return new Resolution(normalized, model, revision, resolvedManufactured, resolvedBatch, autoFilled, snDerived, conflicts);
    }

    @Transactional
    public UnitCreated createUnit(String org, String rawSn, String modelCode, String revisionCode, String batchCode,
            OffsetDateTime manufacturedAt, String deviceId, Map<String, Object> dppAttributes) {
        validateAttributes(dppAttributes);
        Resolution resolved = resolve(org, rawSn, modelCode, revisionCode, batchCode, manufacturedAt);
        if (units.findByOrgCodeAndIssuerCodeAndSnNormalized(org, org, resolved.normalizedSn()).isPresent()) {
            throw new ApiException("L3_SN_EXISTS", "该序列号已存在", HttpStatus.CONFLICT);
        }
        String batchId = null;
        if (resolved.batchCode() != null && !resolved.batchCode().isBlank()) {
            BatchEntity batch = batches.findByOrgCodeAndModelRevisionIdAndBatchCode(org, resolved.revision().getId(), resolved.batchCode())
                .orElseGet(() -> batches.save(new BatchEntity(org, resolved.revision().getId(), resolved.batchCode(), "{}")));
            batchId = batch.getId();
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (dppAttributes != null && !dppAttributes.isEmpty()) attributes.put("dpp", dppAttributes);
        if (!resolved.snDerived().isEmpty()) attributes.put("snDerived", resolved.snDerived());
        String attributeJson = attributes.isEmpty() ? null : hashing.json(attributes);
        String sourceId = UUID.randomUUID().toString();
        BatteryUnitEntity unit = units.save(new BatteryUnitEntity(org, org, rawSn.trim(), resolved.normalizedSn(), resolved.revision().getId(), batchId, resolved.manufacturedAt(), "MANUAL", sourceId, hashing.sha256(sourceId + resolved.normalizedSn()), attributeJson));
        if (deviceId != null && !deviceId.isBlank()) bindings.save(new DeviceBindingEntity(org, unit.getId(), deviceId.trim(), OffsetDateTime.now(), "Initial manual entry"));
        return new UnitCreated(unit.getId(), resolved.snDerived(), resolved.autoFilled(), resolved.conflicts());
    }

    @Transactional
    public String createEvent(String org, String sn, String eventType, OffsetDateTime eventTime, String actorOrgCode, String reasonCode, String evidenceRef) {
        BatteryUnitEntity unit = units.findByOrgCodeAndIssuerCodeAndSnNormalized(org, org, normalize(sn)).orElseThrow(() -> new ApiException("L3_BATTERY_UNIT_NOT_FOUND", "未找到该 SN 对应的电池单体", HttpStatus.NOT_FOUND));
        String sourceId = UUID.randomUUID().toString();
        events.save(new LifecycleEventEntity(org, unit.getId(), eventType, eventTime, actorOrgCode, reasonCode, evidenceRef, "{}", "MANUAL", sourceId, hashing.sha256(sourceId + eventTime)));
        return sourceId;
    }

    public void validateAttributes(Map<String, ?> attributes) {
        if (attributes == null) return;
        for (String code : attributes.keySet()) {
            if (code == null || !code.matches("[a-z][a-z0-9_]{0,95}")) throw bad("INGEST_INVALID_ATTRIBUTE_KEY", "DPP 属性编码必须为 snake_case");
            if (catalog.isReserved(code)) throw bad("INGEST_RESERVED_ATTRIBUTE", "该属性已有专用字段或由系统生成: " + code);
            if (!catalog.contains(code)) throw bad("INGEST_UNKNOWN_ATTRIBUTE", "扩展属性编码不在 DPP 字段字典中: " + code);
            var meta = catalog.find(code);
            if (meta == null || !"GENERIC".equals(meta.implementationStatus())) throw bad("INGEST_RESERVED_ATTRIBUTE", "该属性不支持通过扩展属性填报: " + code);
            Object value = attributes.get(code);
            if (value == null || value instanceof Map<?, ?> || value instanceof Iterable<?>) throw bad("INGEST_INVALID_ATTRIBUTE_VALUE", "DPP 扩展属性必须为简单值: " + code);
        }
    }

    private static String normalize(String sn) { return Normalizer.normalize(sn == null ? "" : sn.trim(), Normalizer.Form.NFC).toUpperCase(Locale.ROOT); }
    private static ApiException bad(String code, String message) { return new ApiException(code, message, HttpStatus.BAD_REQUEST); }
    public record Resolution(String normalizedSn, BatteryModelEntity model, ModelRevisionEntity revision, OffsetDateTime manufacturedAt, String batchCode, Map<String, Object> autoFilled, Map<String, Object> snDerived, Map<String, Object> conflicts) {}
    public record UnitCreated(String id, Map<String, Object> snDerived, Map<String, Object> autoFilled, Map<String, Object> conflicts) {}
}
