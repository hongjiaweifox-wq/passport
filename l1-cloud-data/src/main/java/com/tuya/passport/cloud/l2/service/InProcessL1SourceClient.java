package com.tuya.passport.cloud.l2.service;

import com.tuya.passport.cloud.l1.domain.DataSourceEntity;
import com.tuya.passport.cloud.l1.repository.DataSourceRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InProcessL1SourceClient implements L1SourceClient {
    private final DataSourceRepository sources;

    public InProcessL1SourceClient(DataSourceRepository sources) {
        this.sources = sources;
    }

    @Override
    @Transactional(readOnly = true)
    public List<L1Source> list(String orgCode) {
        return sources.findAllByOrgCodeOrderByUpdatedAtDesc(orgCode).stream()
            .map(InProcessL1SourceClient::toSource)
            .toList();
    }

    @Override
    public L1Source requireByCode(String orgCode, String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        List<L1Source> matches = list(orgCode).stream()
            .filter(source -> source.sourceCode() != null && source.sourceCode().equalsIgnoreCase(normalized))
            .toList();
        if (matches.isEmpty()) {
            throw new com.tuya.passport.cloud.l2.support.ApiException(
                "INGEST_SOURCE_NOT_REGISTERED", "该数据源未在 L1 登记", HttpStatus.BAD_REQUEST);
        }
        if (matches.size() > 1) {
            throw new com.tuya.passport.cloud.l2.support.ApiException(
                "INGEST_SOURCE_AMBIGUOUS", "L1 返回多个相同数据源代码，无法安全选择", HttpStatus.CONFLICT);
        }
        return matches.get(0);
    }

    private static L1Source toSource(DataSourceEntity source) {
        Set<String> templateTypes = source.getTemplateTypes().stream().map(Enum::name)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new L1Source(source.getId(), source.getOrgCode(), source.getSourceCode(), source.getSourceName(),
            source.getSourceType().name(), source.getStatus().name(), templateTypes);
    }
}
