package com.google.clusterfuzz.web.controller;

import com.google.clusterfuzz.web.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;

/**
 * Base controller providing common functionality for all REST API controllers.
 * Includes standardized response handling, pagination, logging, and error management.
 */
public abstract class BaseController {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    // Default pagination constants
    protected static final int DEFAULT_PAGE_SIZE = 20;
    protected static final int MAX_PAGE_SIZE = 100;
    protected static final String DEFAULT_SORT_FIELD = "id";
    protected static final String DEFAULT_SORT_DIRECTION = "desc";
    
    /**
     * Create successful response with data
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    /**
     * Create successful response with data and custom message
     */
    protected <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }
    
    /**
     * Create successful response with paginated data
     */
    protected <T> ResponseEntity<ApiResponse<List<T>>> success(Page<T> page) {
        ApiResponse.PaginationInfo pagination = new ApiResponse.PaginationInfo(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
        return ResponseEntity.ok(ApiResponse.success(page.getContent(), pagination));
    }
    
    /**
     * Create created response (201)
     */
    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(data, "Resource created successfully"));
    }
    
    /**
     * Create no content response (204)
     */
    protected ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Create bad request error response (400)
     */
    protected <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("BAD_REQUEST", message));
    }
    
    /**
     * Create bad request error response with details (400)
     */
    protected <T> ResponseEntity<ApiResponse<T>> badRequest(String message, Object details) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("BAD_REQUEST", message, details));
    }
    
    /**
     * Create unauthorized error response (401)
     */
    protected <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("UNAUTHORIZED", message));
    }
    
    /**
     * Create forbidden error response (403)
     */
    protected <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("FORBIDDEN", message));
    }
    
    /**
     * Create not found error response (404)
     */
    protected <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("NOT_FOUND", message));
    }
    
    /**
     * Create conflict error response (409)
     */
    protected <T> ResponseEntity<ApiResponse<T>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("CONFLICT", message));
    }
    
    /**
     * Create internal server error response (500)
     */
    protected <T> ResponseEntity<ApiResponse<T>> internalError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", message));
    }
    
    /**
     * Create internal server error response with details (500)
     */
    protected <T> ResponseEntity<ApiResponse<T>> internalError(String message, Object details) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", message, details));
    }
    
    /**
     * Create pageable object from request parameters
     */
    protected Pageable createPageable(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = DEFAULT_SORT_FIELD) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIRECTION) String sortDir) {
        
        // Validate and limit page size
        size = Math.min(size, MAX_PAGE_SIZE);
        size = Math.max(size, 1);
        
        // Create sort object
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * Log API request
     */
    protected void logRequest(HttpServletRequest request, String operation) {
        logger.info("API Request: {} {} - Operation: {} - User: {} - IP: {}",
            request.getMethod(),
            request.getRequestURI(),
            operation,
            getCurrentUser(),
            getClientIpAddress(request)
        );
    }
    
    /**
     * Log API response
     */
    protected void logResponse(String operation, boolean success, long duration) {
        if (success) {
            logger.info("API Response: {} - Success - Duration: {}ms", operation, duration);
        } else {
            logger.warn("API Response: {} - Failed - Duration: {}ms", operation, duration);
        }
    }
    
    /**
     * Get current authenticated user
     */
    protected String getCurrentUser() {
        // TODO: Implement with Spring Security context
        return "system"; // Placeholder
    }
    
    /**
     * Get client IP address from request
     */
    protected String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Validate required parameter
     */
    protected void validateRequired(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + paramName + "' is missing");
        }
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            throw new IllegalArgumentException("Required parameter '" + paramName + "' cannot be empty");
        }
    }
    
    /**
     * Validate ID parameter
     */
    protected void validateId(Long id, String entityName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid " + entityName + " ID: " + id);
        }
    }
    
    /**
     * Execute operation with timing and logging
     */
    protected <T> ResponseEntity<ApiResponse<T>> executeWithLogging(
            HttpServletRequest request, 
            String operation, 
            OperationExecutor<T> executor) {
        
        long startTime = System.currentTimeMillis();
        logRequest(request, operation);
        
        try {
            ResponseEntity<ApiResponse<T>> response = executor.execute();
            long duration = System.currentTimeMillis() - startTime;
            logResponse(operation, response.getStatusCode().is2xxSuccessful(), duration);
            return response;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("API Error: {} - Error: {} - Duration: {}ms", operation, e.getMessage(), duration, e);
            logResponse(operation, false, duration);
            return internalError("Internal server error occurred", e.getMessage());
        }
    }
    
    /**
     * Functional interface for operation execution
     */
    @FunctionalInterface
    protected interface OperationExecutor<T> {
        ResponseEntity<ApiResponse<T>> execute() throws Exception;
    }
}