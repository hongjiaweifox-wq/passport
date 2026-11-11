package com.tuya.passport.cloud.l2.service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Immutable in-memory view of the DPP attribute catalog. */
@Component
public class DppAttributeCatalog {
    private static final Set<String> RESERVED = Set.of("dpp_schema_version", "dpp_status", "dpp_granularity", "dpp_last_updated", "dpp_identifier", "unique_battery_identifier", "model_code", "sn", "manufacturer_name", "battery_category", "chemistry", "manufactured_at", "rated_voltage_v");
    private final Map<String, Attribute> attributes;

    public DppAttributeCatalog() {
        try (var reader = new InputStreamReader(new ClassPathResource("dpp/dpp-attributes-v2.0.csv").getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                 .setHeader("dpp_no", "category", "sub_category", "field_code", "display_name_en", "display_name_zh", "value_type", "unit_code", "visibility_class", "granularity", "mandatory_stationary", "static_dynamic", "storage_target", "implementation_status", "legal_reference", "short_definition")
                 .setSkipHeaderRecord(true).setIgnoreEmptyLines(true).build().parse(reader)) {
            Map<String, Attribute> loaded = new LinkedHashMap<>();
            for (var row : parser) {
                String code = row.get("field_code").trim();
                if (loaded.putIfAbsent(code, new Attribute(code, row.get("category"), row.get("display_name_zh"), row.get("value_type"), row.get("visibility_class"), row.get("implementation_status"))) != null) {
                    throw new IllegalStateException("Duplicate DPP field code: " + code);
                }
            }
            if (loaded.size() != 92) throw new IllegalStateException("Expected 92 DPP attributes, got " + loaded.size());
            attributes = Map.copyOf(loaded);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load DPP attribute catalog", ex);
        }
    }

    public boolean contains(String code) { return attributes.containsKey(code); }
    public boolean isReserved(String code) { return RESERVED.contains(code); }
    public Set<String> codes() { return attributes.keySet(); }
    public List<Attribute> all() { return List.copyOf(attributes.values()); }
    public Attribute find(String code) { return attributes.get(code); }

    public record Attribute(String code, String category, String nameZh, String valueType, String visibility, String implementationStatus) {}
}
