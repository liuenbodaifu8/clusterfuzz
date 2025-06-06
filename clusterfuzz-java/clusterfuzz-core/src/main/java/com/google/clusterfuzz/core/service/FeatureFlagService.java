package com.google.clusterfuzz.core.service;

import com.google.clusterfuzz.core.entity.FeatureFlag;
import com.google.clusterfuzz.core.entity.User;
import com.google.clusterfuzz.core.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing feature flags and feature rollouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final UserService userService;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Initializes default feature flags.
     */
    @PostConstruct
    public void initializeDefaultFeatureFlags() {
        log.info("Initializing default feature flags");
        createDefaultFeatureFlags();
    }

    /**
     * Checks if a feature is enabled for a user.
     */
    @Cacheable(value = "featureFlags", key = "#flagKey + ':' + #userEmail")
    @Transactional(readOnly = true)
    public boolean isEnabled(String flagKey, String userEmail) {
        Optional<FeatureFlag> flagOpt = findByKey(flagKey);
        if (flagOpt.isEmpty()) {
            log.debug("Feature flag not found: {}", flagKey);
            return false;
        }

        FeatureFlag flag = flagOpt.get();
        
        // Check if flag is active
        if (!flag.isActive()) {
            return false;
        }

        // Check environment
        String currentEnv = getCurrentEnvironment();
        if (!flag.getEnvironment().equals("ALL") && !flag.getEnvironment().equals(currentEnv)) {
            return false;
        }

        // Get user for targeted checks
        Optional<User> userOpt = userService.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return isEnabledForUser(flag, user);
    }

    /**
     * Checks if a feature is enabled (without user context).
     */
    @Cacheable(value = "featureFlags", key = "#flagKey + ':global'")
    @Transactional(readOnly = true)
    public boolean isEnabled(String flagKey) {
        Optional<FeatureFlag> flagOpt = findByKey(flagKey);
        if (flagOpt.isEmpty()) {
            return false;
        }

        FeatureFlag flag = flagOpt.get();
        
        // Check if flag is active
        if (!flag.isActive()) {
            return false;
        }

        // Check environment
        String currentEnv = getCurrentEnvironment();
        if (!flag.getEnvironment().equals("ALL") && !flag.getEnvironment().equals(currentEnv)) {
            return false;
        }

        // For global checks, use rollout percentage
        return secureRandom.nextInt(100) < flag.getRolloutPercentage();
    }

    /**
     * Gets feature flag value for a user.
     */
    @Cacheable(value = "featureFlags", key = "#flagKey + ':value:' + #userEmail")
    @Transactional(readOnly = true)
    public String getValue(String flagKey, String userEmail, String defaultValue) {
        if (!isEnabled(flagKey, userEmail)) {
            return defaultValue;
        }

        Optional<FeatureFlag> flagOpt = findByKey(flagKey);
        if (flagOpt.isPresent()) {
            String value = flagOpt.get().getEffectiveValue();
            return value != null ? value : defaultValue;
        }

        return defaultValue;
    }

    /**
     * Gets feature flag boolean value.
     */
    @Transactional(readOnly = true)
    public boolean getBooleanValue(String flagKey, String userEmail, boolean defaultValue) {
        String value = getValue(flagKey, userEmail, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets feature flag integer value.
     */
    @Transactional(readOnly = true)
    public int getIntegerValue(String flagKey, String userEmail, int defaultValue) {
        String value = getValue(flagKey, userEmail, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Creates a new feature flag.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public FeatureFlag createFeatureFlag(FeatureFlag featureFlag) {
        validateFeatureFlag(featureFlag);
        
        FeatureFlag saved = featureFlagRepository.save(featureFlag);
        log.info("Created feature flag: {}", saved.getKey());
        
        return saved;
    }

    /**
     * Updates an existing feature flag.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public FeatureFlag updateFeatureFlag(FeatureFlag featureFlag) {
        validateFeatureFlag(featureFlag);
        
        FeatureFlag saved = featureFlagRepository.save(featureFlag);
        log.info("Updated feature flag: {}", saved.getKey());
        
        return saved;
    }

    /**
     * Enables a feature flag.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public void enableFlag(String flagKey, String updatedBy) {
        Optional<FeatureFlag> flagOpt = findByKey(flagKey);
        if (flagOpt.isPresent()) {
            FeatureFlag flag = flagOpt.get();
            flag.setEnabled(true);
            flag.setUpdatedBy(updatedBy);
            featureFlagRepository.save(flag);
            
            log.info("Enabled feature flag: {} by {}", flagKey, updatedBy);
        }
    }

    /**
     * Disables a feature flag.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public void disableFlag(String flagKey, String updatedBy) {
        Optional<FeatureFlag> flagOpt = findByKey(flagKey);
        if (flagOpt.isPresent()) {
            FeatureFlag flag = flagOpt.get();
            flag.setEnabled(false);
            flag.setUpdatedBy(updatedBy);
            featureFlagRepository.save(flag);
            
            log.info("Disabled feature flag: {} by {}", flagKey, updatedBy);
        }
    }

    /**
     * Updates rollout percentage.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public void updateRolloutPercentage(String flagKey, int percentage, String updatedBy) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Rollout percentage must be between 0 and 100");
        }

        Optional<FeatureFlag> flagOpt = findByKey(flagKey);
        if (flagOpt.isPresent()) {
            FeatureFlag flag = flagOpt.get();
            flag.setRolloutPercentage(percentage);
            flag.setUpdatedBy(updatedBy);
            featureFlagRepository.save(flag);
            
            log.info("Updated rollout percentage for {}: {}% by {}", flagKey, percentage, updatedBy);
        }
    }

    /**
     * Adds target user to feature flag.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public void addTargetUser(String flagKey, String userEmail, String updatedBy) {
        Optional<FeatureFlag> flagOpt = findByKey(flagKey);
        Optional<User> userOpt = userService.findByEmail(userEmail);
        
        if (flagOpt.isPresent() && userOpt.isPresent()) {
            FeatureFlag flag = flagOpt.get();
            User user = userOpt.get();
            
            flag.addTargetUser(user);
            flag.setUpdatedBy(updatedBy);
            featureFlagRepository.save(flag);
            
            log.info("Added target user {} to feature flag {} by {}", userEmail, flagKey, updatedBy);
        }
    }

    /**
     * Removes target user from feature flag.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public void removeTargetUser(String flagKey, String userEmail, String updatedBy) {
        Optional<FeatureFlag> flagOpt = findByKey(flagKey);
        Optional<User> userOpt = userService.findByEmail(userEmail);
        
        if (flagOpt.isPresent() && userOpt.isPresent()) {
            FeatureFlag flag = flagOpt.get();
            User user = userOpt.get();
            
            flag.removeTargetUser(user);
            flag.setUpdatedBy(updatedBy);
            featureFlagRepository.save(flag);
            
            log.info("Removed target user {} from feature flag {} by {}", userEmail, flagKey, updatedBy);
        }
    }

    /**
     * Finds feature flag by key.
     */
    @Cacheable(value = "featureFlags", key = "'entity:' + #key")
    @Transactional(readOnly = true)
    public Optional<FeatureFlag> findByKey(String key) {
        return featureFlagRepository.findByKey(key);
    }

    /**
     * Gets all feature flags.
     */
    @Transactional(readOnly = true)
    public List<FeatureFlag> findAll() {
        return featureFlagRepository.findAll();
    }

    /**
     * Gets enabled feature flags.
     */
    @Cacheable(value = "featureFlags", key = "'enabled'")
    @Transactional(readOnly = true)
    public List<FeatureFlag> findAllEnabled() {
        return featureFlagRepository.findByEnabledTrue();
    }

    /**
     * Gets feature flags by environment.
     */
    @Cacheable(value = "featureFlags", key = "'environment:' + #environment")
    @Transactional(readOnly = true)
    public List<FeatureFlag> findByEnvironment(String environment) {
        return featureFlagRepository.findByEnvironmentOrEnvironment(environment, "ALL");
    }

    /**
     * Deletes a feature flag.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public void deleteFeatureFlag(Long flagId) {
        FeatureFlag flag = featureFlagRepository.findById(flagId)
            .orElseThrow(() -> new IllegalArgumentException("Feature flag not found: " + flagId));
        
        featureFlagRepository.delete(flag);
        log.info("Deleted feature flag: {}", flag.getKey());
    }

    /**
     * Checks if feature is enabled for a specific user.
     */
    private boolean isEnabledForUser(FeatureFlag flag, User user) {
        // Check if user is specifically targeted
        if (flag.isTargetUser(user)) {
            return true;
        }

        // Check if user has target role
        if (flag.hasTargetRole(user)) {
            return true;
        }

        // Check rollout percentage based on user ID
        if (flag.getRolloutPercentage() > 0) {
            int userHash = Math.abs(user.getId().hashCode()) % 100;
            return userHash < flag.getRolloutPercentage();
        }

        return false;
    }

    /**
     * Gets current environment.
     */
    private String getCurrentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0].toUpperCase();
            switch (profile) {
                case "DEV":
                case "DEVELOPMENT":
                    return FeatureFlag.Environment.DEVELOPMENT;
                case "STAGING":
                case "STAGE":
                    return FeatureFlag.Environment.STAGING;
                case "PROD":
                case "PRODUCTION":
                    return FeatureFlag.Environment.PRODUCTION;
                default:
                    return FeatureFlag.Environment.ALL;
            }
        }
        return FeatureFlag.Environment.ALL;
    }

    /**
     * Creates default feature flags.
     */
    private void createDefaultFeatureFlags() {
        createFlagIfNotExists(FeatureFlag.StandardFlags.NEW_UI_ENABLED, 
            "New UI", "Enable new user interface", false, 0);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.ADVANCED_FUZZING, 
            "Advanced Fuzzing", "Enable advanced fuzzing algorithms", false, 0);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.ML_CRASH_ANALYSIS, 
            "ML Crash Analysis", "Enable machine learning crash analysis", false, 0);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.REAL_TIME_NOTIFICATIONS, 
            "Real-time Notifications", "Enable real-time notifications", true, 100);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.ENHANCED_REPORTING, 
            "Enhanced Reporting", "Enable enhanced reporting features", false, 25);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.API_V2_ENABLED, 
            "API v2", "Enable API version 2", false, 0);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.EXPERIMENTAL_FEATURES, 
            "Experimental Features", "Enable experimental features", false, 0);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.PERFORMANCE_MONITORING, 
            "Performance Monitoring", "Enable performance monitoring", true, 100);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.SECURITY_AUDIT_LOGGING, 
            "Security Audit Logging", "Enable security audit logging", true, 100);
        
        createFlagIfNotExists(FeatureFlag.StandardFlags.AUTOMATED_TRIAGE, 
            "Automated Triage", "Enable automated issue triage", false, 10);
        
        log.info("Default feature flags created");
    }

    /**
     * Creates feature flag if it doesn't exist.
     */
    private void createFlagIfNotExists(String key, String name, String description, 
                                     boolean enabled, int rolloutPercentage) {
        if (featureFlagRepository.findByKey(key).isEmpty()) {
            FeatureFlag flag = new FeatureFlag();
            flag.setKey(key);
            flag.setName(name);
            flag.setDescription(description);
            flag.setEnabled(enabled);
            flag.setRolloutPercentage(rolloutPercentage);
            flag.setFlagType(FeatureFlag.FlagType.BOOLEAN);
            flag.setDefaultValue("false");
            flag.setEnvironment(FeatureFlag.Environment.ALL);
            flag.setCreatedBy("SYSTEM");
            
            featureFlagRepository.save(flag);
            log.debug("Created default feature flag: {}", key);
        }
    }

    /**
     * Validates feature flag data.
     */
    private void validateFeatureFlag(FeatureFlag featureFlag) {
        if (featureFlag.getKey() == null || featureFlag.getKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Feature flag key is required");
        }
        
        if (featureFlag.getRolloutPercentage() < 0 || featureFlag.getRolloutPercentage() > 100) {
            throw new IllegalArgumentException("Rollout percentage must be between 0 and 100");
        }
        
        // Check for duplicate key (excluding current flag)
        Optional<FeatureFlag> existingFlag = findByKey(featureFlag.getKey());
        if (existingFlag.isPresent() && !existingFlag.get().getId().equals(featureFlag.getId())) {
            throw new IllegalArgumentException("Feature flag key already exists: " + featureFlag.getKey());
        }
    }
}