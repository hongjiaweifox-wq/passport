package com.tuya.passport.cloud.l4.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.l3.domain.BatteryModelEntity;
import com.tuya.passport.cloud.l3.domain.BatteryUnitEntity;
import com.tuya.passport.cloud.l3.domain.ModelRevisionEntity;
import com.tuya.passport.cloud.l3.repository.BatteryModelRepository;
import com.tuya.passport.cloud.l3.repository.BatteryUnitRepository;
import com.tuya.passport.cloud.l3.repository.ModelRevisionRepository;
import com.tuya.passport.cloud.l3.service.SubjectSnapshotService;
import com.tuya.passport.cloud.l4.domain.FieldDefinitionEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FieldCoverageService {
    private static final String SCHEMA = "dpp-schema/1.0";
    private final FieldDefinitionService definitions;
    private final BatteryUnitRepository units;
    private final ModelRevisionRepository revisions;
    private final BatteryModelRepository models;
    private final SubjectSnapshotService snapshots;
    private final ObjectMapper mapper;

    public FieldCoverageService(FieldDefinitionService definitions, BatteryUnitRepository units, ModelRevisionRepository revisions, BatteryModelRepository models, SubjectSnapshotService snapshots, ObjectMapper mapper) {
        this.definitions = definitions;
        this.units = units;
        this.revisions = revisions;
        this.models = models;
        this.snapshots = snapshots;
        this.mapper = mapper;
    }

    @Transactional
    public CoverageView coverage(String org, String subjectType, String subjectId) {
        List<FieldDefinitionEntity> fields = definitions.ensureDefaults(org);
        Map<String, Long> categories = new TreeMap<>();
        Map<String, Long> implementations = new TreeMap<>();
        long mandatory = 0; long publicCount=0;
        for (FieldDefinitionEntity field : fields) {
            categories.merge(field.getCategory() == null ? "Other" : field.getCategory(), 1L, Long::sum);
            implementations.merge(field.getImplementationStatus() == null ? "UNKNOWN" : field.getImplementationStatus(), 1L, Long::sum);
            if (Boolean.TRUE.equals(field.getMandatoryStationary())) mandatory++;
            if (field.getVisibilityClass().name().equals("PUBLIC")) publicCount++;
        }
        if (subjectType == null || subjectType.isBlank() || subjectId == null || subjectId.isBlank()) {
            return new CoverageView(fields.size(), mandatory, fields.stream().filter(f -> f.getVisibilityClass().name().equals("PUBLIC")).count(), categories, implementations, null, null, null, List.of());
        }
        Map<String, Object> snapshot = snapshots.snapshot(org, subjectType, subjectId);
        Map<String, Object> flat = flatValues(snapshot, subjectType, org, subjectId);
        List<FieldCoverage> entries = new ArrayList<>();
        long mandatoryFilled = 0;
        for (FieldDefinitionEntity field : fields) {
            Object value = resolve(field, flat);
            boolean filled = present(value);
            if (Boolean.TRUE.equals(field.getMandatoryStationary()) && filled) mandatoryFilled++;
            entries.add(new FieldCoverage(field.getFieldCode(), field.getDppNo(), field.getCategory(), field.getDisplayNameZh(), Boolean.TRUE.equals(field.getMandatoryStationary()), filled, source(field, flat, value), field.getVisibilityClass().name()));
        }
        double pct = mandatory == 0 ? 100.0 : Math.round(mandatoryFilled * 10000.0 / mandatory) / 100.0;
        return new CoverageView(fields.size(), mandatory, fields.stream().filter(f -> f.getVisibilityClass().name().equals("PUBLIC")).count(), categories, implementations, subjectType, subjectId, new CoverageSummary(mandatoryFilled, pct), entries);
    }

    private Map<String, Object> flatValues(Map<String, Object> snapshot, String subjectType, String org, String subjectId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object model = snapshot.get("model"), revision = snapshot.get("revision"), unit = snapshot.get("unit");
        merge(result, model); merge(result, revision); merge(result, unit);
        if ("BATTERY_UNIT".equals(subjectType)) {
            BatteryUnitEntity unitEntity = units.findByOrgCodeAndId(org, subjectId).orElseThrow(() -> ApiException.notFound("L3_NOT_FOUND", "Battery unit not found"));
            ModelRevisionEntity revisionEntity = revisions.findByOrgCodeAndId(org, unitEntity.getModelRevisionId()).orElseThrow(() -> ApiException.notFound("L3_NOT_FOUND", "Model revision not found"));
            BatteryModelEntity modelEntity = models.findByOrgCodeAndId(org, revisionEntity.getModelId()).orElseThrow(() -> ApiException.notFound("L3_NOT_FOUND", "Battery model not found"));
            putJsonAttrs(result, modelEntity); putJsonAttrs(result, revisionEntity); putJsonAttrs(result, unitEntity);
        } else if ("BATTERY_MODEL_REVISION".equals(subjectType)) {
            ModelRevisionEntity revisionEntity = revisions.findByOrgCodeAndId(org, subjectId).orElseThrow(() -> ApiException.notFound("L3_NOT_FOUND", "Model revision not found"));
            putJsonAttrs(result, revisionEntity);
        }
        return result;
    }

    private void putJsonAttrs(Map<String, Object> result, Object entity) {
        try {
            String json = entity instanceof BatteryUnitEntity u ? u.getAttributeJson() : entity instanceof ModelRevisionEntity r ? r.getAttributeJson() : null;
            if (json == null || json.isBlank()) return;
            Map<String, Object> attrs = mapper.readValue(json, new TypeReference<>() {});
            merge(result, attrs.get("dpp"));
            merge(result, attrs.get("snDerived"));
            merge(result, attrs);
        } catch (Exception ignored) { }
    }

    private static Object resolve(FieldDefinitionEntity field, Map<String, Object> values) {
        String code = field.getFieldCode();
        Object value = values.get(code);
        if (value == null) value = values.get(camel(code));
        if (value == null) value = switch (code) {
            case "battery_model_identifier" -> values.get("modelCode");
            case "battery_serial_number" -> values.get("snRaw");
            case "manufacturing_date" -> values.get("manufacturedAt");
            case "manufacturer_information" -> values.get("manufacturerName");
            case "materials_battery_chemistry" -> values.containsKey("chemistryCode") ? values.get("chemistryCode") : values.get("chemistry");
            case "performance_nominal_voltage" -> values.get("ratedVoltageV");
            default -> null;
        };
        if (value == null && "battery_mass".equals(code)) value = values.get("battery_mass");
        return value;
    }

    private static String source(FieldDefinitionEntity field, Map<String, Object> values, Object value) {
        if (!present(value)) return null;
        return switch (field.getImplementationStatus() == null ? "" : field.getImplementationStatus()) {
            case "GENERATED" -> "GENERATED";
            case "NATIVE" -> "NATIVE";
            default -> values.containsKey(field.getFieldCode()) || values.containsKey(camel(field.getFieldCode())) ? "GENERIC" : "NATIVE";
        };
    }

    private static boolean present(Object value) { return value != null && !value.toString().isBlank() && !"null".equalsIgnoreCase(value.toString()); }
    private static void merge(Map<String, Object> into, Object source) { if (source instanceof Map<?, ?> map) map.forEach((k, v) -> into.put(String.valueOf(k), v)); }
    private static String camel(String snake) { StringBuilder b = new StringBuilder(); boolean upper = false; for (char c : snake.toCharArray()) { if (c == '_') upper = true; else { b.append(upper ? Character.toUpperCase(c) : c); upper = false; } } return b.toString(); }

    public record FieldCoverage(String fieldCode, Integer dppNo, String category, String displayNameZh, boolean mandatory, boolean filled, String source, String visibility) {}
    public record CoverageSummary(long mandatoryFilled, double coveragePct) {}
    public record CoverageView(long total, long mandatory, long publicCount, Map<String, Long> categories, Map<String, Long> implementationStatus, String subjectType, String subjectId, CoverageSummary coverage, List<FieldCoverage> fields) {}
}
