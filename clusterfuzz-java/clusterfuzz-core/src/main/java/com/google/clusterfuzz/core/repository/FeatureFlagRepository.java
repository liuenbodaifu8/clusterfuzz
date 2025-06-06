package com.google.clusterfuzz.core.repository;

import com.google.clusterfuzz.core.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for FeatureFlag entity.
 */
@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    /**
     * Finds feature flag by key.
     */
    Optional<FeatureFlag> findByKey(String key);

    /**
     * Finds all enabled feature flags.
     */
    List<FeatureFlag> findByEnabledTrue();

    /**
     * Finds all disabled feature flags.
     */
    List<FeatureFlag> findByEnabledFalse();

    /**
     * Finds feature flags by environment.
     */
    List<FeatureFlag> findByEnvironment(String environment);

    /**
     * Finds feature flags by environment or ALL environment.
     */
    List<FeatureFlag> findByEnvironmentOrEnvironment(String environment1, String environment2);

    /**
     * Finds feature flags by flag type.
     */
    List<FeatureFlag> findByFlagType(String flagType);

    /**
     * Finds feature flags with rollout percentage greater than specified value.
     */
    List<FeatureFlag> findByRolloutPercentageGreaterThan(Integer percentage);

    /**
     * Finds feature flags with rollout percentage between values.
     */
    List<FeatureFlag> findByRolloutPercentageBetween(Integer minPercentage, Integer maxPercentage);

    /**
     * Finds active feature flags (enabled and within date range).
     */
    @Query("SELECT f FROM FeatureFlag f WHERE f.enabled = true AND " +
           "(f.startDate IS NULL OR f.startDate <= :now) AND " +
           "(f.endDate IS NULL OR f.endDate >= :now)")
    List<FeatureFlag> findActiveFlags(@Param("now") LocalDateTime now);

    /**
     * Finds feature flags that target a specific user.
     */
    @Query("SELECT f FROM FeatureFlag f JOIN f.targetUsers u WHERE u.email = :userEmail")
    List<FeatureFlag> findByTargetUserEmail(@Param("userEmail") String userEmail);

    /**
     * Finds feature flags that target a specific role.
     */
    @Query("SELECT f FROM FeatureFlag f JOIN f.targetRoles r WHERE r.name = :roleName")
    List<FeatureFlag> findByTargetRoleName(@Param("roleName") String roleName);

    /**
     * Finds feature flags created by user.
     */
    List<FeatureFlag> findByCreatedBy(String createdBy);

    /**
     * Finds feature flags updated by user.
     */
    List<FeatureFlag> findByUpdatedBy(String updatedBy);

    /**
     * Finds feature flags by key pattern.
     */
    @Query("SELECT f FROM FeatureFlag f WHERE f.key LIKE :keyPattern")
    List<FeatureFlag> findByKeyPattern(@Param("keyPattern") String keyPattern);

    /**
     * Finds feature flags that are scheduled to start.
     */
    @Query("SELECT f FROM FeatureFlag f WHERE f.startDate > :now AND f.enabled = true")
    List<FeatureFlag> findScheduledFlags(@Param("now") LocalDateTime now);

    /**
     * Finds feature flags that have expired.
     */
    @Query("SELECT f FROM FeatureFlag f WHERE f.endDate < :now AND f.enabled = true")
    List<FeatureFlag> findExpiredFlags(@Param("now") LocalDateTime now);

    /**
     * Finds feature flags with target conditions.
     */
    @Query("SELECT f FROM FeatureFlag f WHERE f.targetConditions IS NOT NULL")
    List<FeatureFlag> findFlagsWithTargetConditions();

    /**
     * Checks if feature flag exists by key.
     */
    boolean existsByKey(String key);

    /**
     * Counts enabled feature flags.
     */
    long countByEnabledTrue();

    /**
     * Counts feature flags by environment.
     */
    long countByEnvironment(String environment);

    /**
     * Finds feature flags ordered by rollout percentage.
     */
    List<FeatureFlag> findAllByOrderByRolloutPercentageDesc();

    /**
     * Finds recently created feature flags.
     */
    @Query("SELECT f FROM FeatureFlag f WHERE f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<FeatureFlag> findRecentlyCreatedFlags(@Param("since") LocalDateTime since);

    /**
     * Finds recently updated feature flags.
     */
    @Query("SELECT f FROM FeatureFlag f WHERE f.updatedAt >= :since ORDER BY f.updatedAt DESC")
    List<FeatureFlag> findRecentlyUpdatedFlags(@Param("since") LocalDateTime since);
}