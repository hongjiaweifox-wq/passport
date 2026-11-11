package com.tuya.passport.cloud.l1.api;

import com.tuya.passport.cloud.l1.api.dto.ApiEnvelope;
import com.tuya.passport.cloud.l1.api.dto.DataSourceRequest;
import com.tuya.passport.cloud.l1.api.dto.DataSourceResponse;
import com.tuya.passport.cloud.l1.api.dto.DataSourceSummary;
import com.tuya.passport.cloud.l1.api.dto.PageResponse;
import com.tuya.passport.cloud.l1.api.dto.StatusRequest;
import com.tuya.passport.cloud.l1.domain.SourceStatus;
import com.tuya.passport.cloud.l1.service.DataSourceService;
import com.tuya.passport.cloud.l1.support.TraceIdProvider;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/l1/data-sources")
public class DataSourceController {
    private final DataSourceService service;
    private final TraceIdProvider traceIdProvider;
    public DataSourceController(DataSourceService service, TraceIdProvider traceIdProvider) {
        this.service = service; this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public ApiEnvelope<PageResponse<DataSourceResponse>> list(
        @RequestHeader(value = "X-Org-Code", required = false) String headerOrg,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        String org = org(headerOrg);
        validatePage(page, size);
        Page<DataSourceResponse> result = service.list(org, q, type, status, page - 1, size).map(DataSourceResponse::from);
        return new ApiEnvelope<>(new PageResponse<>(result.getContent(), result.getTotalElements(), page, size), traceIdProvider.current());
    }

    @GetMapping("/summary")
    public ApiEnvelope<DataSourceSummary> summary(@RequestHeader(value = "X-Org-Code", required = false) String headerOrg) {
        return new ApiEnvelope<>(service.summary(org(headerOrg)), traceIdProvider.current());
    }

    @GetMapping("/{id}")
    public ApiEnvelope<DataSourceResponse> get(@RequestHeader(value = "X-Org-Code", required = false) String headerOrg,
                                               @PathVariable String id) {
        return new ApiEnvelope<>(DataSourceResponse.from(service.get(org(headerOrg), id)), traceIdProvider.current());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<DataSourceResponse> create(@RequestHeader(value = "X-Org-Code", required = false) String headerOrg,
                                                   @Valid @RequestBody DataSourceRequest request) {
        return new ApiEnvelope<>(DataSourceResponse.from(service.create(org(headerOrg), request)), traceIdProvider.current());
    }

    @PutMapping("/{id}")
    public ApiEnvelope<DataSourceResponse> update(@RequestHeader(value = "X-Org-Code", required = false) String headerOrg,
                                                   @PathVariable String id, @Valid @RequestBody DataSourceRequest request) {
        if (request.version() == null) {
            throw new com.tuya.passport.cloud.l1.support.ApiException(
                "L1_VALIDATION_ERROR", "version is required for updates", HttpStatus.BAD_REQUEST);
        }
        return new ApiEnvelope<>(DataSourceResponse.from(service.update(org(headerOrg), id, request)), traceIdProvider.current());
    }

    @PatchMapping("/{id}/status")
    public ApiEnvelope<DataSourceResponse> status(@RequestHeader(value = "X-Org-Code", required = false) String headerOrg,
                                                  @PathVariable String id, @Valid @RequestBody StatusRequest request) {
        return new ApiEnvelope<>(DataSourceResponse.from(service.changeStatus(org(headerOrg), id, request.status(), request.version())), traceIdProvider.current());
    }

    @GetMapping("/enums")
    public ApiEnvelope<Map<String, Object>> enums() {
        return new ApiEnvelope<>(Map.of(
            "sourceTypes", names(com.tuya.passport.cloud.l1.domain.SourceType.values()),
            "deliveryMethods", names(com.tuya.passport.cloud.l1.domain.DeliveryMethod.values()),
            "frequencies", names(com.tuya.passport.cloud.l1.domain.DataFrequency.values()),
            "authMethods", names(com.tuya.passport.cloud.l1.domain.AuthMethod.values()),
            "dataDomains", names(com.tuya.passport.cloud.l1.domain.DataDomain.values()),
            "templateTypes", names(com.tuya.passport.cloud.l1.domain.TemplateType.values()),
            "statuses", names(SourceStatus.values())), traceIdProvider.current());
    }

    private static java.util.List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
    private static String org(String raw) {
        if (raw == null) return "DEMO-ORG";
        String value = raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (!value.matches("[A-Z0-9_-]{1,64}")) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-Org-Code");
        return value;
    }
    private static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 1 and size must be 1..100");
    }
}
