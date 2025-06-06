package com.google.clusterfuzz.core.config.environment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Environment detector for identifying runtime environment characteristics.
 * Based on clusterfuzz._internal.system.environment module.
 */
@Component
@Slf4j
public class EnvironmentDetector {

    private final Environment springEnvironment;

    public EnvironmentDetector(Environment springEnvironment) {
        this.springEnvironment = springEnvironment;
    }

    /**
     * Detects the current platform.
     * Based on environment.platform() function.
     */
    public String detectPlatform() {
        // Check for OS override first
        String osOverride = System.getenv("OS_OVERRIDE");
        if (osOverride != null && !osOverride.trim().isEmpty()) {
            return osOverride.toUpperCase();
        }

        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return "WINDOWS";
        } else if (osName.contains("linux")) {
            return "LINUX";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return "MAC";
        } else if (osName.contains("android")) {
            return "ANDROID";
        }
        
        log.warn("Unsupported platform: {}", osName);
        return "UNKNOWN";
    }

    /**
     * Detects platform ID.
     * Based on environment.get_platform_id() function.
     */
    public String detectPlatformId() {
        String platform = detectPlatform();
        
        // Check for platform ID override
        String platformId = System.getenv("PLATFORM_ID");
        if (platformId != null && !platformId.trim().isEmpty()) {
            return platformId.toLowerCase();
        }
        
        if (isAndroid(platform)) {
            // For Android, try to get more specific platform ID
            String androidPlatformId = detectAndroidPlatformId();
            return androidPlatformId != null ? androidPlatformId : platform.toLowerCase();
        }
        
        return platform.toLowerCase();
    }

    /**
     * Detects if running on App Engine.
     * Based on environment.is_running_on_app_engine() function.
     */
    public boolean isRunningOnAppEngine() {
        // Check for GAE environment variables
        String gaeEnv = System.getenv("GAE_ENV");
        String gaeApplication = System.getenv("GAE_APPLICATION");
        String gaeService = System.getenv("GAE_SERVICE");
        
        return (gaeEnv != null && !gaeEnv.trim().isEmpty()) ||
               (gaeApplication != null && !gaeApplication.trim().isEmpty()) ||
               (gaeService != null && !gaeService.trim().isEmpty());
    }

    /**
     * Detects if running on App Engine development server.
     * Based on environment.is_running_on_app_engine_development() function.
     */
    public boolean isRunningOnAppEngineDevelopment() {
        String gaeEnv = System.getenv("GAE_ENV");
        return "localdev".equals(gaeEnv);
    }

    /**
     * Detects if running on Kubernetes.
     * Based on environment.is_running_on_k8s() function.
     */
    public boolean isRunningOnKubernetes() {
        // Check for Kubernetes environment variables
        String kubernetesServiceHost = System.getenv("KUBERNETES_SERVICE_HOST");
        String kubernetesServicePort = System.getenv("KUBERNETES_SERVICE_PORT");
        
        // Check for Kubernetes service account token
        File serviceAccountToken = new File("/var/run/secrets/kubernetes.io/serviceaccount/token");
        
        return (kubernetesServiceHost != null && kubernetesServicePort != null) ||
               serviceAccountToken.exists();
    }

    /**
     * Detects if this is a trusted host.
     * Based on environment.is_trusted_host() function.
     */
    public boolean isTrustedHost() {
        // Check for trusted host environment variable
        String trustedHost = System.getenv("TRUSTED_HOST");
        if ("true".equalsIgnoreCase(trustedHost) || "1".equals(trustedHost)) {
            return true;
        }
        
        // Check if running on App Engine (trusted by default)
        if (isRunningOnAppEngine()) {
            return true;
        }
        
        // Check if running on Kubernetes (trusted by default)
        if (isRunningOnKubernetes()) {
            return true;
        }
        
        return false;
    }

    /**
     * Detects if this is a development environment.
     */
    public boolean isDevelopmentEnvironment() {
        // Check Spring profiles
        String[] activeProfiles = springEnvironment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equalsIgnoreCase(profile) || 
                "development".equalsIgnoreCase(profile) ||
                "local".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        
        // Check for development environment variables
        String env = System.getenv("ENVIRONMENT");
        if ("development".equalsIgnoreCase(env) || "dev".equalsIgnoreCase(env)) {
            return true;
        }
        
        // Check if running on App Engine development
        return isRunningOnAppEngineDevelopment();
    }

    /**
     * Detects environment type.
     */
    public String detectEnvironmentType() {
        if (isDevelopmentEnvironment()) {
            return "DEVELOPMENT";
        }
        
        String[] activeProfiles = springEnvironment.getActiveProfiles();
        for (String profile : activeProfiles) {
            switch (profile.toLowerCase()) {
                case "prod":
                case "production":
                    return "PRODUCTION";
                case "staging":
                case "stage":
                    return "STAGING";
                case "test":
                case "testing":
                    return "TESTING";
                case "local":
                    return "LOCAL";
            }
        }
        
        // Check environment variable
        String env = System.getenv("ENVIRONMENT");
        if (env != null) {
            return env.toUpperCase();
        }
        
        return "UNKNOWN";
    }

    /**
     * Gets configuration directory.
     * Based on environment.get_config_directory() function.
     */
    public String getConfigDirectory() {
        String configDir = System.getenv("CONFIG_DIR");
        if (configDir != null && !configDir.trim().isEmpty()) {
            return configDir;
        }
        
        // Default config directory based on platform
        String rootDir = getRootDirectory();
        if (rootDir != null) {
            return rootDir + File.separator + "src" + File.separator + "appengine" + File.separator + "config";
        }
        
        return null;
    }

    /**
     * Gets root directory.
     * Based on environment.get_root_directory() function.
     */
    public String getRootDirectory() {
        String rootDir = System.getenv("ROOT_DIR");
        if (rootDir != null && !rootDir.trim().isEmpty()) {
            return rootDir;
        }
        
        // Try to detect from current working directory
        String currentDir = System.getProperty("user.dir");
        if (currentDir != null) {
            // Look for clusterfuzz root indicators
            File currentFile = new File(currentDir);
            while (currentFile != null) {
                if (new File(currentFile, "src").exists() && 
                    new File(currentFile, "clusterfuzz").exists()) {
                    return currentFile.getAbsolutePath();
                }
                currentFile = currentFile.getParentFile();
            }
        }
        
        return currentDir;
    }

    /**
     * Gets resources directory.
     * Based on environment.get_resources_directory() function.
     */
    public String getResourcesDirectory() {
        String rootDir = getRootDirectory();
        if (rootDir != null) {
            return rootDir + File.separator + "resources";
        }
        return null;
    }

    /**
     * Gets platform-specific resources directory.
     * Based on environment.get_platform_resources_directory() function.
     */
    public String getPlatformResourcesDirectory(String platformOverride) {
        String platform = platformOverride != null ? platformOverride : detectPlatform();
        
        // Android resources share the same android directory
        if (isAndroid(platform)) {
            platform = "ANDROID";
        }
        
        String resourcesDir = getResourcesDirectory();
        if (resourcesDir != null) {
            return resourcesDir + File.separator + "platform" + File.separator + platform.toLowerCase();
        }
        
        return null;
    }

    /**
     * Detects supported memory tools.
     * Based on SUPPORTED_MEMORY_TOOLS_FOR_OPTIONS constant.
     */
    public String[] getSupportedMemoryTools() {
        return new String[]{
            "HWASAN", "ASAN", "KASAN", "CFI", "MSAN", "TSAN", "UBSAN", "NOSANITIZER", "MTE"
        };
    }

    /**
     * Gets common sanitizer options.
     * Based on COMMON_SANITIZER_OPTIONS constant.
     */
    public Map<String, Object> getCommonSanitizerOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("handle_abort", 1);
        options.put("handle_segv", 1);
        options.put("handle_sigbus", 1);
        options.put("handle_sigfpe", 1);
        options.put("handle_sigill", 1);
        options.put("print_summary", 1);
        options.put("use_sigaltstack", 1);
        return options;
    }

    /**
     * Gets hostname.
     */
    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Failed to get hostname", e);
            return "unknown";
        }
    }

    /**
     * Gets IP address.
     */
    public String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Failed to get IP address", e);
            return "unknown";
        }
    }

    /**
     * Checks if platform is Android.
     */
    private boolean isAndroid(String platform) {
        return platform != null && platform.toUpperCase().contains("ANDROID");
    }

    /**
     * Detects Android platform ID.
     */
    private String detectAndroidPlatformId() {
        // Try to get Android-specific platform information
        String androidProduct = System.getenv("ANDROID_PRODUCT");
        String androidDevice = System.getenv("ANDROID_DEVICE");
        
        if (androidProduct != null && !androidProduct.trim().isEmpty()) {
            return androidProduct.toLowerCase();
        }
        
        if (androidDevice != null && !androidDevice.trim().isEmpty()) {
            return androidDevice.toLowerCase();
        }
        
        return null;
    }
}