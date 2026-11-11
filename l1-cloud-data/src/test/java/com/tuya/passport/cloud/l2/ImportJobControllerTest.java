package com.tuya.passport.cloud.l2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuya.passport.cloud.l2.service.L1Source;
import com.tuya.passport.cloud.l2.service.L1SourceClient;
import com.tuya.passport.cloud.l2.domain.ImportJobEntity;
import com.tuya.passport.cloud.l2.repository.ImportJobRepository;
import com.tuya.passport.cloud.l2.repository.ImportRowErrorRepository;
import com.tuya.passport.cloud.l2.repository.ImportRowRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportJobControllerTest {
    @Autowired MockMvc mvc; @Autowired ImportJobRepository jobs; @Autowired ImportRowRepository rows; @Autowired ImportRowErrorRepository errors;
    @MockBean L1SourceClient l1;

    @DynamicPropertySource
    static void l2TestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:l2test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        registry.add("l2.storage.root", () -> "./target/test-uploads-l2");
    }

    @BeforeEach void setup(){errors.deleteAll();rows.deleteAll();jobs.deleteAll();given(l1.list("DEMO-ORG")).willReturn(List.of(new L1Source("1","DEMO-ORG","SUPPLIER-PLM","Supplier PLM","SUPPLIER","ACTIVE",Set.of("BATTERY_MODEL","BATTERY_UNIT"))));given(l1.requireByCode("DEMO-ORG","SUPPLIER-PLM")).willReturn(new L1Source("1","DEMO-ORG","SUPPLIER-PLM","Supplier PLM","SUPPLIER","ACTIVE",Set.of("BATTERY_MODEL","BATTERY_UNIT")));}
    private MockMultipartFile file(String name,String content){return new MockMultipartFile("file",name,"text/csv",content.getBytes(StandardCharsets.UTF_8));}
    private String validCsv(){return "模板版本,来源系统,来源记录编号,组织编码,型号编码,修订版本,制造商名称,电池类别,化学体系,额定容量(kWh),额定电压(V),目标市场,生效日期\n"+
      "dpp-schema/1.0,SUPPLIER-PLM,R1,DEMO-ORG,HOME-5,R1,Example,INDUSTRIAL,LFP,5.2,51.2,EU,2026-01-01\n";}
    private String legacyEnglishCsv(){return "schema_version,source_system,source_record_id,org_code,model_code,revision_code,manufacturer_name,battery_category,chemistry,rated_capacity_kwh,rated_voltage_v,market_code,effective_from\n"+
      "dpp-schema/1.0,SUPPLIER-PLM,R1,DEMO-ORG,HOME-5,R1,Example,INDUSTRIAL,LFP,5.2,51.2,EU,2026-01-01\n";}
    @Test void validCsvCreatesCompletedJobAndStagingRow() throws Exception {
        mvc.perform(multipart("/api/v1/l2/import-jobs").file(file("电池型号模板.csv",validCsv())).param("sourceCode","SUPPLIER-PLM").param("entityType","BATTERY_MODEL").header("X-Org-Code","DEMO-ORG").header("Idempotency-Key","same-key"))
          .andExpect(status().isAccepted());
        ImportJobEntity job=jobs.findAll().get(0);org.junit.jupiter.api.Assertions.assertEquals("COMPLETED",job.getStatus().name());org.junit.jupiter.api.Assertions.assertEquals(1,job.getAcceptedRows());org.junit.jupiter.api.Assertions.assertEquals(1,rows.findByOrgCodeAndJobIdAndAttemptNo("DEMO-ORG",job.getId(),1).size());
        org.junit.jupiter.api.Assertions.assertTrue(rows.findByOrgCodeAndJobIdAndAttemptNo("DEMO-ORG",job.getId(),1).get(0).getCanonicalJson().contains("\"model_code\""));
    }
    @Test void legacyEnglishHeadersRemainAccepted() throws Exception {
        mvc.perform(multipart("/api/v1/l2/import-jobs").file(file("battery_model.csv",legacyEnglishCsv())).param("sourceCode","SUPPLIER-PLM").param("entityType","BATTERY_MODEL").header("X-Org-Code","DEMO-ORG").header("Idempotency-Key","legacy-key"))
          .andExpect(status().isAccepted());
        org.junit.jupiter.api.Assertions.assertEquals("COMPLETED",jobs.findAll().get(0).getStatus().name());
    }
    @Test void templateDownloadsWithChineseFilenameAndHeaders() throws Exception {
        mvc.perform(get("/api/v1/l2/templates/BATTERY_MODEL"))
          .andExpect(status().isOk())
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.containsString("%E7%94%B5%E6%B1%A0%E5%9E%8B%E5%8F%B7%E6%A8%A1%E6%9D%BF.csv")))
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("模板版本,来源系统,来源记录编号,组织编码")));
    }
    @Test void sameKeyAndFileReusesJobButDifferentBytesConflict() throws Exception {
        mvc.perform(multipart("/api/v1/l2/import-jobs").file(file("battery.csv",validCsv())).param("sourceCode","SUPPLIER-PLM").param("entityType","BATTERY_MODEL").header("Idempotency-Key","idem")).andExpect(status().isAccepted());
        mvc.perform(multipart("/api/v1/l2/import-jobs").file(file("battery.csv",validCsv())).param("sourceCode","SUPPLIER-PLM").param("entityType","BATTERY_MODEL").header("Idempotency-Key","idem")).andExpect(status().isAccepted()).andExpect(jsonPath("$.data.reused").value(true));
        mvc.perform(multipart("/api/v1/l2/import-jobs").file(file("battery.csv",validCsv()+"\n")).param("sourceCode","SUPPLIER-PLM").param("entityType","BATTERY_MODEL").header("Idempotency-Key","idem")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("L2_IDEMPOTENCY_CONFLICT"));
    }
    @Test void wrongHeaderFailsAndCrossOrganizationIsHidden() throws Exception {
        mvc.perform(multipart("/api/v1/l2/import-jobs").file(file("bad.csv","a,b\n1,2\n")).param("sourceCode","SUPPLIER-PLM").param("entityType","BATTERY_MODEL").header("Idempotency-Key","bad-header")).andExpect(status().isAccepted());
        ImportJobEntity job=jobs.findAll().get(0);org.junit.jupiter.api.Assertions.assertEquals("INGEST_SCHEMA_MISMATCH",job.getFailureCode());
        mvc.perform(get("/api/v1/l2/import-jobs/"+job.getId()).header("X-Org-Code","OTHER")).andExpect(status().isNotFound());
    }
    @Test void invalidExtensionRejected() throws Exception {
        mvc.perform(multipart("/api/v1/l2/import-jobs").file(file("battery.xlsx",validCsv())).param("sourceCode","SUPPLIER-PLM").param("entityType","BATTERY_MODEL")).andExpect(status().isUnsupportedMediaType());
    }
}
