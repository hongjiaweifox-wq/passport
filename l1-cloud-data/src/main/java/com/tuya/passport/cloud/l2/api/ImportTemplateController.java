package com.tuya.passport.cloud.l2.api;

import com.tuya.passport.cloud.l2.domain.EntityType;
import com.tuya.passport.cloud.l2.support.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/l2/templates")
public class ImportTemplateController {
    @GetMapping("/{type}") public ResponseEntity<byte[]> template(@PathVariable String type){
        EntityType entity;try{entity=EntityType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));}catch(Exception ex){throw new com.tuya.passport.cloud.l2.support.ApiException("INGEST_SCHEMA_MISMATCH","Unknown template type",org.springframework.http.HttpStatus.NOT_FOUND);}
        String body="﻿"+String.join(",",entity.templateHeaders())+"\r\n";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(entity.downloadFilename(),StandardCharsets.UTF_8).build().toString()).body(body.getBytes(StandardCharsets.UTF_8));
    }
}
