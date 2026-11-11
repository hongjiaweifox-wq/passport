package com.tuya.passport.cloud.l1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuya.passport.cloud.l1.api.dto.DataSourceRequest;
import com.tuya.passport.cloud.l1.domain.AuthMethod;
import com.tuya.passport.cloud.l1.domain.DataDomain;
import com.tuya.passport.cloud.l1.domain.DataFrequency;
import com.tuya.passport.cloud.l1.domain.DeliveryMethod;
import com.tuya.passport.cloud.l1.domain.SourceStatus;
import com.tuya.passport.cloud.l1.domain.SourceType;
import com.tuya.passport.cloud.l1.domain.TemplateType;
import com.tuya.passport.cloud.l1.repository.DataSourceRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataSourceControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DataSourceRepository repository;

    @BeforeEach void clean() { repository.deleteAll(); }

    @Test void createListUpdateAndStatus() throws Exception {
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-a")
                .contentType(MediaType.APPLICATION_JSON).content(json(request("VENDOR-A", 0))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.sourceCode").value("VENDOR-A"));
        String id = repository.findAll().get(0).getId();
        long version = repository.findAll().get(0).getVersion();
        String updateBody = json(request("VENDOR-A", version)).replace("\"sourceName\":\"A Source\"", "\"sourceName\":\"Updated Source\"");
        mvc.perform(put("/api/v1/l1/data-sources/" + id).header("X-Org-Code", "org-a")
                .contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.sourceName").value("Updated Source"));
        long updatedVersion = repository.findById(id).orElseThrow().getVersion();
        mvc.perform(patch("/api/v1/l1/data-sources/" + id + "/status").header("X-Org-Code", "org-a")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\",\"version\":" + updatedVersion + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUSPENDED"));
        mvc.perform(get("/api/v1/l1/data-sources").header("X-Org-Code", "org-a").param("page", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
    }

    @Test void createDoesNotRequireVersionButUpdateDoes() throws Exception {
        String bodyWithoutVersion = json(request("NO-VERSION", 0)).replace(",\"version\":0", "");
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-a")
                .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutVersion))
            .andExpect(status().isCreated());
        String id = repository.findAll().get(0).getId();
        mvc.perform(put("/api/v1/l1/data-sources/" + id).header("X-Org-Code", "org-a")
                .contentType(MediaType.APPLICATION_JSON).content(bodyWithoutVersion))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("L1_VALIDATION_ERROR"));
    }

    @Test void sourceCodeIsUniqueWithinOrganizationButNotAcrossOrganizations() throws Exception {
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-a").contentType(MediaType.APPLICATION_JSON).content(json(request("DUPLICATE", 0))))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-a").contentType(MediaType.APPLICATION_JSON).content(json(request("duplicate", 0))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("L1_SOURCE_CODE_DUPLICATE"));
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-b").contentType(MediaType.APPLICATION_JSON).content(json(request("duplicate", 0))))
            .andExpect(status().isCreated());
    }

    @Test void crossOrganizationReadLooksNotFound() throws Exception {
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-a").contentType(MediaType.APPLICATION_JSON).content(json(request("A", 0))))
            .andExpect(status().isCreated());
        String id = repository.findAll().get(0).getId();
        mvc.perform(get("/api/v1/l1/data-sources/" + id).header("X-Org-Code", "org-b"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("L1_NOT_FOUND"));
    }

    @Test void listSearchAndSummaryAreTenantScoped() throws Exception {
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-a").contentType(MediaType.APPLICATION_JSON).content(json(request("A", 0))))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/l1/data-sources/summary").header("X-Org-Code", "org-b"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/v1/l1/data-sources").header("X-Org-Code", "org-a").param("q", "Source"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
    }

    @Test void optimisticConflictReturns409() throws Exception {
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-a").contentType(MediaType.APPLICATION_JSON).content(json(request("A", 0))))
            .andExpect(status().isCreated());
        String id = repository.findAll().get(0).getId();
        mvc.perform(put("/api/v1/l1/data-sources/" + id).header("X-Org-Code", "org-a").contentType(MediaType.APPLICATION_JSON).content(json(request("A", 4))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("L1_VERSION_CONFLICT"));
    }

    @Test void templateDownloadsWithExpectedHeaders() throws Exception {
        mvc.perform(get("/api/v1/l1/templates/BATTERY_MODEL"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename*=UTF-8''%E7%94%B5%E6%B1%A0%E5%9E%8B%E5%8F%B7%E6%A8%A1%E6%9D%BF.csv")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith("text/csv"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("模板版本,来源系统,来源记录编号,组织编码")));
    }

    @Test void unknownTemplateReturnsEnvelope() throws Exception {
        mvc.perform(get("/api/v1/l1/templates/unknown"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("L1_TEMPLATE_NOT_FOUND"));
    }

    @Test void invalidRequestHasFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/l1/data-sources").header("X-Org-Code", "org-a").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("L1_VALIDATION_ERROR"));
    }

    private DataSourceRequest request(String code, long version) {
        return new DataSourceRequest(code, "A Source", SourceType.SUPPLIER, "Battery Team", "owner@example.com",
            Set.of(DataDomain.MODEL), DeliveryMethod.FILE_TEMPLATE, DataFrequency.ON_CHANGE, AuthMethod.MANUAL,
            Set.of(TemplateType.BATTERY_MODEL), SourceStatus.DRAFT, "Test", version);
    }
    private String json(DataSourceRequest request) throws Exception { return objectMapper.writeValueAsString(request); }
}
