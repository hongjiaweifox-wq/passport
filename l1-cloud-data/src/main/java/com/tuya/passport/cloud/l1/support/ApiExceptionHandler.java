package com.tuya.passport.cloud.l1.support;

import com.tuya.passport.cloud.l1.api.dto.ApiError;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackages = "com.tuya.passport.cloud.l1.api")
public class ApiExceptionHandler {
    private final TraceIdProvider traceIdProvider;
    public ApiExceptionHandler(TraceIdProvider traceIdProvider) { this.traceIdProvider = traceIdProvider; }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(error(ex.getCode(), ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(error("L1_VALIDATION_ERROR", "Request validation failed", fields));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> unreadable(Exception ex) {
        return ResponseEntity.badRequest().body(error("L1_VALIDATION_ERROR", "Invalid request value", Map.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> responseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = status == HttpStatus.NOT_FOUND ? "L1_NOT_FOUND" : "L1_VALIDATION_ERROR";
        return ResponseEntity.status(status).body(error(code, ex.getReason() == null ? "Request failed" : ex.getReason(), Map.of()));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ApiError> optimistic(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("L1_VERSION_CONFLICT", "Data source was changed; reload and retry", Map.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("L1_SOURCE_CODE_DUPLICATE", "Source code already exists in this organization", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> internal(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("L1_INTERNAL_ERROR", "Unexpected server error", Map.of()));
    }

    private ApiError error(String code, String message, Map<String, String> fields) {
        return new ApiError(code, message, traceIdProvider.current(), fields);
    }
}
