package com.tuya.passport.cloud;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowEntity;
import com.tuya.passport.cloud.l2.domain.ImportRowStatus;
import com.tuya.passport.cloud.l2.domain.EntityType;
import com.tuya.passport.cloud.l2.domain.ImportJobStatus;
import com.tuya.passport.cloud.l2.repository.ImportJobRepository;
import com.tuya.passport.cloud.l2.repository.ImportRowRepository;
import com.tuya.passport.cloud.l1.api.DataSourceController;
import com.tuya.passport.cloud.l3.domain.BatteryUnitEntity;
import com.tuya.passport.cloud.l3.repository.BatteryUnitRepository;
import com.tuya.passport.cloud.l3.repository.BatteryModelRepository;
import com.tuya.passport.cloud.l3.repository.ModelRevisionRepository;
import com.tuya.passport.cloud.l3.repository.BatchRepository;
import com.tuya.passport.cloud.l3.repository.DeviceBindingRepository;
import com.tuya.passport.cloud.l3.repository.LifecycleEventRepository;
import com.tuya.passport.cloud.l3.repository.PromotionItemRepository;
import com.tuya.passport.cloud.l3.repository.PromotionRunRepository;
import com.tuya.passport.cloud.l4.repository.FieldDefinitionRepository;
import com.tuya.passport.cloud.l4.repository.ValidationIssueRepository;
import com.tuya.passport.cloud.l4.repository.ValidationRunRepository;
import com.tuya.passport.cloud.l4.repository.ReviewTaskRepository;
import com.tuya.passport.cloud.l4.repository.PublicationCandidateRepository;
import com.tuya.passport.cloud.l5.repository.PassportRepository;
import com.tuya.passport.cloud.l5.repository.PassportVersionRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class L3L4L5FlowTest {
 @Autowired MockMvc mvc;@Autowired ImportJobRepository jobs;@Autowired ImportRowRepository rows;@Autowired BatteryModelRepository models;@Autowired ModelRevisionRepository revisions;@Autowired BatteryUnitRepository units;@Autowired BatchRepository batches;@Autowired DeviceBindingRepository bindings;@Autowired LifecycleEventRepository events;@Autowired PromotionRunRepository promotionRuns;@Autowired PromotionItemRepository promotionItems;@Autowired FieldDefinitionRepository fieldDefinitions;@Autowired ValidationRunRepository validationRuns;@Autowired ValidationIssueRepository validationIssues;@Autowired ReviewTaskRepository reviewTasks;@Autowired PublicationCandidateRepository candidates;@Autowired PassportRepository passports;@Autowired PassportVersionRepository passportVersions;
 @MockBean com.tuya.passport.cloud.l2.service.L1SourceClient l1;
 @DynamicPropertySource static void db(DynamicPropertyRegistry registry){registry.add("spring.datasource.url",()->"jdbc:h2:mem:l3l4l5test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");registry.add("l2.storage.root",()->"./target/test-uploads-l3l4l5");}
 @BeforeEach void reset(){passportVersions.deleteAll();passports.deleteAll();candidates.deleteAll();reviewTasks.deleteAll();validationIssues.deleteAll();validationRuns.deleteAll();fieldDefinitions.deleteAll();promotionItems.deleteAll();promotionRuns.deleteAll();events.deleteAll();bindings.deleteAll();units.deleteAll();batches.deleteAll();revisions.deleteAll();models.deleteAll();rows.deleteAll();jobs.deleteAll();given(l1.list("DEMO-ORG")).willReturn(List.of());}
 @Test void promotesAndPublishesPublicProjection() throws Exception {String jobId="job-model";ImportJobEntity job=new ImportJobEntity(jobId,"DEMO-ORG","SRC","Source",EntityType.BATTERY_MODEL,"dpp-schema/1.0","model.csv",20,"sha","/tmp/model.csv","idem","tester",null);job.changeStatus(ImportJobStatus.COMPLETED);jobs.save(job);rows.save(new ImportRowEntity("DEMO-ORG",jobId,1,1,"model-rec","model-hash","{\"entityType\":\"BATTERY_MODEL\",\"schemaVersion\":\"dpp-schema/1.0\",\"sourceSystem\":\"SRC\",\"sourceRecordId\":\"model-rec\",\"orgCode\":\"DEMO-ORG\",\"data\":{\"model_code\":\"M1\",\"revision_code\":\"R1\",\"manufacturer_name\":\"Maker\",\"battery_category\":\"INDUSTRIAL\",\"chemistry\":\"LFP\",\"rated_capacity_kwh\":\"5.2\",\"rated_voltage_v\":\"51.2\",\"market_code\":\"EU\",\"effective_from\":\"2026-01-01\"}}",ImportRowStatus.ACCEPTED));
 mvc.perform(post("/api/v1/l3/promotions/import-jobs/"+jobId).header("X-Org-Code","DEMO-ORG").header("X-User","alice").header("X-Role","COMPLIANCE_MAKER")).andExpect(status().isOk()).andExpect(jsonPath("$.data.promoted").value(1));
 assertEquals(1,models.countByOrgCode("DEMO-ORG"));assertEquals(1,revisions.countByOrgCode("DEMO-ORG"));
 var model=models.findByOrgCodeAndIssuerCodeAndModelCode("DEMO-ORG","DEMO-ORG","M1").orElseThrow();var revision=revisions.findByOrgCodeAndModelIdAndRevisionCode("DEMO-ORG",model.getId(),"R1").orElseThrow();BatteryUnitEntity unit=units.save(new BatteryUnitEntity("DEMO-ORG","DEMO-ORG","SN-001","SN-001",revision.getId(),null,null,"SRC","unit-rec","unit-hash"));
 mvc.perform(post("/api/v1/l4/records/BATTERY_UNIT/"+unit.getId()+"/validate").header("X-Org-Code","DEMO-ORG").header("X-User","alice").header("X-Role","COMPLIANCE_MAKER")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PASS"));
 mvc.perform(post("/api/v1/l4/reviews").header("X-Org-Code","DEMO-ORG").header("X-User","alice").header("X-Role","COMPLIANCE_MAKER").contentType(MediaType.APPLICATION_JSON).content("{\"subjectType\":\"BATTERY_UNIT\",\"subjectId\":\""+unit.getId()+"\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.data.makerId").value("alice"));String reviewId=reviewTasks.findAll().get(0).getId();
 mvc.perform(post("/api/v1/l4/reviews/"+reviewId+":decision").header("X-Org-Code","DEMO-ORG").header("X-User","alice").header("X-Role","COMPLIANCE_CHECKER").contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\",\"reason\":\"same person\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("GOV_MAKER_CHECKER_CONFLICT"));
 mvc.perform(post("/api/v1/l4/reviews/"+reviewId+":decision").header("X-Org-Code","DEMO-ORG").header("X-User","bob").header("X-Role","COMPLIANCE_CHECKER").contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\",\"reason\":\"verified\"}")).andExpect(status().isOk());
 String passportId=mvc.perform(post("/api/v1/l5/passports").header("X-Org-Code","DEMO-ORG").header("X-User","carol").header("X-Role","PASSPORT_OPERATOR").contentType(MediaType.APPLICATION_JSON).content("{\"batteryUnitId\":\""+unit.getId()+"\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();String id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(passportId).path("data").path("id").asText();
 mvc.perform(post("/api/v1/l5/passports/"+id+":publish").header("X-Org-Code","DEMO-ORG").header("X-User","carol").header("X-Role","PASSPORT_OPERATOR").header("Idempotency-Key","publish-key")).andExpect(status().isOk()).andExpect(jsonPath("$.data.version.versionNo").value(1));String publicCode=passports.findByOrgCodeAndId("DEMO-ORG",id).orElseThrow().getPublicCode();
 mvc.perform(post("/api/v1/l5/passports/"+id+":publish").header("X-Org-Code","DEMO-ORG").header("X-User","carol").header("X-Role","PASSPORT_OPERATOR").header("Idempotency-Key","publish-key")).andExpect(status().isOk()).andExpect(jsonPath("$.data.reused").value(true)).andExpect(jsonPath("$.data.version.versionNo").value(1));
 var publicResponse=mvc.perform(get("/api/v1/public/passports/"+publicCode)).andExpect(status().isOk()).andExpect(jsonPath("$.data.data.model_code").value("M1")).andExpect(jsonPath("$.data.data.battery_serial_number").value("SN-001")).andExpect(jsonPath("$.data.data.sn").value("SN-001")).andExpect(jsonPath("$.data.data.dpp_schema_version").value("dpp-schema/1.0")).andExpect(jsonPath("$.data.data.dpp_identifier").exists()).andExpect(jsonPath("$.data.data.unique_battery_identifier").exists()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("actor_org_code")))).andReturn();String etag=publicResponse.getResponse().getHeader("ETag");mvc.perform(get("/api/v1/public/passports/"+publicCode).header("If-None-Match",etag)).andExpect(status().isNotModified());
 mvc.perform(post("/api/v1/l5/passports/"+id+":suspend").header("X-Org-Code","DEMO-ORG").header("X-User","carol").header("X-Role","PASSPORT_OPERATOR").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"safety review\"}")).andExpect(status().isOk());mvc.perform(get("/api/v1/public/passports/"+publicCode)).andExpect(status().isGone());
 }

 @Test void fieldVisibilityEditSurvivesCatalogRefresh() throws Exception {
  String json=mvc.perform(get("/api/v1/l4/field-definitions").header("X-Org-Code","DEMO-ORG")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String id=com.jayway.jsonpath.JsonPath.read(json,"$.data[0].id");
  mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/l4/field-definitions/"+id).header("X-Org-Code","DEMO-ORG").header("X-User","alice").header("X-Role","COMPLIANCE_MAKER").contentType(MediaType.APPLICATION_JSON).content("{\"visibilityClass\":\"INTERNAL\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.visibilityClass").value("INTERNAL"));
  mvc.perform(get("/api/v1/l4/field-definitions").header("X-Org-Code","DEMO-ORG")).andExpect(status().isOk());
  assertEquals(com.tuya.passport.cloud.l4.domain.VisibilityClass.INTERNAL,fieldDefinitions.findById(id).orElseThrow().getVisibilityClass());
 }
}
