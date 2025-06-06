package com.google.clusterfuzz.core.security.audit;

import com.google.clusterfuzz.core.entity.SecurityAuditLog;
import com.google.clusterfuzz.core.repository.SecurityAuditLogRepository;
import com.google.clusterfuzz.core.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for security audit logging and monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;
    private final FeatureFlagService featureFlagService;

    /**
     * Logs a security event asynchronously.
     */
    @Async
    @Transactional
    public void logSecurityEvent(String eventType, String eventCategory, String message) {
        if (!isAuditLoggingEnabled()) {
            return;
        }

        SecurityAuditLog auditLog = createBaseAuditLog(eventType, eventCategory, message);
        auditLogRepository.save(auditLog);
        
        log.debug("Security event logged: {} - {}", eventType, message);
    }

    /**
     * Logs a security event with details.
     */
    @Async
    @Transactional
    public void logSecurityEvent(String eventType, String eventCategory, String message, 
                               String details, String severity) {
        if (!isAuditLoggingEnabled()) {
            return;
        }

        SecurityAuditLog auditLog = createBaseAuditLog(eventType, eventCategory, message);
        auditLog.setDetails(details);
        auditLog.setSeverity(severity);
        auditLog.setRiskScore(calculateRiskScore(eventType, severity));
        
        auditLogRepository.save(auditLog);
        
        log.debug("Security event logged: {} - {} [{}]", eventType, message, severity);
    }

    /**
     * Logs authentication success.
     */
    @Async
    @Transactional
    public void logAuthenticationSuccess(String userEmail) {
        logSecurityEvent(
            SecurityAuditLog.EventType.AUTHENTICATION_SUCCESS,
            SecurityAuditLog.EventCategory.AUTHENTICATION,
            "User authenticated successfully: " + userEmail,
            null,
            SecurityAuditLog.Severity.INFO
        );
    }

    /**
     * Logs authentication failure.
     */
    @Async
    @Transactional
    public void logAuthenticationFailure(String userEmail, String reason) {
        logSecurityEvent(
            SecurityAuditLog.EventType.AUTHENTICATION_FAILURE,
            SecurityAuditLog.EventCategory.AUTHENTICATION,
            "Authentication failed for user: " + userEmail,
            "Reason: " + reason,
            SecurityAuditLog.Severity.MEDIUM
        );
    }

    /**
     * Logs authorization failure.
     */
    @Async
    @Transactional
    public void logAuthorizationFailure(String userEmail, String resource, String action) {
        logSecurityEvent(
            SecurityAuditLog.EventType.AUTHORIZATION_FAILURE,
            SecurityAuditLog.EventCategory.AUTHORIZATION,
            "Access denied for user: " + userEmail,
            String.format("Resource: %s, Action: %s", resource, action),
            SecurityAuditLog.Severity.MEDIUM
        );
    }

    /**
     * Logs user login.
     */
    @Async
    @Transactional
    public void logUserLogin(String userEmail) {
        SecurityAuditLog auditLog = createBaseAuditLog(
            SecurityAuditLog.EventType.LOGIN,
            SecurityAuditLog.EventCategory.AUTHENTICATION,
            "User logged in: " + userEmail
        );
        auditLog.setResult(SecurityAuditLog.Result.SUCCESS);
        auditLog.setSeverity(SecurityAuditLog.Severity.INFO);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Logs user logout.
     */
    @Async
    @Transactional
    public void logUserLogout(String userEmail) {
        SecurityAuditLog auditLog = createBaseAuditLog(
            SecurityAuditLog.EventType.LOGOUT,
            SecurityAuditLog.EventCategory.AUTHENTICATION,
            "User logged out: " + userEmail
        );
        auditLog.setResult(SecurityAuditLog.Result.SUCCESS);
        auditLog.setSeverity(SecurityAuditLog.Severity.INFO);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Logs configuration changes.
     */
    @Async
    @Transactional
    public void logConfigurationChange(String userEmail, String configKey, String action) {
        logSecurityEvent(
            SecurityAuditLog.EventType.CONFIGURATION_CHANGE,
            SecurityAuditLog.EventCategory.CONFIGURATION,
            "Configuration changed by: " + userEmail,
            String.format("Key: %s, Action: %s", configKey, action),
            SecurityAuditLog.Severity.MEDIUM
        );
    }

    /**
     * Logs feature flag changes.
     */
    @Async
    @Transactional
    public void logFeatureFlagChange(String userEmail, String flagKey, String action) {
        logSecurityEvent(
            SecurityAuditLog.EventType.FEATURE_FLAG_CHANGE,
            SecurityAuditLog.EventCategory.CONFIGURATION,
            "Feature flag changed by: " + userEmail,
            String.format("Flag: %s, Action: %s", flagKey, action),
            SecurityAuditLog.Severity.LOW
        );
    }

    /**
     * Logs admin actions.
     */
    @Async
    @Transactional
    public void logAdminAction(String userEmail, String action, String target) {
        logSecurityEvent(
            SecurityAuditLog.EventType.ADMIN_ACTION,
            SecurityAuditLog.EventCategory.ADMIN,
            "Admin action performed by: " + userEmail,
            String.format("Action: %s, Target: %s", action, target),
            SecurityAuditLog.Severity.HIGH
        );
    }

    /**
     * Logs sensitive data access.
     */
    @Async
    @Transactional
    public void logSensitiveDataAccess(String userEmail, String resource) {
        logSecurityEvent(
            SecurityAuditLog.EventType.SENSITIVE_DATA_ACCESS,
            SecurityAuditLog.EventCategory.DATA_ACCESS,
            "Sensitive data accessed by: " + userEmail,
            "Resource: " + resource,
            SecurityAuditLog.Severity.MEDIUM
        );
    }

    /**
     * Logs security violations.
     */
    @Async
    @Transactional
    public void logSecurityViolation(String userEmail, String violation, String details) {
        logSecurityEvent(
            SecurityAuditLog.EventType.SECURITY_VIOLATION,
            SecurityAuditLog.EventCategory.SECURITY,
            "Security violation: " + violation,
            details,
            SecurityAuditLog.Severity.HIGH
        );
    }

    /**
     * Logs suspicious activity.
     */
    @Async
    @Transactional
    public void logSuspiciousActivity(String userEmail, String activity, String details) {
        logSecurityEvent(
            SecurityAuditLog.EventType.SUSPICIOUS_ACTIVITY,
            SecurityAuditLog.EventCategory.SECURITY,
            "Suspicious activity detected: " + activity,
            details,
            SecurityAuditLog.Severity.HIGH
        );
    }

    /**
     * Logs API access.
     */
    @Async
    @Transactional
    public void logApiAccess(String userEmail, String endpoint, String method, String result) {
        SecurityAuditLog auditLog = createBaseAuditLog(
            SecurityAuditLog.EventType.API_ACCESS,
            SecurityAuditLog.EventCategory.API,
            "API access: " + endpoint
        );
        auditLog.setResource(endpoint);
        auditLog.setAction(method);
        auditLog.setResult(result);
        auditLog.setSeverity(SecurityAuditLog.Severity.INFO);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Gets audit logs for a user.
     */
    @Transactional(readOnly = true)
    public List<SecurityAuditLog> getAuditLogsForUser(String userEmail, LocalDateTime since) {
        return auditLogRepository.findByUserEmailAndTimestampAfter(userEmail, since);
    }

    /**
     * Gets audit logs by event type.
     */
    @Transactional(readOnly = true)
    public List<SecurityAuditLog> getAuditLogsByEventType(String eventType, LocalDateTime since) {
        return auditLogRepository.findByEventTypeAndTimestampAfter(eventType, since);
    }

    /**
     * Gets high-risk audit logs.
     */
    @Transactional(readOnly = true)
    public List<SecurityAuditLog> getHighRiskAuditLogs(LocalDateTime since) {
        return auditLogRepository.findByRiskScoreGreaterThanAndTimestampAfter(70, since);
    }

    /**
     * Gets recent security violations.
     */
    @Transactional(readOnly = true)
    public List<SecurityAuditLog> getRecentSecurityViolations(LocalDateTime since) {
        return auditLogRepository.findByEventTypeAndTimestampAfter(
            SecurityAuditLog.EventType.SECURITY_VIOLATION, since);
    }

    /**
     * Creates base audit log with common fields.
     */
    private SecurityAuditLog createBaseAuditLog(String eventType, String eventCategory, String message) {
        SecurityAuditLog auditLog = new SecurityAuditLog();
        auditLog.setEventType(eventType);
        auditLog.setEventCategory(eventCategory);
        auditLog.setMessage(message);
        auditLog.setTimestamp(LocalDateTime.now());
        
        // Get current user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            auditLog.setUserEmail(authentication.getName());
        }
        
        // Get request information
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            auditLog.setSourceIp(getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setSessionId(request.getSession(false) != null ? 
                request.getSession().getId() : null);
        }
        
        return auditLog;
    }

    /**
     * Calculates risk score based on event type and severity.
     */
    private Integer calculateRiskScore(String eventType, String severity) {
        int baseScore = 0;
        
        // Base score by event type
        switch (eventType) {
            case SecurityAuditLog.EventType.AUTHENTICATION_FAILURE:
                baseScore = 30;
                break;
            case SecurityAuditLog.EventType.AUTHORIZATION_FAILURE:
                baseScore = 40;
                break;
            case SecurityAuditLog.EventType.SECURITY_VIOLATION:
                baseScore = 80;
                break;
            case SecurityAuditLog.EventType.SUSPICIOUS_ACTIVITY:
                baseScore = 70;
                break;
            case SecurityAuditLog.EventType.ADMIN_ACTION:
                baseScore = 50;
                break;
            case SecurityAuditLog.EventType.SENSITIVE_DATA_ACCESS:
                baseScore = 60;
                break;
            default:
                baseScore = 10;
        }
        
        // Multiply by severity factor
        double severityMultiplier = 1.0;
        switch (severity) {
            case SecurityAuditLog.Severity.LOW:
                severityMultiplier = 0.5;
                break;
            case SecurityAuditLog.Severity.INFO:
                severityMultiplier = 0.3;
                break;
            case SecurityAuditLog.Severity.MEDIUM:
                severityMultiplier = 1.0;
                break;
            case SecurityAuditLog.Severity.HIGH:
                severityMultiplier = 1.5;
                break;
            case SecurityAuditLog.Severity.CRITICAL:
                severityMultiplier = 2.0;
                break;
        }
        
        return Math.min(100, (int) (baseScore * severityMultiplier));
    }

    /**
     * Gets client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
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
     * Checks if audit logging is enabled via feature flag.
     */
    private boolean isAuditLoggingEnabled() {
        try {
            return featureFlagService.isEnabled("security_audit_logging_enabled");
        } catch (Exception e) {
            // Default to enabled if feature flag service is not available
            return true;
        }
    }
}