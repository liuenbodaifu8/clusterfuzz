package com.google.clusterfuzz.core.service;

import com.google.clusterfuzz.core.config.environment.EnvironmentDetector;
import com.google.clusterfuzz.core.config.environment.EnvironmentHealthChecker;
import com.google.clusterfuzz.core.entity.Environment;
import com.google.clusterfuzz.core.repository.EnvironmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing environments and their configurations.
 * Based on clusterfuzz._internal.system.environment and config modules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentDetector environmentDetector;
    private final EnvironmentHealthChecker healthChecker;

    /**
     * Initializes default environments.
     */
    @PostConstruct
    public void initializeDefaultEnvironments() {
        log.info("Initializing default environments");
        createDefaultEnvironments();
    }

    /**
     * Gets current environment.
     */
    @Cacheable(value = "environments", key = "'current'")
    @Transactional(readOnly = true)
    public Environment getCurrentEnvironment() {
        // Try to find environment by detected characteristics
        String platform = environmentDetector.detectPlatform();
        String environmentType = environmentDetector.detectEnvironmentType();
        
        Optional<Environment> environment = environmentRepository
            .findByPlatformAndActive(platform, true)
            .stream()
            .filter(env -> environmentType.equals(env.getEnvironmentType()))
            .findFirst();
        
        if (environment.isPresent()) {
            return environment.get();
        }
        
        // Create and return a default environment if none found
        return createDefaultEnvironmentForCurrent();
    }

    /**
     * Creates a new environment.
     */
    @CacheEvict(value = "environments", allEntries = true)
    public Environment createEnvironment(Environment environment) {
        validateEnvironment(environment);
        
        Environment saved = environmentRepository.save(environment);
        log.info("Created environment: {}", saved.getName());
        
        return saved;
    }

    /**
     * Updates an existing environment.
     */
    @CacheEvict(value = "environments", allEntries = true)
    public Environment updateEnvironment(Environment environment) {
        validateEnvironment(environment);
        
        Environment existing = environmentRepository.findById(environment.getId())
            .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + environment.getId()));
        
        Environment saved = environmentRepository.save(environment);
        log.info("Updated environment: {}", saved.getName());
        
        return saved;
    }

    /**
     * Finds environment by name.
     */
    @Cacheable(value = "environments", key = "#name")
    @Transactional(readOnly = true)
    public Optional<Environment> findByName(String name) {
        return environmentRepository.findByName(name);
    }

    /**
     * Gets all environments.
     */
    @Transactional(readOnly = true)
    public List<Environment> findAll() {
        return environmentRepository.findAll();
    }

    /**
     * Gets all active environments.
     */
    @Cacheable(value = "environments", key = "'active'")
    @Transactional(readOnly = true)
    public List<Environment> findAllActive() {
        return environmentRepository.findByActiveTrue();
    }

    /**
     * Gets environments by platform.
     */
    @Cacheable(value = "environments", key = "'platform:' + #platform")
    @Transactional(readOnly = true)
    public List<Environment> findByPlatform(String platform) {
        return environmentRepository.findByPlatform(platform);
    }

    /**
     * Gets environments by environment type.
     */
    @Cacheable(value = "environments", key = "'type:' + #environmentType")
    @Transactional(readOnly = true)
    public List<Environment> findByEnvironmentType(String environmentType) {
        return environmentRepository.findByEnvironmentType(environmentType);
    }

    /**
     * Gets healthy environments.
     */
    @Cacheable(value = "environments", key = "'healthy'")
    @Transactional(readOnly = true)
    public List<Environment> findHealthyEnvironments() {
        return environmentRepository.findHealthyEnvironments();
    }

    /**
     * Gets unhealthy environments.
     */
    @Transactional(readOnly = true)
    public List<Environment> findUnhealthyEnvironments() {
        return environmentRepository.findUnhealthyEnvironments();
    }

    /**
     * Performs health check on an environment.
     */
    @Async
    public CompletableFuture<Void> performHealthCheck(Long environmentId) {
        Optional<Environment> environmentOpt = environmentRepository.findById(environmentId);
        if (environmentOpt.isEmpty()) {
            log.warn("Environment not found for health check: {}", environmentId);
            return CompletableFuture.completedFuture(null);
        }
        
        Environment environment = environmentOpt.get();
        log.debug("Starting health check for environment: {}", environment.getName());
        
        return healthChecker.performHealthCheck(environment)
            .thenAccept(result -> updateHealthStatus(environment, result));
    }

    /**
     * Performs health check on all active environments.
     */
    @Async
    public CompletableFuture<Void> performHealthCheckOnAllEnvironments() {
        List<Environment> activeEnvironments = findAllActive();
        log.info("Starting health check on {} active environments", activeEnvironments.size());
        
        List<CompletableFuture<Void>> futures = activeEnvironments.stream()
            .map(env -> performHealthCheck(env.getId()))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Scheduled health check for all environments.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void scheduledHealthCheck() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<Environment> environmentsNeedingCheck = 
            environmentRepository.findEnvironmentsNeedingHealthCheck(threshold);
        
        if (!environmentsNeedingCheck.isEmpty()) {
            log.info("Performing scheduled health check on {} environments", 
                    environmentsNeedingCheck.size());
            
            for (Environment environment : environmentsNeedingCheck) {
                performHealthCheck(environment.getId());
            }
        }
    }

    /**
     * Detects and updates current environment configuration.
     */
    @CacheEvict(value = "environments", allEntries = true)
    public Environment detectAndUpdateCurrentEnvironment() {
        Environment current = getCurrentEnvironment();
        
        // Update with detected values
        current.setPlatform(environmentDetector.detectPlatform());
        current.setPlatformId(environmentDetector.detectPlatformId());
        current.setEnvironmentType(environmentDetector.detectEnvironmentType());
        current.setTrustedHost(environmentDetector.isTrustedHost());
        current.setAppEngine(environmentDetector.isRunningOnAppEngine());
        current.setKubernetes(environmentDetector.isRunningOnKubernetes());
        current.setDevelopment(environmentDetector.isDevelopmentEnvironment());
        current.setConfigDirectory(environmentDetector.getConfigDirectory());
        current.setResourcesDirectory(environmentDetector.getResourcesDirectory());
        current.setRootDirectory(environmentDetector.getRootDirectory());
        
        Environment saved = environmentRepository.save(current);
        log.info("Updated current environment configuration: {}", saved.getName());
        
        return saved;
    }

    /**
     * Gets environment statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getEnvironmentStatistics() {
        List<Object[]> platformStats = environmentRepository.getPlatformStatistics();
        List<Object[]> healthStats = environmentRepository.getHealthStatusStatistics();
        List<Object[]> typeStats = environmentRepository.getEnvironmentTypeStatistics();
        
        return Map.of(
            "total_environments", environmentRepository.count(),
            "active_environments", environmentRepository.countByActiveTrue(),
            "healthy_environments", environmentRepository.countByHealthStatus(Environment.HealthStatus.HEALTHY),
            "platform_statistics", platformStats,
            "health_statistics", healthStats,
            "type_statistics", typeStats
        );
    }

    /**
     * Activates an environment.
     */
    @CacheEvict(value = "environments", allEntries = true)
    public void activateEnvironment(Long environmentId, String updatedBy) {
        Environment environment = environmentRepository.findById(environmentId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + environmentId));
        
        environment.setActive(true);
        environment.setUpdatedBy(updatedBy);
        environmentRepository.save(environment);
        
        log.info("Activated environment: {} by {}", environment.getName(), updatedBy);
    }

    /**
     * Deactivates an environment.
     */
    @CacheEvict(value = "environments", allEntries = true)
    public void deactivateEnvironment(Long environmentId, String updatedBy) {
        Environment environment = environmentRepository.findById(environmentId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + environmentId));
        
        environment.setActive(false);
        environment.setUpdatedBy(updatedBy);
        environmentRepository.save(environment);
        
        log.info("Deactivated environment: {} by {}", environment.getName(), updatedBy);
    }

    /**
     * Deletes an environment.
     */
    @CacheEvict(value = "environments", allEntries = true)
    public void deleteEnvironment(Long environmentId) {
        Environment environment = environmentRepository.findById(environmentId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + environmentId));
        
        environmentRepository.delete(environment);
        log.info("Deleted environment: {}", environment.getName());
    }

    /**
     * Updates health status of an environment.
     */
    private void updateHealthStatus(Environment environment, EnvironmentHealthChecker.HealthCheckResult result) {
        environment.setHealthStatus(result.getHealthStatus());
        environment.setLastHealthCheck(result.getCheckTime());
        
        // Create health details summary
        StringBuilder healthDetails = new StringBuilder();
        if (!result.getIssues().isEmpty()) {
            healthDetails.append("Issues: ").append(String.join(", ", result.getIssues()));
        }
        if (result.getDetails() != null && !result.getDetails().isEmpty()) {
            if (healthDetails.length() > 0) {
                healthDetails.append("; ");
            }
            healthDetails.append("Details: ").append(result.getDetails().toString());
        }
        
        environment.setHealthDetails(healthDetails.toString());
        environmentRepository.save(environment);
        
        log.debug("Updated health status for environment {}: {}", 
                 environment.getName(), result.getHealthStatus());
    }

    /**
     * Creates default environments.
     */
    private void createDefaultEnvironments() {
        // Create current environment if it doesn't exist
        String currentPlatform = environmentDetector.detectPlatform();
        String currentType = environmentDetector.detectEnvironmentType();
        String currentName = "current-" + currentPlatform.toLowerCase() + "-" + currentType.toLowerCase();
        
        if (environmentRepository.findByName(currentName).isEmpty()) {
            createDefaultEnvironmentForCurrent();
        }
        
        // Create standard environments for each platform
        createStandardEnvironments();
    }

    /**
     * Creates default environment for current runtime.
     */
    private Environment createDefaultEnvironmentForCurrent() {
        Environment environment = new Environment();
        
        String platform = environmentDetector.detectPlatform();
        String environmentType = environmentDetector.detectEnvironmentType();
        
        environment.setName("current-" + platform.toLowerCase() + "-" + environmentType.toLowerCase());
        environment.setDisplayName("Current " + platform + " " + environmentType);
        environment.setDescription("Auto-detected current environment");
        environment.setPlatform(platform);
        environment.setEnvironmentType(environmentType);
        environment.setPlatformId(environmentDetector.detectPlatformId());
        environment.setTrustedHost(environmentDetector.isTrustedHost());
        environment.setAppEngine(environmentDetector.isRunningOnAppEngine());
        environment.setKubernetes(environmentDetector.isRunningOnKubernetes());
        environment.setDevelopment(environmentDetector.isDevelopmentEnvironment());
        environment.setConfigDirectory(environmentDetector.getConfigDirectory());
        environment.setResourcesDirectory(environmentDetector.getResourcesDirectory());
        environment.setRootDirectory(environmentDetector.getRootDirectory());
        environment.setActive(true);
        environment.setCreatedBy("SYSTEM");
        
        Environment saved = environmentRepository.save(environment);
        log.info("Created default current environment: {}", saved.getName());
        
        return saved;
    }

    /**
     * Creates standard environments.
     */
    private void createStandardEnvironments() {
        String[] platforms = {"LINUX", "WINDOWS", "MAC", "ANDROID"};
        String[] types = {"PRODUCTION", "STAGING", "DEVELOPMENT"};
        
        for (String platform : platforms) {
            for (String type : types) {
                String name = "standard-" + platform.toLowerCase() + "-" + type.toLowerCase();
                
                if (environmentRepository.findByName(name).isEmpty()) {
                    Environment environment = new Environment();
                    environment.setName(name);
                    environment.setDisplayName("Standard " + platform + " " + type);
                    environment.setDescription("Standard environment for " + platform + " " + type);
                    environment.setPlatform(platform);
                    environment.setEnvironmentType(type);
                    environment.setActive(false); // Inactive by default
                    environment.setCreatedBy("SYSTEM");
                    
                    environmentRepository.save(environment);
                    log.debug("Created standard environment: {}", name);
                }
            }
        }
    }

    /**
     * Validates environment data.
     */
    private void validateEnvironment(Environment environment) {
        if (environment.getName() == null || environment.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Environment name is required");
        }
        
        if (environment.getPlatform() == null || environment.getPlatform().trim().isEmpty()) {
            throw new IllegalArgumentException("Platform is required");
        }
        
        // Check for duplicate name (excluding current environment)
        Optional<Environment> existingEnvironment = findByName(environment.getName());
        if (existingEnvironment.isPresent() && 
            !existingEnvironment.get().getId().equals(environment.getId())) {
            throw new IllegalArgumentException("Environment name already exists: " + environment.getName());
        }
    }
}