package com.google.clusterfuzz.core.repository;

import com.google.clusterfuzz.core.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for SecurityAuditLog entity.
 */
@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {

    /**
     * Finds audit logs by user email.
     */
    List<SecurityAuditLog> findByUserEmail(String userEmail);

    /**
     * Finds audit logs by user email after a specific timestamp.
     */
    List<SecurityAuditLog> findByUserEmailAndTimestampAfter(String userEmail, LocalDateTime timestamp);

    /**
     * Finds audit logs by event type.
     */
    List<SecurityAuditLog> findByEventType(String eventType);

    /**
     * Finds audit logs by event type after a specific timestamp.
     */
    List<SecurityAuditLog> findByEventTypeAndTimestampAfter(String eventType, LocalDateTime timestamp);

    /**
     * Finds audit logs by event category.
     */
    List<SecurityAuditLog> findByEventCategory(String eventCategory);

    /**
     * Finds audit logs by severity.
     */
    List<SecurityAuditLog> findBySeverity(String severity);

    /**
     * Finds audit logs by result.
     */
    List<SecurityAuditLog> findByResult(String result);

    /**
     * Finds audit logs by source IP.
     */
    List<SecurityAuditLog> findBySourceIp(String sourceIp);

    /**
     * Finds audit logs with risk score greater than specified value.
     */
    List<SecurityAuditLog> findByRiskScoreGreaterThan(Integer riskScore);

    /**
     * Finds audit logs with risk score greater than specified value after timestamp.
     */
    List<SecurityAuditLog> findByRiskScoreGreaterThanAndTimestampAfter(Integer riskScore, LocalDateTime timestamp);

    /**
     * Finds audit logs between timestamps.
     */
    List<SecurityAuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Finds audit logs after a specific timestamp.
     */
    List<SecurityAuditLog> findByTimestampAfter(LocalDateTime timestamp);

    /**
     * Finds audit logs by user and event type.
     */
    List<SecurityAuditLog> findByUserEmailAndEventType(String userEmail, String eventType);

    /**
     * Finds audit logs by source IP and event type.
     */
    List<SecurityAuditLog> findBySourceIpAndEventType(String sourceIp, String eventType);

    /**
     * Finds failed authentication attempts by IP.
     */
    @Query("SELECT s FROM SecurityAuditLog s WHERE s.sourceIp = :sourceIp AND " +
           "s.eventType = 'AUTHENTICATION_FAILURE' AND s.timestamp >= :since")
    List<SecurityAuditLog> findFailedAuthenticationsByIp(@Param("sourceIp") String sourceIp, 
                                                         @Param("since") LocalDateTime since);

    /**
     * Finds failed authentication attempts by user.
     */
    @Query("SELECT s FROM SecurityAuditLog s WHERE s.userEmail = :userEmail AND " +
           "s.eventType = 'AUTHENTICATION_FAILURE' AND s.timestamp >= :since")
    List<SecurityAuditLog> findFailedAuthenticationsByUser(@Param("userEmail") String userEmail, 
                                                           @Param("since") LocalDateTime since);

    /**
     * Finds security violations by user.
     */
    @Query("SELECT s FROM SecurityAuditLog s WHERE s.userEmail = :userEmail AND " +
           "s.eventType = 'SECURITY_VIOLATION' AND s.timestamp >= :since")
    List<SecurityAuditLog> findSecurityViolationsByUser(@Param("userEmail") String userEmail, 
                                                        @Param("since") LocalDateTime since);

    /**
     * Finds suspicious activities.
     */
    @Query("SELECT s FROM SecurityAuditLog s WHERE s.eventType = 'SUSPICIOUS_ACTIVITY' AND " +
           "s.timestamp >= :since ORDER BY s.timestamp DESC")
    List<SecurityAuditLog> findSuspiciousActivities(@Param("since") LocalDateTime since);

    /**
     * Finds admin actions.
     */
    @Query("SELECT s FROM SecurityAuditLog s WHERE s.eventType = 'ADMIN_ACTION' AND " +
           "s.timestamp >= :since ORDER BY s.timestamp DESC")
    List<SecurityAuditLog> findAdminActions(@Param("since") LocalDateTime since);

    /**
     * Counts audit logs by event type.
     */
    long countByEventType(String eventType);

    /**
     * Counts audit logs by user email.
     */
    long countByUserEmail(String userEmail);

    /**
     * Counts audit logs by severity.
     */
    long countBySeverity(String severity);

    /**
     * Counts audit logs after timestamp.
     */
    long countByTimestampAfter(LocalDateTime timestamp);

    /**
     * Gets audit log statistics by event type.
     */
    @Query("SELECT s.eventType, COUNT(s) FROM SecurityAuditLog s WHERE s.timestamp >= :since " +
           "GROUP BY s.eventType ORDER BY COUNT(s) DESC")
    List<Object[]> getEventTypeStatistics(@Param("since") LocalDateTime since);

    /**
     * Gets audit log statistics by severity.
     */
    @Query("SELECT s.severity, COUNT(s) FROM SecurityAuditLog s WHERE s.timestamp >= :since " +
           "GROUP BY s.severity ORDER BY COUNT(s) DESC")
    List<Object[]> getSeverityStatistics(@Param("since") LocalDateTime since);

    /**
     * Gets top users by audit log count.
     */
    @Query("SELECT s.userEmail, COUNT(s) FROM SecurityAuditLog s WHERE s.timestamp >= :since " +
           "GROUP BY s.userEmail ORDER BY COUNT(s) DESC")
    List<Object[]> getTopUsersByActivity(@Param("since") LocalDateTime since);

    /**
     * Gets top source IPs by audit log count.
     */
    @Query("SELECT s.sourceIp, COUNT(s) FROM SecurityAuditLog s WHERE s.timestamp >= :since " +
           "GROUP BY s.sourceIp ORDER BY COUNT(s) DESC")
    List<Object[]> getTopSourceIpsByActivity(@Param("since") LocalDateTime since);

    /**
     * Finds recent audit logs ordered by timestamp.
     */
    @Query("SELECT s FROM SecurityAuditLog s WHERE s.timestamp >= :since " +
           "ORDER BY s.timestamp DESC")
    List<SecurityAuditLog> findRecentAuditLogs(@Param("since") LocalDateTime since);

    /**
     * Deletes old audit logs before a specific timestamp.
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}