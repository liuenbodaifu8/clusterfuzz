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
import java.util.HashSet;
import java.util.Set;

/**
 * Feature flag entity for controlling feature rollouts and A/B testing.
 */
@Entity
@Table(name = "feature_flags", indexes = {
    @Index(name = "idx_feature_flag_key", columnList = "flag_key"),
    @Index(name = "idx_feature_flag_enabled", columnList = "enabled"),
    @Index(name = "idx_feature_flag_environment", columnList = "environment"),
    @Index(name = "idx_feature_flag_rollout", columnList = "rollout_percentage")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode(exclude = {"targetUsers", "targetRoles"})
@ToString(exclude = {"targetUsers", "targetRoles"})
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Feature flag key is required")
    @Size(max = 100, message = "Feature flag key must not exceed 100 characters")
    private String key;

    @Column(name = "name", length = 100)
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Column(name = "environment", length = 20)
    @Size(max = 20, message = "Environment must not exceed 20 characters")
    private String environment = "ALL";

    @Column(name = "rollout_percentage", nullable = false)
    private Integer rolloutPercentage = 0;

    @Column(name = "flag_type", length = 20)
    @Size(max = 20, message = "Flag type must not exceed 20 characters")
    private String flagType = "BOOLEAN";

    @Column(name = "flag_value", columnDefinition = "TEXT")
    private String flagValue;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "target_user_percentage")
    private Integer targetUserPercentage;

    @Column(name = "target_conditions", columnDefinition = "TEXT")
    private String targetConditions;

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

    // Many-to-many relationship with Users for targeted rollouts
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "feature_flag_target_users",
        joinColumns = @JoinColumn(name = "feature_flag_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> targetUsers = new HashSet<>();

    // Many-to-many relationship with Roles for role-based rollouts
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "feature_flag_target_roles",
        joinColumns = @JoinColumn(name = "feature_flag_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> targetRoles = new HashSet<>();

    /**
     * Checks if the feature flag is currently active.
     */
    public boolean isActive() {
        if (!enabled) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        
        // Check start date
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        
        // Check end date
        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Gets the effective value (flagValue if set, otherwise defaultValue).
     */
    public String getEffectiveValue() {
        return flagValue != null ? flagValue : defaultValue;
    }

    /**
     * Gets value as boolean.
     */
    public Boolean getBooleanValue() {
        String effectiveValue = getEffectiveValue();
        if (effectiveValue == null) {
            return false;
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
     * Checks if user is in target users.
     */
    public boolean isTargetUser(User user) {
        return targetUsers.contains(user);
    }

    /**
     * Checks if user has target role.
     */
    public boolean hasTargetRole(User user) {
        return user.getRoles().stream()
            .anyMatch(targetRoles::contains);
    }

    /**
     * Adds target user.
     */
    public void addTargetUser(User user) {
        targetUsers.add(user);
    }

    /**
     * Removes target user.
     */
    public void removeTargetUser(User user) {
        targetUsers.remove(user);
    }

    /**
     * Adds target role.
     */
    public void addTargetRole(Role role) {
        targetRoles.add(role);
    }

    /**
     * Removes target role.
     */
    public void removeTargetRole(Role role) {
        targetRoles.remove(role);
    }

    /**
     * Pre-persist callback to set default values.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (enabled == null) {
            enabled = false;
        }
        if (rolloutPercentage == null) {
            rolloutPercentage = 0;
        }
        if (flagType == null) {
            flagType = FlagType.BOOLEAN;
        }
        if (environment == null) {
            environment = Environment.ALL;
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
     * Feature flag types.
     */
    public static class FlagType {
        public static final String BOOLEAN = "BOOLEAN";
        public static final String STRING = "STRING";
        public static final String INTEGER = "INTEGER";
        public static final String DOUBLE = "DOUBLE";
        public static final String JSON = "JSON";
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

    /**
     * Standard feature flags for ClusterFuzz.
     */
    public static class StandardFlags {
        public static final String NEW_UI_ENABLED = "new_ui_enabled";
        public static final String ADVANCED_FUZZING = "advanced_fuzzing_enabled";
        public static final String ML_CRASH_ANALYSIS = "ml_crash_analysis_enabled";
        public static final String REAL_TIME_NOTIFICATIONS = "real_time_notifications_enabled";
        public static final String ENHANCED_REPORTING = "enhanced_reporting_enabled";
        public static final String API_V2_ENABLED = "api_v2_enabled";
        public static final String EXPERIMENTAL_FEATURES = "experimental_features_enabled";
        public static final String PERFORMANCE_MONITORING = "performance_monitoring_enabled";
        public static final String SECURITY_AUDIT_LOGGING = "security_audit_logging_enabled";
        public static final String AUTOMATED_TRIAGE = "automated_triage_enabled";
    }
}