package com.google.clusterfuzz.core.repository;

import com.google.clusterfuzz.core.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Configuration entity.
 */
@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    /**
     * Finds configuration by key.
     */
    Optional<Configuration> findByKey(String key);

    /**
     * Finds configurations by category.
     */
    List<Configuration> findByCategory(String category);

    /**
     * Finds configurations by environment.
     */
    List<Configuration> findByEnvironment(String environment);

    /**
     * Finds configurations by environment or ALL environment.
     */
    List<Configuration> findByEnvironmentOrEnvironment(String environment1, String environment2);

    /**
     * Finds configurations by data type.
     */
    List<Configuration> findByDataType(String dataType);

    /**
     * Finds all active configurations.
     */
    List<Configuration> findByActiveTrue();

    /**
     * Finds all inactive configurations.
     */
    List<Configuration> findByActiveFalse();

    /**
     * Finds sensitive configurations.
     */
    List<Configuration> findBySensitiveTrue();

    /**
     * Finds system configurations.
     */
    List<Configuration> findBySystemConfigTrue();

    /**
     * Finds non-system configurations.
     */
    List<Configuration> findBySystemConfigFalse();

    /**
     * Finds encrypted configurations.
     */
    List<Configuration> findByEncryptedTrue();

    /**
     * Finds configurations by category and environment.
     */
    List<Configuration> findByCategoryAndEnvironment(String category, String environment);

    /**
     * Finds configurations by category and active status.
     */
    List<Configuration> findByCategoryAndActive(String category, Boolean active);

    /**
     * Finds configurations with null values.
     */
    @Query("SELECT c FROM Configuration c WHERE c.value IS NULL")
    List<Configuration> findConfigurationsWithNullValues();

    /**
     * Finds configurations with default values only.
     */
    @Query("SELECT c FROM Configuration c WHERE c.value IS NULL AND c.defaultValue IS NOT NULL")
    List<Configuration> findConfigurationsUsingDefaults();

    /**
     * Finds configurations by key pattern.
     */
    @Query("SELECT c FROM Configuration c WHERE c.key LIKE :keyPattern")
    List<Configuration> findByKeyPattern(@Param("keyPattern") String keyPattern);

    /**
     * Finds configurations created by user.
     */
    List<Configuration> findByCreatedBy(String createdBy);

    /**
     * Finds configurations updated by user.
     */
    List<Configuration> findByUpdatedBy(String updatedBy);

    /**
     * Checks if configuration exists by key.
     */
    boolean existsByKey(String key);

    /**
     * Counts configurations by category.
     */
    long countByCategory(String category);

    /**
     * Counts active configurations.
     */
    long countByActiveTrue();

    /**
     * Counts sensitive configurations.
     */
    long countBySensitiveTrue();

    /**
     * Finds configurations ordered by category and key.
     */
    List<Configuration> findAllByOrderByCategoryAscKeyAsc();

    /**
     * Finds configurations by multiple categories.
     */
    @Query("SELECT c FROM Configuration c WHERE c.category IN :categories")
    List<Configuration> findByCategories(@Param("categories") List<String> categories);

    /**
     * Finds configurations that need values (null value and null default).
     */
    @Query("SELECT c FROM Configuration c WHERE c.value IS NULL AND c.defaultValue IS NULL AND c.active = true")
    List<Configuration> findConfigurationsNeedingValues();
}