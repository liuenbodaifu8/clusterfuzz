package com.google.clusterfuzz.core.config.management;

import com.google.clusterfuzz.core.entity.Configuration;
import com.google.clusterfuzz.core.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Configuration version service for managing configuration history and rollback.
 * Provides versioning capabilities for configuration changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConfigurationVersionService {

    private final ConfigurationRepository configurationRepository;
    
    // In-memory version history (in production, this should be persisted)
    private final Map<String, List<ConfigurationVersion>> versionHistory = new HashMap<>();
    private final Map<String, Integer> currentVersions = new HashMap<>();

    /**
     * Creates a new version of a configuration.
     */
    public ConfigurationVersion createVersion(Configuration configuration, String changeReason, String changedBy) {
        String key = configuration.getKey();
        
        ConfigurationVersion version = new ConfigurationVersion();
        version.setConfigurationKey(key);
        version.setValue(configuration.getValue());
        version.setDefaultValue(configuration.getDefaultValue());
        version.setDescription(configuration.getDescription());
        version.setCategory(configuration.getCategory());
        version.setDataType(configuration.getDataType());
        version.setEnvironment(configuration.getEnvironment());
        version.setSensitive(configuration.getSensitive());
        version.setEncrypted(configuration.getEncrypted());
        version.setActive(configuration.getActive());
        version.setValidationRegex(configuration.getValidationRegex());
        version.setMinValue(configuration.getMinValue());
        version.setMaxValue(configuration.getMaxValue());
        version.setAllowedValues(configuration.getAllowedValues());
        version.setChangeReason(changeReason);
        version.setChangedBy(changedBy);
        version.setCreatedAt(LocalDateTime.now());
        
        // Assign version number
        int versionNumber = getNextVersionNumber(key);
        version.setVersionNumber(versionNumber);
        
        // Store version
        versionHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(version);
        currentVersions.put(key, versionNumber);
        
        log.info("Created version {} for configuration: {} by {}", versionNumber, key, changedBy);
        
        return version;
    }

    /**
     * Gets all versions for a configuration.
     */
    @Transactional(readOnly = true)
    public List<ConfigurationVersion> getVersionHistory(String configurationKey) {
        List<ConfigurationVersion> versions = versionHistory.get(configurationKey);
        return versions != null ? new ArrayList<>(versions) : new ArrayList<>();
    }

    /**
     * Gets a specific version of a configuration.
     */
    @Transactional(readOnly = true)
    public Optional<ConfigurationVersion> getVersion(String configurationKey, int versionNumber) {
        List<ConfigurationVersion> versions = versionHistory.get(configurationKey);
        if (versions != null) {
            return versions.stream()
                .filter(v -> v.getVersionNumber() == versionNumber)
                .findFirst();
        }
        return Optional.empty();
    }

    /**
     * Gets the latest version of a configuration.
     */
    @Transactional(readOnly = true)
    public Optional<ConfigurationVersion> getLatestVersion(String configurationKey) {
        List<ConfigurationVersion> versions = versionHistory.get(configurationKey);
        if (versions != null && !versions.isEmpty()) {
            return Optional.of(versions.get(versions.size() - 1));
        }
        return Optional.empty();
    }

    /**
     * Gets the current version number for a configuration.
     */
    @Transactional(readOnly = true)
    public int getCurrentVersionNumber(String configurationKey) {
        return currentVersions.getOrDefault(configurationKey, 0);
    }

    /**
     * Rolls back a configuration to a previous version.
     */
    public Configuration rollbackToVersion(String configurationKey, int versionNumber, String rolledBackBy) {
        Optional<ConfigurationVersion> versionOpt = getVersion(configurationKey, versionNumber);
        if (versionOpt.isEmpty()) {
            throw new IllegalArgumentException("Version " + versionNumber + " not found for configuration: " + configurationKey);
        }
        
        ConfigurationVersion version = versionOpt.get();
        
        // Get current configuration
        Optional<Configuration> configOpt = configurationRepository.findByKey(configurationKey);
        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException("Configuration not found: " + configurationKey);
        }
        
        Configuration configuration = configOpt.get();
        
        // Create version for current state before rollback
        createVersion(configuration, "Pre-rollback backup", rolledBackBy);
        
        // Apply version values to configuration
        configuration.setValue(version.getValue());
        configuration.setDefaultValue(version.getDefaultValue());
        configuration.setDescription(version.getDescription());
        configuration.setCategory(version.getCategory());
        configuration.setDataType(version.getDataType());
        configuration.setEnvironment(version.getEnvironment());
        configuration.setSensitive(version.getSensitive());
        configuration.setEncrypted(version.getEncrypted());
        configuration.setActive(version.getActive());
        configuration.setValidationRegex(version.getValidationRegex());
        configuration.setMinValue(version.getMinValue());
        configuration.setMaxValue(version.getMaxValue());
        configuration.setAllowedValues(version.getAllowedValues());
        configuration.setUpdatedBy(rolledBackBy);
        
        Configuration saved = configurationRepository.save(configuration);
        
        // Create version for rollback
        createVersion(saved, "Rolled back to version " + versionNumber, rolledBackBy);
        
        log.info("Rolled back configuration {} to version {} by {}", configurationKey, versionNumber, rolledBackBy);
        
        return saved;
    }

    /**
     * Compares two versions of a configuration.
     */
    @Transactional(readOnly = true)
    public ConfigurationComparison compareVersions(String configurationKey, int version1, int version2) {
        Optional<ConfigurationVersion> v1Opt = getVersion(configurationKey, version1);
        Optional<ConfigurationVersion> v2Opt = getVersion(configurationKey, version2);
        
        if (v1Opt.isEmpty() || v2Opt.isEmpty()) {
            throw new IllegalArgumentException("One or both versions not found");
        }
        
        ConfigurationVersion v1 = v1Opt.get();
        ConfigurationVersion v2 = v2Opt.get();
        
        ConfigurationComparison comparison = new ConfigurationComparison();
        comparison.setConfigurationKey(configurationKey);
        comparison.setVersion1(version1);
        comparison.setVersion2(version2);
        comparison.setDifferences(findDifferences(v1, v2));
        
        return comparison;
    }

    /**
     * Gets version statistics for a configuration.
     */
    @Transactional(readOnly = true)
    public ConfigurationVersionStats getVersionStats(String configurationKey) {
        List<ConfigurationVersion> versions = getVersionHistory(configurationKey);
        
        ConfigurationVersionStats stats = new ConfigurationVersionStats();
        stats.setConfigurationKey(configurationKey);
        stats.setTotalVersions(versions.size());
        stats.setCurrentVersion(getCurrentVersionNumber(configurationKey));
        
        if (!versions.isEmpty()) {
            stats.setFirstVersionDate(versions.get(0).getCreatedAt());
            stats.setLastVersionDate(versions.get(versions.size() - 1).getCreatedAt());
            
            // Count changes by user
            Map<String, Long> changesByUser = versions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    ConfigurationVersion::getChangedBy,
                    java.util.stream.Collectors.counting()
                ));
            stats.setChangesByUser(changesByUser);
            
            // Count changes by reason
            Map<String, Long> changesByReason = versions.stream()
                .filter(v -> v.getChangeReason() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    ConfigurationVersion::getChangeReason,
                    java.util.stream.Collectors.counting()
                ));
            stats.setChangesByReason(changesByReason);
        }
        
        return stats;
    }

    /**
     * Purges old versions beyond a certain limit.
     */
    public void purgeOldVersions(String configurationKey, int keepVersions) {
        List<ConfigurationVersion> versions = versionHistory.get(configurationKey);
        if (versions != null && versions.size() > keepVersions) {
            int toRemove = versions.size() - keepVersions;
            for (int i = 0; i < toRemove; i++) {
                versions.remove(0); // Remove oldest versions
            }
            log.info("Purged {} old versions for configuration: {}", toRemove, configurationKey);
        }
    }

    /**
     * Exports version history for backup.
     */
    @Transactional(readOnly = true)
    public Map<String, List<ConfigurationVersion>> exportVersionHistory() {
        Map<String, List<ConfigurationVersion>> export = new HashMap<>();
        for (Map.Entry<String, List<ConfigurationVersion>> entry : versionHistory.entrySet()) {
            export.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return export;
    }

    /**
     * Imports version history from backup.
     */
    public void importVersionHistory(Map<String, List<ConfigurationVersion>> importData) {
        versionHistory.clear();
        currentVersions.clear();
        
        for (Map.Entry<String, List<ConfigurationVersion>> entry : importData.entrySet()) {
            String key = entry.getKey();
            List<ConfigurationVersion> versions = entry.getValue();
            
            versionHistory.put(key, new ArrayList<>(versions));
            
            if (!versions.isEmpty()) {
                int maxVersion = versions.stream()
                    .mapToInt(ConfigurationVersion::getVersionNumber)
                    .max()
                    .orElse(0);
                currentVersions.put(key, maxVersion);
            }
        }
        
        log.info("Imported version history for {} configurations", importData.size());
    }

    /**
     * Gets next version number for a configuration.
     */
    private int getNextVersionNumber(String configurationKey) {
        int current = currentVersions.getOrDefault(configurationKey, 0);
        return current + 1;
    }

    /**
     * Finds differences between two configuration versions.
     */
    private List<String> findDifferences(ConfigurationVersion v1, ConfigurationVersion v2) {
        List<String> differences = new ArrayList<>();
        
        if (!Objects.equals(v1.getValue(), v2.getValue())) {
            differences.add("Value: '" + v1.getValue() + "' -> '" + v2.getValue() + "'");
        }
        
        if (!Objects.equals(v1.getDefaultValue(), v2.getDefaultValue())) {
            differences.add("Default Value: '" + v1.getDefaultValue() + "' -> '" + v2.getDefaultValue() + "'");
        }
        
        if (!Objects.equals(v1.getDescription(), v2.getDescription())) {
            differences.add("Description: '" + v1.getDescription() + "' -> '" + v2.getDescription() + "'");
        }
        
        if (!Objects.equals(v1.getCategory(), v2.getCategory())) {
            differences.add("Category: '" + v1.getCategory() + "' -> '" + v2.getCategory() + "'");
        }
        
        if (!Objects.equals(v1.getDataType(), v2.getDataType())) {
            differences.add("Data Type: '" + v1.getDataType() + "' -> '" + v2.getDataType() + "'");
        }
        
        if (!Objects.equals(v1.getEnvironment(), v2.getEnvironment())) {
            differences.add("Environment: '" + v1.getEnvironment() + "' -> '" + v2.getEnvironment() + "'");
        }
        
        if (!Objects.equals(v1.getSensitive(), v2.getSensitive())) {
            differences.add("Sensitive: " + v1.getSensitive() + " -> " + v2.getSensitive());
        }
        
        if (!Objects.equals(v1.getActive(), v2.getActive())) {
            differences.add("Active: " + v1.getActive() + " -> " + v2.getActive());
        }
        
        return differences;
    }

    /**
     * Configuration version data class.
     */
    public static class ConfigurationVersion {
        private String configurationKey;
        private int versionNumber;
        private String value;
        private String defaultValue;
        private String description;
        private String category;
        private String dataType;
        private String environment;
        private Boolean sensitive;
        private Boolean encrypted;
        private Boolean active;
        private String validationRegex;
        private Double minValue;
        private Double maxValue;
        private String allowedValues;
        private String changeReason;
        private String changedBy;
        private LocalDateTime createdAt;

        // Getters and setters
        public String getConfigurationKey() { return configurationKey; }
        public void setConfigurationKey(String configurationKey) { this.configurationKey = configurationKey; }

        public int getVersionNumber() { return versionNumber; }
        public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public Boolean getSensitive() { return sensitive; }
        public void setSensitive(Boolean sensitive) { this.sensitive = sensitive; }

        public Boolean getEncrypted() { return encrypted; }
        public void setEncrypted(Boolean encrypted) { this.encrypted = encrypted; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }

        public String getValidationRegex() { return validationRegex; }
        public void setValidationRegex(String validationRegex) { this.validationRegex = validationRegex; }

        public Double getMinValue() { return minValue; }
        public void setMinValue(Double minValue) { this.minValue = minValue; }

        public Double getMaxValue() { return maxValue; }
        public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }

        public String getAllowedValues() { return allowedValues; }
        public void setAllowedValues(String allowedValues) { this.allowedValues = allowedValues; }

        public String getChangeReason() { return changeReason; }
        public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

        public String getChangedBy() { return changedBy; }
        public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Configuration comparison result.
     */
    public static class ConfigurationComparison {
        private String configurationKey;
        private int version1;
        private int version2;
        private List<String> differences;

        // Getters and setters
        public String getConfigurationKey() { return configurationKey; }
        public void setConfigurationKey(String configurationKey) { this.configurationKey = configurationKey; }

        public int getVersion1() { return version1; }
        public void setVersion1(int version1) { this.version1 = version1; }

        public int getVersion2() { return version2; }
        public void setVersion2(int version2) { this.version2 = version2; }

        public List<String> getDifferences() { return differences; }
        public void setDifferences(List<String> differences) { this.differences = differences; }
    }

    /**
     * Configuration version statistics.
     */
    public static class ConfigurationVersionStats {
        private String configurationKey;
        private int totalVersions;
        private int currentVersion;
        private LocalDateTime firstVersionDate;
        private LocalDateTime lastVersionDate;
        private Map<String, Long> changesByUser;
        private Map<String, Long> changesByReason;

        // Getters and setters
        public String getConfigurationKey() { return configurationKey; }
        public void setConfigurationKey(String configurationKey) { this.configurationKey = configurationKey; }

        public int getTotalVersions() { return totalVersions; }
        public void setTotalVersions(int totalVersions) { this.totalVersions = totalVersions; }

        public int getCurrentVersion() { return currentVersion; }
        public void setCurrentVersion(int currentVersion) { this.currentVersion = currentVersion; }

        public LocalDateTime getFirstVersionDate() { return firstVersionDate; }
        public void setFirstVersionDate(LocalDateTime firstVersionDate) { this.firstVersionDate = firstVersionDate; }

        public LocalDateTime getLastVersionDate() { return lastVersionDate; }
        public void setLastVersionDate(LocalDateTime lastVersionDate) { this.lastVersionDate = lastVersionDate; }

        public Map<String, Long> getChangesByUser() { return changesByUser; }
        public void setChangesByUser(Map<String, Long> changesByUser) { this.changesByUser = changesByUser; }

        public Map<String, Long> getChangesByReason() { return changesByReason; }
        public void setChangesByReason(Map<String, Long> changesByReason) { this.changesByReason = changesByReason; }
    }
}