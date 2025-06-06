package com.google.clusterfuzz.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/**
 * Standardized API response wrapper for all REST endpoints.
 * Provides consistent response format across the entire API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("data")
    private T data;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("error")
    private ErrorDetails error;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("requestId")
    private String requestId;
    
    @JsonProperty("pagination")
    private PaginationInfo pagination;
    
    // Private constructor to enforce builder pattern
    private ApiResponse() {
        this.timestamp = Instant.now();
        this.requestId = UUID.randomUUID().toString();
    }
    
    /**
     * Create successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = "Operation completed successfully";
        return response;
    }
    
    /**
     * Create successful response with data and custom message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = message;
        return response;
    }
    
    /**
     * Create successful response with data and pagination
     */
    public static <T> ApiResponse<T> success(T data, PaginationInfo pagination) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.message = "Operation completed successfully";
        response.pagination = pagination;
        return response;
    }
    
    /**
     * Create error response
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = new ErrorDetails(code, message);
        return response;
    }
    
    /**
     * Create error response with details
     */
    public static <T> ApiResponse<T> error(String code, String message, Object details) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = new ErrorDetails(code, message, details);
        return response;
    }
    
    // Getters and setters
    public boolean isSuccess() {
        return success;
    }
    
    public T getData() {
        return data;
    }
    
    public String getMessage() {
        return message;
    }
    
    public ErrorDetails getError() {
        return error;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public PaginationInfo getPagination() {
        return pagination;
    }
    
    public ApiResponse<T> withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
    
    /**
     * Error details structure
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("details")
        private Object details;
        
        public ErrorDetails(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public ErrorDetails(String code, String message, Object details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
        
        // Getters
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Object getDetails() { return details; }
    }
    
    /**
     * Pagination information structure
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationInfo {
        @JsonProperty("page")
        private int page;
        
        @JsonProperty("size")
        private int size;
        
        @JsonProperty("totalElements")
        private long totalElements;
        
        @JsonProperty("totalPages")
        private int totalPages;
        
        @JsonProperty("hasNext")
        private boolean hasNext;
        
        @JsonProperty("hasPrevious")
        private boolean hasPrevious;
        
        public PaginationInfo(int page, int size, long totalElements) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = (int) Math.ceil((double) totalElements / size);
            this.hasNext = page < totalPages - 1;
            this.hasPrevious = page > 0;
        }
        
        // Getters
        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
        public boolean isHasNext() { return hasNext; }
        public boolean isHasPrevious() { return hasPrevious; }
    }
}