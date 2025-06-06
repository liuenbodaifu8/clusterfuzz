package com.google.clusterfuzz.web.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom API exception for handling application-specific errors.
 * Provides structured error information for consistent API responses.
 */
public class ApiException extends RuntimeException {
    
    private final HttpStatus status;
    private final String code;
    private final Object details;
    
    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = null;
    }
    
    public ApiException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }
    
    public ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.details = null;
    }
    
    public ApiException(HttpStatus status, String code, String message, Object details, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.details = details;
    }
    
    // Static factory methods for common error types
    
    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
    
    public static ApiException badRequest(String code, String message, Object details) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message, details);
    }
    
    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }
    
    public static ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }
    
    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }
    
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
    
    public static ApiException conflict(String code, String message, Object details) {
        return new ApiException(HttpStatus.CONFLICT, code, message, details);
    }
    
    public static ApiException internalError(String code, String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }
    
    public static ApiException internalError(String code, String message, Throwable cause) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, code, message, cause);
    }
    
    // Getters
    public HttpStatus getStatus() {
        return status;
    }
    
    public String getCode() {
        return code;
    }
    
    public Object getDetails() {
        return details;
    }
}