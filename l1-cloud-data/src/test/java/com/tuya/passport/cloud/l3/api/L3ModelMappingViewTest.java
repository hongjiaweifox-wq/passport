package com.tuya.passport.cloud.l3.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tuya.passport.cloud.l3.domain.BatteryModelEntity;
import com.tuya.passport.cloud.l3.domain.ModelRevisionEntity;
import com.tuya.passport.cloud.l3.repository.BatteryModelRepository;
import com.tuya.passport.cloud.l3.repository.ModelRevisionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class L3ModelMappingViewTest {
    private static final String ORG = "MODEL-MAPPING-TEST";

    @Autowired MockMvc mvc;
    @Autowired BatteryModelRepository models;
    @Autowired ModelRevisionRepository revisions;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:l3modelmappingview;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }

    @BeforeEach
    void reset() {
        revisions.deleteAll();
        models.deleteAll();
        var model = new BatteryModelEntity(ORG, ORG, "MODEL-C", "Maker", "STATIONARY");
        model.setSnProductCode("C");
        models.save(model);
        revisions.save(new ModelRevisionEntity(ORG, model.getId(), "R1", "dpp-schema/1.0", new BigDecimal("2.0"), new BigDecimal("51.2"), "LFP", "{}", "manual", "hash", LocalDate.of(2025, 1, 1)));
    }

    @Test
    void modelListAndDetailExposeProductCodeNeededForSnMapping() throws Exception {
        mvc.perform(get("/api/v1/l3/models").header("X-Org-Code", ORG).header("X-User", "viewer").param("page", "1").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].snProductCode").value("C"));
        mvc.perform(get("/api/v1/l3/models/{id}", models.findByOrgCodeAndIssuerCodeAndModelCode(ORG, ORG, "MODEL-C").orElseThrow().getId())
                        .header("X-Org-Code", ORG).header("X-User", "viewer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.model.snProductCode").value("C"));
    }

    @Test
    void snRuleEndpointExposesDecoderRuleTables() throws Exception {
        mvc.perform(get("/api/v1/l3/sn-rule").header("X-Org-Code", ORG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length").value(16))
                .andExpect(jsonPath("$.data.segments.length()").value(10))
                .andExpect(jsonPath("$.data.segments[0].position").value("X1"))
                .andExpect(jsonPath("$.data.codeTables[0].key").value("scheme"))
                .andExpect(jsonPath("$.data.codeTables[1].options.length()").value(17))
                .andExpect(jsonPath("$.data.codeTables[1].options[0].code").value("A"))
                .andExpect(jsonPath("$.data.codeTables[4].key").value("cell"));
    }
}
