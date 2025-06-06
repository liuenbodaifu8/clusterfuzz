package com.google.clusterfuzz.core.service;

import com.google.clusterfuzz.core.entity.Configuration;
import com.google.clusterfuzz.core.repository.ConfigurationRepository;
import com.google.clusterfuzz.core.security.encryption.ConfigurationEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing application configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;
    private final ConfigurationEncryptionService encryptionService;
    private final Environment environment;

    /**
     * Initializes default configurations.
     */
    @PostConstruct
    public void initializeDefaultConfigurations() {
        log.info("Initializing default configurations");
        createDefaultConfigurations();
    }

    /**
     * Gets configuration value by key.
     */
    @Cacheable(value = "configurations", key = "#key")
    @Transactional(readOnly = true)
    public String getValue(String key) {
        return getValue(key, null);
    }

    /**
     * Gets configuration value by key with default.
     */
    @Cacheable(value = "configurations", key = "#key + ':' + #defaultValue")
    @Transactional(readOnly = true)
    public String getValue(String key, String defaultValue) {
        Optional<Configuration> config = findByKey(key);
        if (config.isPresent()) {
            Configuration configuration = config.get();
            String value = configuration.getEffectiveValue();
            
            // Decrypt if encrypted
            if (configuration.getEncrypted() && value != null) {
                try {
                    value = encryptionService.decrypt(value);
                } catch (Exception e) {
                    log.error("Failed to decrypt configuration: {}", key, e);
                    return defaultValue;
                }
            }
            
            return value != null ? value : defaultValue;
        }
        return defaultValue;
    }

    /**
     * Gets configuration value as boolean.
     */
    @Transactional(readOnly = true)
    public Boolean getBooleanValue(String key, Boolean defaultValue) {
        String value = getValue(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets configuration value as integer.
     */
    @Transactional(readOnly = true)
    public Integer getIntegerValue(String key, Integer defaultValue) {
        String value = getValue(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid integer configuration value for key {}: {}", key, value);
            return defaultValue;
        }
    }

    /**
     * Gets configuration value as long.
     */
    @Transactional(readOnly = true)
    public Long getLongValue(String key, Long defaultValue) {
        String value = getValue(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid long configuration value for key {}: {}", key, value);
            return defaultValue;
        }
    }

    /**
     * Gets configuration value as double.
     */
    @Transactional(readOnly = true)
    public Double getDoubleValue(String key, Double defaultValue) {
        String value = getValue(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid double configuration value for key {}: {}", key, value);
            return defaultValue;
        }
    }

    /**
     * Sets configuration value.
     */
    @CacheEvict(value = "configurations", allEntries = true)
    public void setValue(String key, String value, String updatedBy) {
        Optional<Configuration> configOpt = findByKey(key);
        
        if (configOpt.isPresent()) {
            Configuration config = configOpt.get();
            
            // Validate value
            if (!config.isValidValue(value)) {
                throw new IllegalArgumentException("Invalid value for configuration: " + key);
            }
            
            // Encrypt if sensitive
            String valueToStore = value;
            if (config.getSensitive() && value != null) {
                try {
                    valueToStore = encryptionService.encrypt(value);
                    config.setEncrypted(true);
                } catch (Exception e) {
                    log.error("Failed to encrypt configuration: {}", key, e);
                    throw new RuntimeException("Failed to encrypt sensitive configuration", e);
                }
            }
            
            config.setValue(valueToStore);
            config.setUpdatedBy(updatedBy);
            configurationRepository.save(config);
            
            log.info("Updated configuration: {} by {}", key, updatedBy);
        } else {
            throw new IllegalArgumentException("Configuration not found: " + key);
        }
    }

    /**
     * Creates a new configuration.
     */
    @CacheEvict(value = "configurations", allEntries = true)
    public Configuration createConfiguration(Configuration configuration) {
        validateConfiguration(configuration);
        
        // Encrypt value if sensitive
        if (configuration.getSensitive() && configuration.getValue() != null) {
            try {
                String encryptedValue = encryptionService.encrypt(configuration.getValue());
                configuration.setValue(encryptedValue);
                configuration.setEncrypted(true);
            } catch (Exception e) {
                log.error("Failed to encrypt configuration: {}", configuration.getKey(), e);
                throw new RuntimeException("Failed to encrypt sensitive configuration", e);
            }
        }
        
        Configuration saved = configurationRepository.save(configuration);
        log.info("Created configuration: {}", saved.getKey());
        
        return saved;
    }

    /**
     * Updates an existing configuration.
     */
    @CacheEvict(value = "configurations", allEntries = true)
    public Configuration updateConfiguration(Configuration configuration) {
        validateConfiguration(configuration);
        
        Configuration existing = configurationRepository.findById(configuration.getId())
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configuration.getId()));
        
        // Prevent modification of system configurations
        if (existing.getSystemConfig()) {
            throw new IllegalArgumentException("Cannot modify system configuration: " + existing.getKey());
        }
        
        // Handle encryption
        if (configuration.getSensitive() && configuration.getValue() != null) {
            try {
                String encryptedValue = encryptionService.encrypt(configuration.getValue());
                configuration.setValue(encryptedValue);
                configuration.setEncrypted(true);
            } catch (Exception e) {
                log.error("Failed to encrypt configuration: {}", configuration.getKey(), e);
                throw new RuntimeException("Failed to encrypt sensitive configuration", e);
            }
        }
        
        Configuration saved = configurationRepository.save(configuration);
        log.info("Updated configuration: {}", saved.getKey());
        
        return saved;
    }

    /**
     * Deletes a configuration.
     */
    @CacheEvict(value = "configurations", allEntries = true)
    public void deleteConfiguration(Long configurationId) {
        Configuration configuration = configurationRepository.findById(configurationId)
            .orElseThrow(() -> new IllegalArgumentException("Configuration not found: " + configurationId));
        
        // Prevent deletion of system configurations
        if (configuration.getSystemConfig()) {
            throw new IllegalArgumentException("Cannot delete system configuration: " + configuration.getKey());
        }
        
        configurationRepository.delete(configuration);
        log.info("Deleted configuration: {}", configuration.getKey());
    }

    /**
     * Finds configuration by key.
     */
    @Cacheable(value = "configurations", key = "'entity:' + #key")
    @Transactional(readOnly = true)
    public Optional<Configuration> findByKey(String key) {
        return configurationRepository.findByKey(key);
    }

    /**
     * Gets all configurations.
     */
    @Transactional(readOnly = true)
    public List<Configuration> findAll() {
        return configurationRepository.findAll();
    }

    /**
     * Gets configurations by category.
     */
    @Cacheable(value = "configurations", key = "'category:' + #category")
    @Transactional(readOnly = true)
    public List<Configuration> findByCategory(String category) {
        return configurationRepository.findByCategory(category);
    }

    /**
     * Gets configurations by environment.
     */
    @Cacheable(value = "configurations", key = "'environment:' + #environment")
    @Transactional(readOnly = true)
    public List<Configuration> findByEnvironment(String environment) {
        return configurationRepository.findByEnvironmentOrEnvironment(environment, "ALL");
    }

    /**
     * Gets active configurations.
     */
    @Cacheable(value = "configurations", key = "'active'")
    @Transactional(readOnly = true)
    public List<Configuration> findAllActive() {
        return configurationRepository.findByActiveTrue();
    }

    /**
     * Resets configuration to default value.
     */
    @CacheEvict(value = "configurations", allEntries = true)
    public void resetToDefault(String key, String updatedBy) {
        Optional<Configuration> configOpt = findByKey(key);
        if (configOpt.isPresent()) {
            Configuration config = configOpt.get();
            config.setValue(null); // This will make getEffectiveValue() return defaultValue
            config.setEncrypted(false);
            config.setUpdatedBy(updatedBy);
            configurationRepository.save(config);
            
            log.info("Reset configuration to default: {} by {}", key, updatedBy);
        }
    }

    /**
     * Exports configurations for backup.
     */
    @Transactional(readOnly = true)
    public List<Configuration> exportConfigurations(boolean includeSensitive) {
        List<Configuration> configurations = findAllActive();
        
        if (!includeSensitive) {
            // Remove sensitive values
            configurations.forEach(config -> {
                if (config.getSensitive()) {
                    config.setValue("[HIDDEN]");
                }
            });
        }
        
        return configurations;
    }

    /**
     * Creates default configurations.
     */
    private void createDefaultConfigurations() {
        // Security configurations
        createConfigIfNotExists("security.jwt.secret", null, "JWT secret key", 
            Configuration.Category.SECURITY, Configuration.DataType.PASSWORD, true, true);
        
        createConfigIfNotExists("security.oauth2.google.client-id", null, "Google OAuth2 client ID",
            Configuration.Category.SECURITY, Configuration.DataType.STRING, false, false);
        
        createConfigIfNotExists("security.oauth2.google.client-secret", null, "Google OAuth2 client secret",
            Configuration.Category.SECURITY, Configuration.DataType.PASSWORD, true, true);
        
        createConfigIfNotExists("security.session.timeout", "1800", "Session timeout in seconds",
            Configuration.Category.SECURITY, Configuration.DataType.INTEGER, false, true);
        
        // Fuzzing configurations
        createConfigIfNotExists("fuzzing.max-testcases-per-job", "1000", "Maximum testcases per fuzzing job",
            Configuration.Category.FUZZING, Configuration.DataType.INTEGER, false, true);
        
        createConfigIfNotExists("fuzzing.timeout-per-testcase", "30", "Timeout per testcase in seconds",
            Configuration.Category.FUZZING, Configuration.DataType.INTEGER, false, true);
        
        createConfigIfNotExists("fuzzing.max-concurrent-jobs", "10", "Maximum concurrent fuzzing jobs",
            Configuration.Category.FUZZING, Configuration.DataType.INTEGER, false, true);
        
        // Storage configurations
        createConfigIfNotExists("storage.gcs.bucket", null, "Google Cloud Storage bucket name",
            Configuration.Category.STORAGE, Configuration.DataType.STRING, false, false);
        
        createConfigIfNotExists("storage.max-file-size", "104857600", "Maximum file size in bytes (100MB)",
            Configuration.Category.STORAGE, Configuration.DataType.LONG, false, true);
        
        // Notification configurations
        createConfigIfNotExists("notification.email.enabled", "false", "Enable email notifications",
            Configuration.Category.NOTIFICATION, Configuration.DataType.BOOLEAN, false, true);
        
        createConfigIfNotExists("notification.slack.webhook", null, "Slack webhook URL",
            Configuration.Category.NOTIFICATION, Configuration.DataType.URL, true, false);
        
        // Performance configurations
        createConfigIfNotExists("performance.cache.ttl", "3600", "Cache TTL in seconds",
            Configuration.Category.PERFORMANCE, Configuration.DataType.INTEGER, false, true);
        
        createConfigIfNotExists("performance.max-threads", "20", "Maximum thread pool size",
            Configuration.Category.PERFORMANCE, Configuration.DataType.INTEGER, false, true);
        
        log.info("Default configurations created");
    }

    /**
     * Creates configuration if it doesn't exist.
     */
    private void createConfigIfNotExists(String key, String defaultValue, String description,
                                       String category, String dataType, boolean sensitive, boolean systemConfig) {
        if (configurationRepository.findByKey(key).isEmpty()) {
            Configuration config = new Configuration();
            config.setKey(key);
            config.setDefaultValue(defaultValue);
            config.setDescription(description);
            config.setCategory(category);
            config.setDataType(dataType);
            config.setSensitive(sensitive);
            config.setSystemConfig(systemConfig);
            config.setActive(true);
            config.setCreatedBy("SYSTEM");
            
            configurationRepository.save(config);
            log.debug("Created default configuration: {}", key);
        }
    }

    /**
     * Validates configuration data.
     */
    private void validateConfiguration(Configuration configuration) {
        if (configuration.getKey() == null || configuration.getKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration key is required");
        }
        
        if (configuration.getValue() != null && !configuration.isValidValue(configuration.getValue())) {
            throw new IllegalArgumentException("Invalid value for configuration: " + configuration.getKey());
        }
        
        // Check for duplicate key (excluding current configuration)
        Optional<Configuration> existingConfig = findByKey(configuration.getKey());
        if (existingConfig.isPresent() && !existingConfig.get().getId().equals(configuration.getId())) {
            throw new IllegalArgumentException("Configuration key already exists: " + configuration.getKey());
        }
    }
}