package com.google.clusterfuzz.platform.common;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Abstract platform interface defining common operations across all supported platforms.
 * Implementations provide platform-specific behavior for Linux, Windows, Android, and macOS.
 */
public interface Platform {

    /**
     * Gets the platform type.
     */
    PlatformType getPlatformType();

    /**
     * Gets the platform name.
     */
    String getPlatformName();

    /**
     * Checks if this platform is currently running.
     */
    boolean isCurrentPlatform();

    /**
     * Gets platform-specific file system operations.
     */
    FileSystemOperations getFileSystem();

    /**
     * Gets platform-specific process management.
     */
    ProcessManager getProcessManager();

    /**
     * Gets platform-specific environment operations.
     */
    EnvironmentManager getEnvironmentManager();

    /**
     * Gets platform-specific resource monitoring.
     */
    ResourceMonitor getResourceMonitor();

    /**
     * Initializes the platform with configuration.
     */
    void initialize(PlatformConfig config);

    /**
     * Cleans up platform resources.
     */
    void cleanup();

    /**
     * Gets platform-specific temporary directory.
     */
    Path getTempDirectory();

    /**
     * Gets platform-specific executable extension.
     */
    String getExecutableExtension();

    /**
     * Gets platform-specific library extension.
     */
    String getLibraryExtension();

    /**
     * Gets platform-specific path separator.
     */
    String getPathSeparator();

    /**
     * Gets platform-specific environment variable separator.
     */
    String getEnvironmentSeparator();

    /**
     * Converts a path to platform-specific format.
     */
    String toPlatformPath(String path);

    /**
     * Gets platform-specific system information.
     */
    SystemInfo getSystemInfo();

    /**
     * Checks if a binary is compatible with this platform.
     */
    boolean isBinaryCompatible(Path binaryPath);

    /**
     * Gets platform-specific debugging tools.
     */
    List<String> getDebugTools();

    /**
     * Gets platform-specific sanitizer support.
     */
    Map<String, Boolean> getSanitizerSupport();

    /**
     * Gets platform-specific fuzzing engine support.
     */
    Map<String, Boolean> getFuzzingEngineSupport();
}