package com.tuya.passport.cloud.common.support;

import com.tuya.passport.cloud.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(value = {"com.tuya.passport.cloud.l3.api", "com.tuya.passport.cloud.l4.api", "com.tuya.passport.cloud.l5.api"}, name = "layerApiExceptionHandler")
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handle(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.status()).body(new ApiError(ex.code(), ex.getMessage(), trace(request), Map.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> internal(Exception ex, HttpServletRequest request) {
        log.error("Unhandled API error for trace {}", trace(request), ex);
        return ResponseEntity.internalServerError().body(new ApiError("API_INTERNAL_ERROR", "The request could not be completed", trace(request), Map.of()));
    }

    private String trace(HttpServletRequest request) {
        Object trace = request.getAttribute("traceId");
        return trace == null ? "" : trace.toString();
    }
}
