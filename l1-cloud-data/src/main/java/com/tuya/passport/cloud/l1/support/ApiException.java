package com.tuya.passport.cloud.l1.support;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    public ApiException(String code, String message, HttpStatus status) {
        super(message); this.code = code; this.status = status;
    }
    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }
    public static ApiException notFound() { return new ApiException("L1_NOT_FOUND", "Data source not found", HttpStatus.NOT_FOUND); }
}
