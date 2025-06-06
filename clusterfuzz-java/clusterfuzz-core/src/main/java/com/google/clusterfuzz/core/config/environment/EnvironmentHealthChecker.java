package com.google.clusterfuzz.core.config.environment;

import com.google.clusterfuzz.core.entity.Environment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Environment health checker for monitoring environment status.
 * Performs comprehensive health checks on environment configurations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnvironmentHealthChecker {

    private final EnvironmentDetector environmentDetector;

    /**
     * Performs comprehensive health check on an environment.
     */
    @Async
    public CompletableFuture<HealthCheckResult> performHealthCheck(Environment environment) {
        log.debug("Starting health check for environment: {}", environment.getName());
        
        HealthCheckResult result = new HealthCheckResult();
        result.setEnvironmentId(environment.getId());
        result.setEnvironmentName(environment.getName());
        result.setCheckTime(LocalDateTime.now());
        
        List<String> issues = new ArrayList<>();
        Map<String, Object> details = new HashMap<>();
        
        try {
            // Check basic environment properties
            checkBasicProperties(environment, issues, details);
            
            // Check directories and paths
            checkDirectories(environment, issues, details);
            
            // Check platform compatibility
            checkPlatformCompatibility(environment, issues, details);
            
            // Check memory tools configuration
            checkMemoryToolsConfiguration(environment, issues, details);
            
            // Check environment variables
            checkEnvironmentVariables(environment, issues, details);
            
            // Check network connectivity (if applicable)
            checkNetworkConnectivity(environment, issues, details);
            
            // Check resource availability
            checkResourceAvailability(environment, issues, details);
            
            // Determine overall health status
            String healthStatus = determineHealthStatus(issues);
            result.setHealthStatus(healthStatus);
            result.setIssues(issues);
            result.setDetails(details);
            result.setHealthy(Environment.HealthStatus.HEALTHY.equals(healthStatus));
            
            log.debug("Health check completed for environment: {} - Status: {}", 
                     environment.getName(), healthStatus);
            
        } catch (Exception e) {
            log.error("Health check failed for environment: {}", environment.getName(), e);
            result.setHealthStatus(Environment.HealthStatus.UNHEALTHY);
            result.setHealthy(false);
            issues.add("Health check failed with exception: " + e.getMessage());
            result.setIssues(issues);
        }
        
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Checks basic environment properties.
     */
    private void checkBasicProperties(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("basic_check_time", LocalDateTime.now());
        
        // Check if environment is active
        if (!environment.getActive()) {
            issues.add("Environment is marked as inactive");
        }
        
        // Check platform
        if (environment.getPlatform() == null || environment.getPlatform().trim().isEmpty()) {
            issues.add("Platform is not specified");
        } else {
            String detectedPlatform = environmentDetector.detectPlatform();
            details.put("detected_platform", detectedPlatform);
            details.put("configured_platform", environment.getPlatform());
            
            if (!environment.getPlatform().equals(detectedPlatform)) {
                issues.add(String.format("Platform mismatch: configured=%s, detected=%s", 
                                        environment.getPlatform(), detectedPlatform));
            }
        }
        
        // Check environment type
        if (environment.getEnvironmentType() == null || environment.getEnvironmentType().trim().isEmpty()) {
            issues.add("Environment type is not specified");
        }
        
        details.put("basic_properties_checked", true);
    }

    /**
     * Checks directories and paths.
     */
    private void checkDirectories(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("directory_check_time", LocalDateTime.now());
        
        // Check config directory
        String configDir = environment.getConfigDirectory();
        if (configDir != null) {
            if (!checkDirectoryExists(configDir)) {
                issues.add("Config directory does not exist: " + configDir);
            } else {
                details.put("config_directory_exists", true);
            }
        } else {
            // Try to detect config directory
            String detectedConfigDir = environmentDetector.getConfigDirectory();
            if (detectedConfigDir != null) {
                details.put("detected_config_directory", detectedConfigDir);
                if (!checkDirectoryExists(detectedConfigDir)) {
                    issues.add("Detected config directory does not exist: " + detectedConfigDir);
                }
            }
        }
        
        // Check resources directory
        String resourcesDir = environment.getResourcesDirectory();
        if (resourcesDir != null) {
            if (!checkDirectoryExists(resourcesDir)) {
                issues.add("Resources directory does not exist: " + resourcesDir);
            } else {
                details.put("resources_directory_exists", true);
            }
        }
        
        // Check root directory
        String rootDir = environment.getRootDirectory();
        if (rootDir != null) {
            if (!checkDirectoryExists(rootDir)) {
                issues.add("Root directory does not exist: " + rootDir);
            } else {
                details.put("root_directory_exists", true);
            }
        }
        
        details.put("directories_checked", true);
    }

    /**
     * Checks platform compatibility.
     */
    private void checkPlatformCompatibility(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("platform_check_time", LocalDateTime.now());
        
        String platform = environment.getPlatform();
        if (platform != null) {
            // Check if platform is supported
            String[] supportedPlatforms = {"WINDOWS", "LINUX", "MAC", "ANDROID", "CHROMEOS", "FUCHSIA"};
            boolean supported = false;
            for (String supportedPlatform : supportedPlatforms) {
                if (supportedPlatform.equals(platform)) {
                    supported = true;
                    break;
                }
            }
            
            if (!supported) {
                issues.add("Unsupported platform: " + platform);
            } else {
                details.put("platform_supported", true);
            }
            
            // Platform-specific checks
            if (environment.isAndroid()) {
                checkAndroidSpecific(environment, issues, details);
            } else if (environment.isWindows()) {
                checkWindowsSpecific(environment, issues, details);
            } else if (environment.isLinux()) {
                checkLinuxSpecific(environment, issues, details);
            } else if (environment.isMac()) {
                checkMacSpecific(environment, issues, details);
            }
        }
        
        details.put("platform_compatibility_checked", true);
    }

    /**
     * Checks memory tools configuration.
     */
    private void checkMemoryToolsConfiguration(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("memory_tools_check_time", LocalDateTime.now());
        
        String memoryTools = environment.getMemoryTools();
        if (memoryTools != null && !memoryTools.trim().isEmpty()) {
            String[] supportedTools = environmentDetector.getSupportedMemoryTools();
            String[] configuredTools = memoryTools.split(",");
            
            for (String tool : configuredTools) {
                String trimmedTool = tool.trim().toUpperCase();
                boolean supported = false;
                for (String supportedTool : supportedTools) {
                    if (supportedTool.equals(trimmedTool)) {
                        supported = true;
                        break;
                    }
                }
                
                if (!supported) {
                    issues.add("Unsupported memory tool: " + trimmedTool);
                }
            }
            
            details.put("memory_tools_configured", configuredTools.length);
        }
        
        // Check sanitizer options
        String sanitizerOptions = environment.getSanitizerOptions();
        if (sanitizerOptions != null && !sanitizerOptions.trim().isEmpty()) {
            try {
                Map<String, String> options = environment.getSanitizerOptionsMap();
                details.put("sanitizer_options_count", options.size());
                details.put("sanitizer_options_valid", true);
            } catch (Exception e) {
                issues.add("Invalid sanitizer options format: " + e.getMessage());
            }
        }
        
        details.put("memory_tools_checked", true);
    }

    /**
     * Checks environment variables.
     */
    private void checkEnvironmentVariables(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("env_vars_check_time", LocalDateTime.now());
        
        String envVars = environment.getEnvironmentVariables();
        if (envVars != null && !envVars.trim().isEmpty()) {
            try {
                Map<String, String> variables = environment.getEnvironmentVariablesMap();
                details.put("environment_variables_count", variables.size());
                
                // Check for required environment variables
                checkRequiredEnvironmentVariables(variables, issues, details);
                
                details.put("environment_variables_valid", true);
            } catch (Exception e) {
                issues.add("Invalid environment variables format: " + e.getMessage());
            }
        }
        
        details.put("environment_variables_checked", true);
    }

    /**
     * Checks network connectivity.
     */
    private void checkNetworkConnectivity(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("network_check_time", LocalDateTime.now());
        
        // Check if this is a networked environment
        if (environment.getAppEngine() || environment.getKubernetes()) {
            // Check basic network connectivity
            try {
                InetAddress.getByName("google.com");
                details.put("internet_connectivity", true);
            } catch (Exception e) {
                issues.add("No internet connectivity: " + e.getMessage());
                details.put("internet_connectivity", false);
            }
            
            // Check specific service connectivity if configured
            checkServiceConnectivity(environment, issues, details);
        }
        
        details.put("network_connectivity_checked", true);
    }

    /**
     * Checks resource availability.
     */
    private void checkResourceAvailability(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("resource_check_time", LocalDateTime.now());
        
        // Check disk space
        try {
            File rootFile = new File("/");
            long freeSpace = rootFile.getFreeSpace();
            long totalSpace = rootFile.getTotalSpace();
            double freePercentage = (double) freeSpace / totalSpace * 100;
            
            details.put("disk_free_percentage", freePercentage);
            details.put("disk_free_bytes", freeSpace);
            details.put("disk_total_bytes", totalSpace);
            
            if (freePercentage < 10) {
                issues.add(String.format("Low disk space: %.1f%% free", freePercentage));
            }
        } catch (Exception e) {
            log.warn("Failed to check disk space", e);
        }
        
        // Check memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercentage = (double) usedMemory / maxMemory * 100;
        
        details.put("memory_usage_percentage", memoryUsagePercentage);
        details.put("memory_used_bytes", usedMemory);
        details.put("memory_max_bytes", maxMemory);
        
        if (memoryUsagePercentage > 90) {
            issues.add(String.format("High memory usage: %.1f%%", memoryUsagePercentage));
        }
        
        details.put("resource_availability_checked", true);
    }

    /**
     * Determines overall health status based on issues.
     */
    private String determineHealthStatus(List<String> issues) {
        if (issues.isEmpty()) {
            return Environment.HealthStatus.HEALTHY;
        }
        
        // Count critical vs non-critical issues
        int criticalIssues = 0;
        for (String issue : issues) {
            if (isCriticalIssue(issue)) {
                criticalIssues++;
            }
        }
        
        if (criticalIssues > 0) {
            return Environment.HealthStatus.UNHEALTHY;
        } else if (issues.size() > 3) {
            return Environment.HealthStatus.DEGRADED;
        } else {
            return Environment.HealthStatus.DEGRADED;
        }
    }

    /**
     * Checks if an issue is critical.
     */
    private boolean isCriticalIssue(String issue) {
        String lowerIssue = issue.toLowerCase();
        return lowerIssue.contains("does not exist") ||
               lowerIssue.contains("failed") ||
               lowerIssue.contains("exception") ||
               lowerIssue.contains("unsupported platform") ||
               lowerIssue.contains("no internet connectivity");
    }

    /**
     * Checks if directory exists.
     */
    private boolean checkDirectoryExists(String directory) {
        try {
            Path path = Paths.get(directory);
            return Files.exists(path) && Files.isDirectory(path);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Android-specific checks.
     */
    private void checkAndroidSpecific(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("android_check_time", LocalDateTime.now());
        
        // Check for Android-specific environment variables
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome == null) {
            issues.add("ANDROID_HOME environment variable not set");
        } else {
            details.put("android_home", androidHome);
        }
        
        details.put("android_specific_checked", true);
    }

    /**
     * Windows-specific checks.
     */
    private void checkWindowsSpecific(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("windows_check_time", LocalDateTime.now());
        // Add Windows-specific checks here
        details.put("windows_specific_checked", true);
    }

    /**
     * Linux-specific checks.
     */
    private void checkLinuxSpecific(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("linux_check_time", LocalDateTime.now());
        // Add Linux-specific checks here
        details.put("linux_specific_checked", true);
    }

    /**
     * Mac-specific checks.
     */
    private void checkMacSpecific(Environment environment, List<String> issues, Map<String, Object> details) {
        details.put("mac_check_time", LocalDateTime.now());
        // Add Mac-specific checks here
        details.put("mac_specific_checked", true);
    }

    /**
     * Checks required environment variables.
     */
    private void checkRequiredEnvironmentVariables(Map<String, String> variables, List<String> issues, Map<String, Object> details) {
        // Define required variables based on environment type
        String[] requiredVars = {"PATH"};
        
        for (String requiredVar : requiredVars) {
            if (!variables.containsKey(requiredVar)) {
                issues.add("Required environment variable missing: " + requiredVar);
            }
        }
    }

    /**
     * Checks service connectivity.
     */
    private void checkServiceConnectivity(Environment environment, List<String> issues, Map<String, Object> details) {
        // Check Google Cloud services if applicable
        if (environment.getAppEngine()) {
            checkGoogleCloudConnectivity(issues, details);
        }
    }

    /**
     * Checks Google Cloud connectivity.
     */
    private void checkGoogleCloudConnectivity(List<String> issues, Map<String, Object> details) {
        try {
            // Try to connect to Google Cloud APIs
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress("googleapis.com", 443), 5000);
            socket.close();
            details.put("google_cloud_connectivity", true);
        } catch (IOException e) {
            issues.add("Cannot connect to Google Cloud services: " + e.getMessage());
            details.put("google_cloud_connectivity", false);
        }
    }

    /**
     * Health check result container.
     */
    public static class HealthCheckResult {
        private Long environmentId;
        private String environmentName;
        private LocalDateTime checkTime;
        private String healthStatus;
        private boolean healthy;
        private List<String> issues;
        private Map<String, Object> details;

        // Getters and setters
        public Long getEnvironmentId() { return environmentId; }
        public void setEnvironmentId(Long environmentId) { this.environmentId = environmentId; }

        public String getEnvironmentName() { return environmentName; }
        public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }

        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }

        public String getHealthStatus() { return healthStatus; }
        public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }

        public List<String> getIssues() { return issues; }
        public void setIssues(List<String> issues) { this.issues = issues; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}