package com.tuya.passport.cloud.l1.service;

import com.tuya.passport.cloud.l1.api.dto.DataSourceRequest;
import com.tuya.passport.cloud.l1.api.dto.DataSourceSummary;
import com.tuya.passport.cloud.l1.domain.DataSourceEntity;
import com.tuya.passport.cloud.l1.domain.SourceStatus;
import com.tuya.passport.cloud.l1.repository.DataSourceRepository;
import com.tuya.passport.cloud.l1.support.ApiException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DataSourceService {
    private final DataSourceRepository repository;
    public DataSourceService(DataSourceRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public Page<DataSourceEntity> list(String orgCode, String keyword, String type, String status, int page, int size) {
        Specification<DataSourceEntity> spec = (root, query, cb) -> cb.equal(root.get("orgCode"), orgCode);
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("sourceName")), pattern, '\\'),
                cb.like(cb.lower(root.get("sourceCode")), pattern, '\\'),
                cb.like(cb.lower(root.get("ownerTeam")), pattern, '\\')));
        }
        if (type != null && !type.isBlank()) {
            var sourceType = parseEnum(com.tuya.passport.cloud.l1.domain.SourceType.class, type, "sourceType");
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sourceType"), sourceType));
        }
        if (status != null && !status.isBlank()) {
            var sourceStatus = parseEnum(SourceStatus.class, status, "status");
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), sourceStatus));
        }
        return repository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @Transactional(readOnly = true)
    public DataSourceSummary summary(String orgCode) {
        return new DataSourceSummary(repository.countByOrgCode(orgCode),
            repository.countByOrgCodeAndStatus(orgCode, SourceStatus.ACTIVE),
            repository.countByOrgCodeAndStatus(orgCode, SourceStatus.DRAFT),
            repository.countByOrgCodeAndStatus(orgCode, SourceStatus.SUSPENDED));
    }

    @Transactional(readOnly = true)
    public DataSourceEntity get(String orgCode, String id) {
        return repository.findByIdAndOrgCode(id, orgCode).orElseThrow(ApiException::notFound);
    }

    public DataSourceEntity create(String orgCode, DataSourceRequest request) {
        String code = normalizeCode(request.sourceCode());
        if (repository.existsByOrgCodeAndSourceCode(orgCode, code)) duplicate();
        var entity = new DataSourceEntity(orgCode, code, request.sourceName(), request.sourceType(),
            request.ownerTeam(), request.contact(), request.dataDomains(), request.deliveryMethod(),
            request.frequency(), request.authMethod(), request.templateTypes(),
            request.status() == null ? SourceStatus.DRAFT : request.status(), request.description());
        try { return repository.saveAndFlush(entity); }
        catch (DataIntegrityViolationException ex) { throw duplicateException(); }
    }

    public DataSourceEntity update(String orgCode, String id, DataSourceRequest request) {
        var entity = get(orgCode, id);
        if (request.version() == null || request.version() != entity.getVersion()) {
            throw new ApiException("L1_VERSION_CONFLICT", "Data source was changed; reload and retry", HttpStatus.CONFLICT);
        }
        String code = normalizeCode(request.sourceCode());
        if (repository.existsByOrgCodeAndSourceCodeAndIdNot(orgCode, code, id)) duplicate();
        entity.update(code, request.sourceName(), request.sourceType(), request.ownerTeam(), request.contact(),
            request.dataDomains(), request.deliveryMethod(), request.frequency(), request.authMethod(),
            request.templateTypes(), request.status(), request.description());
        try { return repository.saveAndFlush(entity); }
        catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            throw new ApiException("L1_VERSION_CONFLICT", "Data source was changed; reload and retry", HttpStatus.CONFLICT);
        } catch (DataIntegrityViolationException ex) { throw duplicateException(); }
    }

    public DataSourceEntity changeStatus(String orgCode, String id, SourceStatus status, Long version) {
        var entity = get(orgCode, id);
        if (version == null || version != entity.getVersion()) {
            throw new ApiException("L1_VERSION_CONFLICT", "Data source was changed; reload and retry", HttpStatus.CONFLICT);
        }
        entity.changeStatus(status);
        try { return repository.saveAndFlush(entity); }
        catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            throw new ApiException("L1_VERSION_CONFLICT", "Data source was changed; reload and retry", HttpStatus.CONFLICT);
        }
    }

    private static String normalizeCode(String sourceCode) { return sourceCode.trim().toUpperCase(Locale.ROOT); }
    private static String escapeLike(String value) { return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try { return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + field); }
    }
    private static void duplicate() { throw duplicateException(); }
    private static ApiException duplicateException() { return new ApiException("L1_SOURCE_CODE_DUPLICATE", "Source code already exists in this organization", HttpStatus.CONFLICT); }
}
