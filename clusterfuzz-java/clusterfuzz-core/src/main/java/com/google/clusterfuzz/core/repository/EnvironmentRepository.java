package com.google.clusterfuzz.core.repository;

import com.google.clusterfuzz.core.entity.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Environment entity.
 */
@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    /**
     * Finds environment by name.
     */
    Optional<Environment> findByName(String name);

    /**
     * Finds environments by platform.
     */
    List<Environment> findByPlatform(String platform);

    /**
     * Finds environments by environment type.
     */
    List<Environment> findByEnvironmentType(String environmentType);

    /**
     * Finds all active environments.
     */
    List<Environment> findByActiveTrue();

    /**
     * Finds all inactive environments.
     */
    List<Environment> findByActiveFalse();

    /**
     * Finds trusted host environments.
     */
    List<Environment> findByTrustedHostTrue();

    /**
     * Finds App Engine environments.
     */
    List<Environment> findByAppEngineTrue();

    /**
     * Finds Kubernetes environments.
     */
    List<Environment> findByKubernetesTrue();

    /**
     * Finds development environments.
     */
    List<Environment> findByDevelopmentTrue();

    /**
     * Finds environments by health status.
     */
    List<Environment> findByHealthStatus(String healthStatus);

    /**
     * Finds healthy environments.
     */
    @Query("SELECT e FROM Environment e WHERE e.healthStatus = 'HEALTHY' AND e.active = true")
    List<Environment> findHealthyEnvironments();

    /**
     * Finds unhealthy environments.
     */
    @Query("SELECT e FROM Environment e WHERE e.healthStatus IN ('UNHEALTHY', 'DEGRADED') AND e.active = true")
    List<Environment> findUnhealthyEnvironments();

    /**
     * Finds environments that need health check.
     */
    @Query("SELECT e FROM Environment e WHERE e.active = true AND " +
           "(e.lastHealthCheck IS NULL OR e.lastHealthCheck < :threshold)")
    List<Environment> findEnvironmentsNeedingHealthCheck(@Param("threshold") LocalDateTime threshold);

    /**
     * Finds environments by platform and active status.
     */
    List<Environment> findByPlatformAndActive(String platform, Boolean active);

    /**
     * Finds environments by environment type and active status.
     */
    List<Environment> findByEnvironmentTypeAndActive(String environmentType, Boolean active);

    /**
     * Finds Android environments.
     */
    @Query("SELECT e FROM Environment e WHERE e.platform LIKE '%ANDROID%' AND e.active = true")
    List<Environment> findAndroidEnvironments();

    /**
     * Finds Windows environments.
     */
    @Query("SELECT e FROM Environment e WHERE e.platform = 'WINDOWS' AND e.active = true")
    List<Environment> findWindowsEnvironments();

    /**
     * Finds Linux environments.
     */
    @Query("SELECT e FROM Environment e WHERE e.platform = 'LINUX' AND e.active = true")
    List<Environment> findLinuxEnvironments();

    /**
     * Finds Mac environments.
     */
    @Query("SELECT e FROM Environment e WHERE e.platform = 'MAC' AND e.active = true")
    List<Environment> findMacEnvironments();

    /**
     * Finds environments with specific memory tools.
     */
    @Query("SELECT e FROM Environment e WHERE e.memoryTools LIKE %:memoryTool% AND e.active = true")
    List<Environment> findByMemoryTool(@Param("memoryTool") String memoryTool);

    /**
     * Finds environments by platform ID.
     */
    List<Environment> findByPlatformId(String platformId);

    /**
     * Finds environments with OS override.
     */
    List<Environment> findByOsOverrideIsNotNull();

    /**
     * Finds environments by OS override.
     */
    List<Environment> findByOsOverride(String osOverride);

    /**
     * Checks if environment exists by name.
     */
    boolean existsByName(String name);

    /**
     * Counts environments by platform.
     */
    long countByPlatform(String platform);

    /**
     * Counts active environments.
     */
    long countByActiveTrue();

    /**
     * Counts healthy environments.
     */
    long countByHealthStatus(String healthStatus);

    /**
     * Gets environment statistics by platform.
     */
    @Query("SELECT e.platform, COUNT(e) FROM Environment e WHERE e.active = true " +
           "GROUP BY e.platform ORDER BY COUNT(e) DESC")
    List<Object[]> getPlatformStatistics();

    /**
     * Gets environment statistics by health status.
     */
    @Query("SELECT e.healthStatus, COUNT(e) FROM Environment e WHERE e.active = true " +
           "GROUP BY e.healthStatus ORDER BY COUNT(e) DESC")
    List<Object[]> getHealthStatusStatistics();

    /**
     * Gets environment statistics by environment type.
     */
    @Query("SELECT e.environmentType, COUNT(e) FROM Environment e WHERE e.active = true " +
           "GROUP BY e.environmentType ORDER BY COUNT(e) DESC")
    List<Object[]> getEnvironmentTypeStatistics();

    /**
     * Finds environments created by user.
     */
    List<Environment> findByCreatedBy(String createdBy);

    /**
     * Finds environments updated by user.
     */
    List<Environment> findByUpdatedBy(String updatedBy);

    /**
     * Finds recently created environments.
     */
    @Query("SELECT e FROM Environment e WHERE e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<Environment> findRecentlyCreatedEnvironments(@Param("since") LocalDateTime since);

    /**
     * Finds recently updated environments.
     */
    @Query("SELECT e FROM Environment e WHERE e.updatedAt >= :since ORDER BY e.updatedAt DESC")
    List<Environment> findRecentlyUpdatedEnvironments(@Param("since") LocalDateTime since);
}