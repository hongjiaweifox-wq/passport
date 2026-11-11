package com.tuya.passport.cloud.l1.api;

import com.tuya.passport.cloud.l1.api.dto.ApiError;
import com.tuya.passport.cloud.l1.domain.TemplateType;
import com.tuya.passport.cloud.l1.support.ApiException;
import com.tuya.passport.cloud.l1.support.TraceIdProvider;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/l1/templates")
public class TemplateController {
    private final TraceIdProvider traceIdProvider;
    public TemplateController(TraceIdProvider traceIdProvider) { this.traceIdProvider = traceIdProvider; }

    @GetMapping("/{templateType}")
    public ResponseEntity<?> download(@PathVariable String templateType) {
        final TemplateType type;
        try { type = TemplateType.valueOf(templateType.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("L1_TEMPLATE_NOT_FOUND", "Template not found", traceIdProvider.current(), Map.of()));
        }
        Resource resource = new ClassPathResource("templates/" + type.filename());
        if (!resource.exists()) throw new ApiException("L1_TEMPLATE_NOT_FOUND", "Template not found", HttpStatus.NOT_FOUND);
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(type.downloadFilename(), StandardCharsets.UTF_8).build().toString())
            .body(resource);
    }
}
