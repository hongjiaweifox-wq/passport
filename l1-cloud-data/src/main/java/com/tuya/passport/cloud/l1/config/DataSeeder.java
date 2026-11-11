package com.tuya.passport.cloud.l1.config;

import com.tuya.passport.cloud.l1.domain.AuthMethod;
import com.tuya.passport.cloud.l1.domain.DataDomain;
import com.tuya.passport.cloud.l1.domain.DataFrequency;
import com.tuya.passport.cloud.l1.domain.DataSourceEntity;
import com.tuya.passport.cloud.l1.domain.DeliveryMethod;
import com.tuya.passport.cloud.l1.domain.SourceStatus;
import com.tuya.passport.cloud.l1.domain.SourceType;
import com.tuya.passport.cloud.l1.domain.TemplateType;
import com.tuya.passport.cloud.l1.repository.DataSourceRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class DataSeeder {
    @Bean
    CommandLineRunner seed(@Value("${l1.demo.seed-enabled:true}") boolean enabled, DataSourceRepository repository) {
        return args -> {
            if (!enabled || repository.count() > 0) return;
            repository.saveAll(java.util.List.of(
                source("TUYA-IOT", "Tuya IoT 设备云", SourceType.TUYA_IOT, "IoT 产品团队", "iot@example.com", Set.of(DataDomain.UNIT, DataDomain.IOT_TELEMETRY), DeliveryMethod.IOT_API, DataFrequency.DAILY, AuthMethod.OIDC_CC, Set.of(TemplateType.BATTERY_UNIT), SourceStatus.ACTIVE, "设备关联与授权运行数据来源；首期只登记接入责任与数据域。"),
                source("SUPPLIER-PLM", "电池制造商 / 供应链", SourceType.SUPPLIER, "电池供应链", "supplier@example.com", Set.of(DataDomain.MODEL, DataDomain.UNIT), DeliveryMethod.FILE_TEMPLATE, DataFrequency.ON_CHANGE, AuthMethod.MANUAL, Set.of(TemplateType.BATTERY_MODEL, TemplateType.BATTERY_UNIT), SourceStatus.ACTIVE, "提供电池型号、批次、单体序列号及基础技术信息。"),
                source("BRAND-ERP", "品牌 ERP / PLM", SourceType.ERP_PLM, "欧洲产品运营", "product@example.com", Set.of(DataDomain.MODEL), DeliveryMethod.API, DataFrequency.DAILY, AuthMethod.OIDC_CC, Set.of(TemplateType.BATTERY_MODEL), SourceStatus.DRAFT, "维护 SKU、电池型号关联、目标市场与责任主体。"),
                source("TEST-LAB", "测试与合规资料", SourceType.TEST_COMPLIANCE, "产品合规", "compliance@example.com", Set.of(DataDomain.MODEL, DataDomain.EVIDENCE), DeliveryMethod.FILE_TEMPLATE, DataFrequency.ON_CHANGE, AuthMethod.MANUAL, Set.of(TemplateType.BATTERY_MODEL), SourceStatus.ACTIVE, "性能、耐久、碳足迹及相关证据资料。"),
                source("SERVICE-PARTNER", "服务与回收伙伴", SourceType.SERVICE_PARTNER, "售后与循环利用", "service@example.com", Set.of(DataDomain.LIFECYCLE, DataDomain.RECYCLING, DataDomain.EVIDENCE), DeliveryMethod.MANUAL, DataFrequency.AD_HOC, AuthMethod.MANUAL, Set.of(TemplateType.LIFECYCLE_EVENT), SourceStatus.DRAFT, "后续登记维修、更换、再利用及回收事件来源。")
            ));
        };
    }

    private static DataSourceEntity source(String code, String name, SourceType type, String owner, String contact,
            Set<DataDomain> domains, DeliveryMethod method, DataFrequency frequency, AuthMethod auth,
            Set<TemplateType> templates, SourceStatus status, String description) {
        return new DataSourceEntity("DEMO-ORG", code, name, type, owner, contact,
            new LinkedHashSet<>(domains), method, frequency, auth, new LinkedHashSet<>(templates), status, description);
    }
}
