package com.tuya.passport.cloud.l3;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuya.passport.cloud.l3.repository.BatchRepository;
import com.tuya.passport.cloud.l3.repository.BatteryModelRepository;
import com.tuya.passport.cloud.l3.repository.BatteryUnitRepository;
import com.tuya.passport.cloud.l3.repository.DeviceBindingRepository;
import com.tuya.passport.cloud.l3.repository.LifecycleEventRepository;
import com.tuya.passport.cloud.l3.repository.ModelRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ManualRecordTest {
    private static final String ORG = "MANUAL-TEST";
    private static final String SN = "CAACBE2508040001";

    @Autowired MockMvc mvc;
    @Autowired BatteryModelRepository models;
    @Autowired ModelRevisionRepository revisions;
    @Autowired BatteryUnitRepository units;
    @Autowired BatchRepository batches;
    @Autowired DeviceBindingRepository bindings;
    @Autowired LifecycleEventRepository events;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:manualrecordtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() throws Exception {
        events.deleteAll();
        bindings.deleteAll();
        units.deleteAll();
        batches.deleteAll();
        revisions.deleteAll();
        models.deleteAll();
        createModel("MODEL-A", "R1", "A");
    }

    @Test
    void createsUnitFromSnAndRejectsDuplicate() throws Exception {
        mvc.perform(post("/api/v1/l3/battery-units").headers(editorHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sn\":\"" + SN + "\",\"modelCode\":\"MODEL-A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.autoFilled.manufactured_at").value("2025-08-04T00:00Z"))
                .andExpect(jsonPath("$.data.autoFilled.batch_code").value("SN-AC-20250804"))
                .andExpect(jsonPath("$.data.snDerived.productCode").value("A"))
                .andExpect(jsonPath("$.data.snDerived.factoryCode").value("C"))
                .andExpect(jsonPath("$.data.snDerived.cellBrandCode").value("E"));

        mvc.perform(post("/api/v1/l3/battery-units").headers(editorHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sn\":\"" + SN + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("L3_SN_EXISTS"));
    }

    @Test
    void rejectsSnThatConflictsWithSpecifiedModel() throws Exception {
        createModel("MODEL-B", "R1", "B");
        mvc.perform(post("/api/v1/l3/battery-units").headers(editorHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sn\":\"" + SN + "\",\"modelCode\":\"MODEL-B\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("L3_SN_MODEL_MISMATCH"));
    }

    @Test
    void snDecodeReportsMatchingModelAndActiveRevision() throws Exception {
        mvc.perform(get("/api/v1/l3/sn-decode").header("X-Org-Code", ORG).param("sn", SN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.modelMatch.status").value("MATCHED"))
                .andExpect(jsonPath("$.data.modelMatch.modelCode").value("MODEL-A"))
                .andExpect(jsonPath("$.data.modelMatch.revisionCode").value("R1"));
    }

    @Test
    void reportsNoMatchForValidSnWhenModelCodeIsNotRegistered() throws Exception {
        mvc.perform(get("/api/v1/l3/sn-decode").header("X-Org-Code", ORG).param("sn", "CBCACE2509270047"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.modelMatch.status").value("NO_MATCH"));
    }

    @Test
    void createsEventBySnAndDecodesValidAndInvalidSerialNumbers() throws Exception {
        mvc.perform(post("/api/v1/l3/battery-units").headers(editorHeaders()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sn\":\"" + SN + "\",\"modelCode\":\"MODEL-A\"}")).andExpect(status().isCreated());

        mvc.perform(post("/api/v1/l3/events").headers(editorHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sn\":\"" + SN + "\",\"eventType\":\"COMMISSIONED\",\"eventTime\":\"2025-08-05T10:00:00Z\",\"actorOrgCode\":\"" + ORG + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").isNotEmpty());

        mvc.perform(get("/api/v1/l3/sn-decode").header("X-Org-Code", ORG).param("sn", SN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.manufacturedDate").value("2025-08-04"));
        mvc.perform(get("/api/v1/l3/sn-decode").header("X-Org-Code", ORG).param("sn", "not-a-tuya-sn"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.valid").value(false));
    }

    @Test
    void rejectsUnknownDppExtensionAttribute() throws Exception {
        mvc.perform(post("/api/v1/l3/models").headers(editorHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content(modelPayload("MODEL-X", "R1", "A", "\"dppAttributes\":{\"not_a_dpp_field\":\"value\"},")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INGEST_UNKNOWN_ATTRIBUTE"));
    }

    private void createModel(String modelCode, String revisionCode, String snProductCode) throws Exception {
        mvc.perform(post("/api/v1/l3/models").headers(editorHeaders()).contentType(MediaType.APPLICATION_JSON)
                        .content(modelPayload(modelCode, revisionCode, snProductCode, "")))
                .andExpect(status().isCreated());
    }

    private static String modelPayload(String modelCode, String revisionCode, String snProductCode, String extra) {
        return "{" + extra + "\"modelCode\":\"" + modelCode + "\",\"revisionCode\":\"" + revisionCode
                + "\",\"manufacturerName\":\"Maker\",\"batteryCategory\":\"STATIONARY\",\"chemistry\":\"LFP\","
                + "\"ratedCapacityKwh\":2.0,\"ratedVoltageV\":51.2,\"effectiveFrom\":\"2025-01-01\","
                + "\"snProductCode\":\"" + snProductCode + "\"}";
    }

    private static org.springframework.http.HttpHeaders editorHeaders() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Org-Code", ORG);
        headers.set("X-User", "manual-editor");
        headers.set("X-Role", "DATA_EDITOR");
        return headers;
    }
}
