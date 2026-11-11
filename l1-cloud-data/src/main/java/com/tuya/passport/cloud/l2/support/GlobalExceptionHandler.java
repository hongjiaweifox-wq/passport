package com.tuya.passport.cloud.l2.support;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackages = "com.tuya.passport.cloud.l2.api")
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handle(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.status()).body(new ApiError(ex.code(), ex.getMessage(), trace(request), Map.of()));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handle(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(new ApiError("L2_VALIDATION_ERROR", ex.getReason() == null ? "Invalid request" : ex.getReason(), trace(request), Map.of()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleSize(MaxUploadSizeExceededException ex,HttpServletRequest request){return ResponseEntity.status(413).body(new ApiError("L2_FILE_TOO_LARGE","文件不能超过 20MB",trace(request),Map.of()));}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,HttpServletRequest request){Map<String,String> fields=new LinkedHashMap<>();for(FieldError error:ex.getBindingResult().getFieldErrors())fields.put(error.getField(),error.getDefaultMessage());return ResponseEntity.badRequest().body(new ApiError("L2_VALIDATION_ERROR","请求参数校验失败",trace(request),fields));}
    @ExceptionHandler({MissingServletRequestParameterException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleRequest(Exception ex,HttpServletRequest request){return ResponseEntity.badRequest().body(new ApiError("L2_VALIDATION_ERROR","请求参数无效",trace(request),Map.of()));}
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(500).body(new ApiError("L2_INTERNAL_ERROR", "The request could not be completed", trace(request), Map.of()));
    }
    private String trace(HttpServletRequest request) { Object value = request.getAttribute("traceId"); return value == null ? UUID.randomUUID().toString() : value.toString(); }
}
