package com.google.clusterfuzz.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Security audit log entity for tracking security-related events.
 */
@Entity
@Table(name = "security_audit_logs", indexes = {
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_user_email", columnList = "user_email"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_severity", columnList = "severity"),
    @Index(name = "idx_audit_source_ip", columnList = "source_ip")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@EqualsAndHashCode
@ToString
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    @NotBlank(message = "Event type is required")
    @Size(max = 50, message = "Event type must not exceed 50 characters")
    private String eventType;

    @Column(name = "event_category", length = 30)
    @Size(max = 30, message = "Event category must not exceed 30 characters")
    private String eventCategory;

    @Column(name = "user_email", length = 255)
    @Size(max = 255, message = "User email must not exceed 255 characters")
    private String userEmail;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 100)
    @Size(max = 100, message = "Session ID must not exceed 100 characters")
    private String sessionId;

    @Column(name = "source_ip", length = 45)
    @Size(max = 45, message = "Source IP must not exceed 45 characters")
    private String sourceIp;

    @Column(name = "user_agent", length = 500)
    @Size(max = 500, message = "User agent must not exceed 500 characters")
    private String userAgent;

    @Column(name = "resource", length = 200)
    @Size(max = 200, message = "Resource must not exceed 200 characters")
    private String resource;

    @Column(name = "action", length = 50)
    @Size(max = 50, message = "Action must not exceed 50 characters")
    private String action;

    @Column(name = "result", length = 20)
    @Size(max = 20, message = "Result must not exceed 20 characters")
    private String result;

    @Column(name = "severity", length = 20)
    @Size(max = 20, message = "Severity must not exceed 20 characters")
    private String severity;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "timestamp", nullable = false)
    @CreatedDate
    private LocalDateTime timestamp;

    /**
     * Pre-persist callback to set default values.
     */
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (severity == null) {
            severity = Severity.INFO;
        }
        if (result == null) {
            result = Result.SUCCESS;
        }
        if (riskScore == null) {
            riskScore = 0;
        }
    }

    /**
     * Event types for security audit logging.
     */
    public static class EventType {
        public static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
        public static final String AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";
        public static final String AUTHORIZATION_SUCCESS = "AUTHORIZATION_SUCCESS";
        public static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";
        public static final String LOGIN = "LOGIN";
        public static final String LOGOUT = "LOGOUT";
        public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
        public static final String ROLE_CHANGE = "ROLE_CHANGE";
        public static final String PERMISSION_CHANGE = "PERMISSION_CHANGE";
        public static final String CONFIGURATION_CHANGE = "CONFIGURATION_CHANGE";
        public static final String SENSITIVE_DATA_ACCESS = "SENSITIVE_DATA_ACCESS";
        public static final String ADMIN_ACTION = "ADMIN_ACTION";
        public static final String SECURITY_VIOLATION = "SECURITY_VIOLATION";
        public static final String SUSPICIOUS_ACTIVITY = "SUSPICIOUS_ACTIVITY";
        public static final String DATA_EXPORT = "DATA_EXPORT";
        public static final String API_ACCESS = "API_ACCESS";
        public static final String FEATURE_FLAG_CHANGE = "FEATURE_FLAG_CHANGE";
    }

    /**
     * Event categories.
     */
    public static class EventCategory {
        public static final String AUTHENTICATION = "AUTHENTICATION";
        public static final String AUTHORIZATION = "AUTHORIZATION";
        public static final String CONFIGURATION = "CONFIGURATION";
        public static final String DATA_ACCESS = "DATA_ACCESS";
        public static final String ADMIN = "ADMIN";
        public static final String SECURITY = "SECURITY";
        public static final String API = "API";
    }

    /**
     * Severity levels.
     */
    public static class Severity {
        public static final String LOW = "LOW";
        public static final String INFO = "INFO";
        public static final String MEDIUM = "MEDIUM";
        public static final String HIGH = "HIGH";
        public static final String CRITICAL = "CRITICAL";
    }

    /**
     * Result types.
     */
    public static class Result {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";
        public static final String BLOCKED = "BLOCKED";
        public static final String WARNING = "WARNING";
    }
}