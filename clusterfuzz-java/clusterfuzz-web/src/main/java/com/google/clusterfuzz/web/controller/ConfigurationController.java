package com.google.clusterfuzz.web.controller;

import com.google.clusterfuzz.core.entity.Configuration;
import com.google.clusterfuzz.core.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST controller for configuration management.
 */
@RestController
@RequestMapping("/api/v1/configurations")
@RequiredArgsConstructor
@Slf4j
public class ConfigurationController {

    private final ConfigurationService configurationService;

    /**
     * Gets all configurations (admin only).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Configuration>> getAllConfigurations() {
        List<Configuration> configurations = configurationService.findAll();
        
        // Hide sensitive values for non-encrypted configurations
        configurations.forEach(config -> {
            if (config.getSensitive() && !config.getEncrypted()) {
                config.setValue("[HIDDEN]");
            }
        });
        
        return ResponseEntity.ok(configurations);
    }

    /**
     * Gets configurations by category.
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#category, 'config:read')")
    public ResponseEntity<List<Configuration>> getConfigurationsByCategory(@PathVariable String category) {
        List<Configuration> configurations = configurationService.findByCategory(category);
        
        // Hide sensitive values
        configurations.forEach(config -> {
            if (config.getSensitive()) {
                config.setValue("[HIDDEN]");
            }
        });
        
        return ResponseEntity.ok(configurations);
    }

    /**
     * Gets a specific configuration by key.
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#key, 'config:read')")
    public ResponseEntity<Configuration> getConfiguration(@PathVariable String key) {
        return configurationService.findByKey(key)
            .map(config -> {
                // Hide sensitive values
                if (config.getSensitive()) {
                    config.setValue("[HIDDEN]");
                }
                return ResponseEntity.ok(config);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets configuration value by key (for internal use).
     */
    @GetMapping("/{key}/value")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#key, 'config:read')")
    public ResponseEntity<Map<String, String>> getConfigurationValue(@PathVariable String key) {
        String value = configurationService.getValue(key);
        if (value != null) {
            return ResponseEntity.ok(Map.of("key", key, "value", value));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Creates a new configuration.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Configuration> createConfiguration(
            @Valid @RequestBody Configuration configuration,
            Authentication authentication) {
        
        configuration.setCreatedBy(authentication.getName());
        Configuration created = configurationService.createConfiguration(configuration);
        
        // Hide sensitive value in response
        if (created.getSensitive()) {
            created.setValue("[HIDDEN]");
        }
        
        return ResponseEntity.ok(created);
    }

    /**
     * Updates an existing configuration.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Configuration> updateConfiguration(
            @PathVariable Long id,
            @Valid @RequestBody Configuration configuration,
            Authentication authentication) {
        
        configuration.setId(id);
        configuration.setUpdatedBy(authentication.getName());
        Configuration updated = configurationService.updateConfiguration(configuration);
        
        // Hide sensitive value in response
        if (updated.getSensitive()) {
            updated.setValue("[HIDDEN]");
        }
        
        return ResponseEntity.ok(updated);
    }

    /**
     * Updates configuration value only.
     */
    @PatchMapping("/{key}/value")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#key, 'config:write')")
    public ResponseEntity<Map<String, String>> updateConfigurationValue(
            @PathVariable String key,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String value = request.get("value");
        configurationService.setValue(key, value, authentication.getName());
        
        return ResponseEntity.ok(Map.of("key", key, "status", "updated"));
    }

    /**
     * Resets configuration to default value.
     */
    @PostMapping("/{key}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetConfiguration(
            @PathVariable String key,
            Authentication authentication) {
        
        configurationService.resetToDefault(key, authentication.getName());
        return ResponseEntity.ok(Map.of("key", key, "status", "reset"));
    }

    /**
     * Deletes a configuration.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long id) {
        configurationService.deleteConfiguration(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exports configurations for backup.
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Configuration>> exportConfigurations(
            @RequestParam(defaultValue = "false") boolean includeSensitive) {
        
        List<Configuration> configurations = configurationService.exportConfigurations(includeSensitive);
        return ResponseEntity.ok(configurations);
    }

    /**
     * Gets configurations that need values.
     */
    @GetMapping("/incomplete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Configuration>> getIncompleteConfigurations() {
        // This would call a repository method to find configurations with null values
        List<Configuration> configurations = configurationService.findAll().stream()
            .filter(config -> config.getValue() == null && config.getDefaultValue() == null)
            .toList();
        
        return ResponseEntity.ok(configurations);
    }

    /**
     * Validates configuration value.
     */
    @PostMapping("/{key}/validate")
    @PreAuthorize("hasRole('ADMIN') or hasPermission(#key, 'config:read')")
    public ResponseEntity<Map<String, Object>> validateConfigurationValue(
            @PathVariable String key,
            @RequestBody Map<String, String> request) {
        
        return configurationService.findByKey(key)
            .map(config -> {
                String value = request.get("value");
                boolean isValid = config.isValidValue(value);
                
                return ResponseEntity.ok(Map.of(
                    "key", key,
                    "value", value,
                    "valid", isValid
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gets configuration statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfigurationStats() {
        List<Configuration> allConfigs = configurationService.findAll();
        
        long totalConfigs = allConfigs.size();
        long activeConfigs = allConfigs.stream().mapToLong(c -> c.getActive() ? 1 : 0).sum();
        long sensitiveConfigs = allConfigs.stream().mapToLong(c -> c.getSensitive() ? 1 : 0).sum();
        long systemConfigs = allConfigs.stream().mapToLong(c -> c.getSystemConfig() ? 1 : 0).sum();
        long configsWithValues = allConfigs.stream().mapToLong(c -> c.getValue() != null ? 1 : 0).sum();
        
        Map<String, Long> categoryCounts = allConfigs.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                c -> c.getCategory() != null ? c.getCategory() : "uncategorized",
                java.util.stream.Collectors.counting()
            ));
        
        return ResponseEntity.ok(Map.of(
            "total", totalConfigs,
            "active", activeConfigs,
            "sensitive", sensitiveConfigs,
            "system", systemConfigs,
            "withValues", configsWithValues,
            "byCategory", categoryCounts
        ));
    }
}