package com.google.clusterfuzz.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Configuration entity for storing application settings.
 */
@Entity
@Table(name = "configurations", indexes = {
    @Index(name = "idx_config_key", columnList = "config_key"),
    @Index(name = "idx_config_category", columnList = "category"),
    @Index(name = "idx_config_environment", columnList = "environment"),
    @Index(name = "idx_config_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode
@ToString
public class Configuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, length = 100)
    @NotBlank(message = "Configuration key is required")
    @Size(max = 100, message = "Configuration key must not exceed 100 characters")
    private String key;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Column(name = "category", length = 50)
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @Column(name = "data_type", length = 20)
    @Size(max = 20, message = "Data type must not exceed 20 characters")
    private String dataType = "STRING";

    @Column(name = "environment", length = 20)
    @Size(max = 20, message = "Environment must not exceed 20 characters")
    private String environment = "ALL";

    @Column(name = "sensitive", nullable = false)
    private Boolean sensitive = false;

    @Column(name = "encrypted", nullable = false)
    private Boolean encrypted = false;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "system_config", nullable = false)
    private Boolean systemConfig = false;

    @Column(name = "validation_regex", length = 500)
    @Size(max = 500, message = "Validation regex must not exceed 500 characters")
    private String validationRegex;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "allowed_values", columnDefinition = "TEXT")
    private String allowedValues;

    @Column(name = "created_by", length = 100)
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    @Size(max = 100, message = "Updated by must not exceed 100 characters")
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Gets the effective value (value if set, otherwise default value).
     */
    public String getEffectiveValue() {
        return value != null ? value : defaultValue;
    }

    /**
     * Gets value as boolean.
     */
    public Boolean getBooleanValue() {
        String effectiveValue = getEffectiveValue();
        if (effectiveValue == null) {
            return null;
        }
        return Boolean.parseBoolean(effectiveValue);
    }

    /**
     * Gets value as integer.
     */
    public Integer getIntegerValue() {
        String effectiveValue = getEffectiveValue();
        if (effectiveValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(effectiveValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets value as long.
     */
    public Long getLongValue() {
        String effectiveValue = getEffectiveValue();
        if (effectiveValue == null) {
            return null;
        }
        try {
            return Long.parseLong(effectiveValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets value as double.
     */
    public Double getDoubleValue() {
        String effectiveValue = getEffectiveValue();
        if (effectiveValue == null) {
            return null;
        }
        try {
            return Double.parseDouble(effectiveValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks if value is valid according to constraints.
     */
    public boolean isValidValue(String testValue) {
        if (testValue == null) {
            return true; // Null values are handled separately
        }

        // Check regex validation
        if (validationRegex != null && !testValue.matches(validationRegex)) {
            return false;
        }

        // Check allowed values
        if (allowedValues != null) {
            String[] allowed = allowedValues.split(",");
            boolean found = false;
            for (String allowedValue : allowed) {
                if (allowedValue.trim().equals(testValue)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        // Check numeric constraints
        if ("INTEGER".equals(dataType) || "LONG".equals(dataType) || "DOUBLE".equals(dataType)) {
            try {
                double numValue = Double.parseDouble(testValue);
                if (minValue != null && numValue < minValue) {
                    return false;
                }
                if (maxValue != null && numValue > maxValue) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Pre-persist callback to set default values.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (sensitive == null) {
            sensitive = false;
        }
        if (encrypted == null) {
            encrypted = false;
        }
        if (systemConfig == null) {
            systemConfig = false;
        }
        if (dataType == null) {
            dataType = "STRING";
        }
        if (environment == null) {
            environment = "ALL";
        }
    }

    /**
     * Pre-update callback to update timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Configuration data types.
     */
    public static class DataType {
        public static final String STRING = "STRING";
        public static final String INTEGER = "INTEGER";
        public static final String LONG = "LONG";
        public static final String DOUBLE = "DOUBLE";
        public static final String BOOLEAN = "BOOLEAN";
        public static final String JSON = "JSON";
        public static final String URL = "URL";
        public static final String EMAIL = "EMAIL";
        public static final String PASSWORD = "PASSWORD";
    }

    /**
     * Configuration categories.
     */
    public static class Category {
        public static final String SECURITY = "security";
        public static final String FUZZING = "fuzzing";
        public static final String STORAGE = "storage";
        public static final String NOTIFICATION = "notification";
        public static final String INTEGRATION = "integration";
        public static final String PERFORMANCE = "performance";
        public static final String LOGGING = "logging";
        public static final String SYSTEM = "system";
    }

    /**
     * Environment types.
     */
    public static class Environment {
        public static final String ALL = "ALL";
        public static final String DEVELOPMENT = "DEVELOPMENT";
        public static final String STAGING = "STAGING";
        public static final String PRODUCTION = "PRODUCTION";
    }
}