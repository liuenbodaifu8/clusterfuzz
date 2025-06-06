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
import java.util.HashMap;
import java.util.Map;

/**
 * Environment entity for tracking runtime environment configurations.
 * Based on clusterfuzz._internal.system.environment module.
 */
@Entity
@Table(name = "environments", indexes = {
    @Index(name = "idx_env_name", columnList = "name"),
    @Index(name = "idx_env_platform", columnList = "platform"),
    @Index(name = "idx_env_active", columnList = "active"),
    @Index(name = "idx_env_type", columnList = "environment_type")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode
@ToString
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Environment name is required")
    @Size(max = 100, message = "Environment name must not exceed 100 characters")
    private String name;

    @Column(name = "display_name", length = 200)
    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Column(name = "platform", length = 50)
    @Size(max = 50, message = "Platform must not exceed 50 characters")
    private String platform;

    @Column(name = "environment_type", length = 50)
    @Size(max = 50, message = "Environment type must not exceed 50 characters")
    private String environmentType;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "trusted_host", nullable = false)
    private Boolean trustedHost = false;

    @Column(name = "app_engine", nullable = false)
    private Boolean appEngine = false;

    @Column(name = "kubernetes", nullable = false)
    private Boolean kubernetes = false;

    @Column(name = "development", nullable = false)
    private Boolean development = false;

    @Column(name = "config_directory", length = 500)
    @Size(max = 500, message = "Config directory must not exceed 500 characters")
    private String configDirectory;

    @Column(name = "resources_directory", length = 500)
    @Size(max = 500, message = "Resources directory must not exceed 500 characters")
    private String resourcesDirectory;

    @Column(name = "root_directory", length = 500)
    @Size(max = 500, message = "Root directory must not exceed 500 characters")
    private String rootDirectory;

    @Column(name = "platform_id", length = 100)
    @Size(max = 100, message = "Platform ID must not exceed 100 characters")
    private String platformId;

    @Column(name = "os_override", length = 50)
    @Size(max = 50, message = "OS override must not exceed 50 characters")
    private String osOverride;

    @Column(name = "memory_tools", columnDefinition = "TEXT")
    private String memoryTools;

    @Column(name = "sanitizer_options", columnDefinition = "TEXT")
    private String sanitizerOptions;

    @Column(name = "environment_variables", columnDefinition = "TEXT")
    private String environmentVariables;

    @Column(name = "health_status", length = 20)
    @Size(max = 20, message = "Health status must not exceed 20 characters")
    private String healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    @Column(name = "health_details", columnDefinition = "TEXT")
    private String healthDetails;

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
     * Gets environment variables as a map.
     */
    public Map<String, String> getEnvironmentVariablesMap() {
        Map<String, String> envVars = new HashMap<>();
        if (environmentVariables != null && !environmentVariables.trim().isEmpty()) {
            String[] lines = environmentVariables.split("\n");
            for (String line : lines) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    envVars.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return envVars;
    }

    /**
     * Sets environment variables from a map.
     */
    public void setEnvironmentVariablesMap(Map<String, String> envVars) {
        if (envVars == null || envVars.isEmpty()) {
            this.environmentVariables = null;
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        this.environmentVariables = sb.toString();
    }

    /**
     * Gets sanitizer options as a map.
     */
    public Map<String, String> getSanitizerOptionsMap() {
        Map<String, String> options = new HashMap<>();
        if (sanitizerOptions != null && !sanitizerOptions.trim().isEmpty()) {
            String[] pairs = sanitizerOptions.split(":");
            for (String pair : pairs) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    options.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return options;
    }

    /**
     * Sets sanitizer options from a map.
     */
    public void setSanitizerOptionsMap(Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            this.sanitizerOptions = null;
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        this.sanitizerOptions = sb.toString();
    }

    /**
     * Checks if environment is healthy.
     */
    public boolean isHealthy() {
        return HealthStatus.HEALTHY.equals(healthStatus);
    }

    /**
     * Checks if environment is Android-based.
     */
    public boolean isAndroid() {
        return platform != null && (
            platform.toUpperCase().contains("ANDROID") ||
            Platform.ANDROID.equals(platform.toUpperCase())
        );
    }

    /**
     * Checks if environment is Windows-based.
     */
    public boolean isWindows() {
        return Platform.WINDOWS.equals(platform);
    }

    /**
     * Checks if environment is Linux-based.
     */
    public boolean isLinux() {
        return Platform.LINUX.equals(platform);
    }

    /**
     * Checks if environment is macOS-based.
     */
    public boolean isMac() {
        return Platform.MAC.equals(platform);
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
        if (trustedHost == null) {
            trustedHost = false;
        }
        if (appEngine == null) {
            appEngine = false;
        }
        if (kubernetes == null) {
            kubernetes = false;
        }
        if (development == null) {
            development = false;
        }
        if (healthStatus == null) {
            healthStatus = HealthStatus.UNKNOWN;
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
     * Platform types based on ClusterFuzz Python implementation.
     */
    public static class Platform {
        public static final String WINDOWS = "WINDOWS";
        public static final String LINUX = "LINUX";
        public static final String MAC = "MAC";
        public static final String ANDROID = "ANDROID";
        public static final String CHROMEOS = "CHROMEOS";
        public static final String FUCHSIA = "FUCHSIA";
    }

    /**
     * Environment types.
     */
    public static class EnvironmentType {
        public static final String PRODUCTION = "PRODUCTION";
        public static final String STAGING = "STAGING";
        public static final String DEVELOPMENT = "DEVELOPMENT";
        public static final String TESTING = "TESTING";
        public static final String LOCAL = "LOCAL";
    }

    /**
     * Health status values.
     */
    public static class HealthStatus {
        public static final String HEALTHY = "HEALTHY";
        public static final String UNHEALTHY = "UNHEALTHY";
        public static final String DEGRADED = "DEGRADED";
        public static final String UNKNOWN = "UNKNOWN";
    }

    /**
     * Memory tools supported by ClusterFuzz.
     */
    public static class MemoryTool {
        public static final String ASAN = "ASAN";
        public static final String MSAN = "MSAN";
        public static final String TSAN = "TSAN";
        public static final String UBSAN = "UBSAN";
        public static final String LSAN = "LSAN";
        public static final String KASAN = "KASAN";
        public static final String HWASAN = "HWASAN";
        public static final String CFI = "CFI";
        public static final String MTE = "MTE";
        public static final String NOSANITIZER = "NOSANITIZER";
    }
}