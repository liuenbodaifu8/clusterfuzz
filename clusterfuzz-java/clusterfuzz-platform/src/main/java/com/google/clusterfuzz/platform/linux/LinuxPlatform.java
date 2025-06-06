package com.google.clusterfuzz.platform.linux;

import com.google.clusterfuzz.platform.common.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Linux platform implementation providing Linux-specific operations.
 */
@Component
@Slf4j
public class LinuxPlatform implements Platform {

    private LinuxFileSystemOperations fileSystem;
    private LinuxProcessManager processManager;
    private LinuxEnvironmentManager environmentManager;
    private LinuxResourceMonitor resourceMonitor;
    private PlatformConfig config;

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.LINUX;
    }

    @Override
    public String getPlatformName() {
        return "Linux";
    }

    @Override
    public boolean isCurrentPlatform() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    @Override
    public FileSystemOperations getFileSystem() {
        return fileSystem;
    }

    @Override
    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Override
    public EnvironmentManager getEnvironmentManager() {
        return environmentManager;
    }

    @Override
    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }

    @Override
    public void initialize(PlatformConfig config) {
        this.config = config;
        this.fileSystem = new LinuxFileSystemOperations();
        this.processManager = new LinuxProcessManager();
        this.environmentManager = new LinuxEnvironmentManager();
        this.resourceMonitor = new LinuxResourceMonitor();

        log.info("Initialized Linux platform with config: {}", config);
    }

    @Override
    public void cleanup() {
        if (resourceMonitor != null) {
            resourceMonitor.cleanup();
        }
        if (processManager != null) {
            processManager.cleanup();
        }
        log.info("Cleaned up Linux platform resources");
    }

    @Override
    public Path getTempDirectory() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir == null) {
            tmpDir = "/tmp";
        }
        return Paths.get(tmpDir);
    }

    @Override
    public String getExecutableExtension() {
        return ""; // Linux executables have no extension
    }

    @Override
    public String getLibraryExtension() {
        return ".so";
    }

    @Override
    public String getPathSeparator() {
        return ":";
    }

    @Override
    public String getEnvironmentSeparator() {
        return ":";
    }

    @Override
    public String toPlatformPath(String path) {
        return path.replace("\\", "/");
    }

    @Override
    public SystemInfo getSystemInfo() {
        return SystemInfo.builder()
            .osName("Linux")
            .osVersion(System.getProperty("os.version"))
            .architecture(System.getProperty("os.arch"))
            .availableProcessors(Runtime.getRuntime().availableProcessors())
            .maxMemory(Runtime.getRuntime().maxMemory())
            .build();
    }

    @Override
    public boolean isBinaryCompatible(Path binaryPath) {
        // Check if binary is ELF format and compatible with current architecture
        try {
            ProcessBuilder pb = new ProcessBuilder("file", binaryPath.toString());
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Parse file output to check for ELF and architecture compatibility
                // This is a simplified check
                return true;
            }
        } catch (Exception e) {
            log.warn("Failed to check binary compatibility for {}", binaryPath, e);
        }
        return false;
    }

    @Override
    public List<String> getDebugTools() {
        return List.of("gdb", "lldb", "strace", "valgrind", "perf");
    }

    @Override
    public Map<String, Boolean> getSanitizerSupport() {
        return Map.of(
            "AddressSanitizer", true,
            "MemorySanitizer", true,
            "ThreadSanitizer", true,
            "UndefinedBehaviorSanitizer", true,
            "LeakSanitizer", true
        );
    }

    @Override
    public Map<String, Boolean> getFuzzingEngineSupport() {
        return Map.of(
            "libFuzzer", true,
            "AFL", true,
            "AFL++", true,
            "honggfuzz", true,
            "Centipede", true
        );
    }
}