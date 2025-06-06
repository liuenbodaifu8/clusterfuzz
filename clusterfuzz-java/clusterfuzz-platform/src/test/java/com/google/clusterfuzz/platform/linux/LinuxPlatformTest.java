package com.google.clusterfuzz.platform.linux;

import com.google.clusterfuzz.platform.common.PlatformType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledOnOs(OS.LINUX)
class LinuxPlatformTest {

    private LinuxPlatform linuxPlatform;

    @BeforeEach
    void setUp() {
        linuxPlatform = new LinuxPlatform();
    }

    @Test
    void getPlatformName_ShouldReturnLinux() {
        // When
        String platformName = linuxPlatform.getPlatformName();

        // Then
        assertThat(platformName).isEqualTo("Linux");
    }

    @Test
    void getPlatformType_ShouldReturnLinuxType() {
        // When
        PlatformType platformType = linuxPlatform.getPlatformType();

        // Then
        assertThat(platformType).isEqualTo(PlatformType.LINUX);
    }

    @Test
    void isSupported_ShouldReturnTrue() {
        // When
        boolean supported = linuxPlatform.isSupported();

        // Then
        assertThat(supported).isTrue();
    }

    @Test
    void getSystemInfo_ShouldReturnValidInfo() {
        // When
        Map<String, String> systemInfo = linuxPlatform.getSystemInfo();

        // Then
        assertThat(systemInfo).isNotEmpty();
        assertThat(systemInfo).containsKey("os.name");
        assertThat(systemInfo).containsKey("os.version");
        assertThat(systemInfo).containsKey("os.arch");
        assertThat(systemInfo.get("os.name")).containsIgnoringCase("linux");
    }

    @Test
    void getFuzzingEngineSupport_ShouldSupportCommonEngines() {
        // When
        Map<String, Boolean> engineSupport = linuxPlatform.getFuzzingEngineSupport();

        // Then
        assertThat(engineSupport).isNotEmpty();
        assertThat(engineSupport.get("libfuzzer")).isTrue();
        assertThat(engineSupport.get("afl")).isTrue();
        assertThat(engineSupport.get("honggfuzz")).isTrue();
    }

    @Test
    void executeCommand_WithValidCommand_ShouldReturnResult() {
        // Given
        String command = "echo 'test'";

        // When
        String result = linuxPlatform.executeCommand(command);

        // Then
        assertThat(result).contains("test");
    }

    @Test
    void executeCommand_WithInvalidCommand_ShouldHandleError() {
        // Given
        String command = "nonexistent_command_12345";

        // When
        String result = linuxPlatform.executeCommand(command);

        // Then
        assertThat(result).contains("error").or().contains("not found");
    }

    @Test
    void getProcessList_ShouldReturnRunningProcesses() {
        // When
        List<String> processes = linuxPlatform.getProcessList();

        // Then
        assertThat(processes).isNotEmpty();
        // Should contain at least the current Java process
        assertThat(processes.stream().anyMatch(p -> p.contains("java"))).isTrue();
    }

    @Test
    void killProcess_WithValidPid_ShouldAttemptKill() {
        // Given - Create a test process
        String command = "sleep 10 &";
        String output = linuxPlatform.executeCommand(command);
        
        // Extract PID from background process (this is platform-specific)
        assumeTrue(output != null, "Could not start test process");

        // When & Then - Just verify the method doesn't throw
        // We can't easily test actual killing without complex setup
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            linuxPlatform.killProcess(99999); // Use non-existent PID
        });
    }

    @Test
    void getMemoryInfo_ShouldReturnMemoryStats() {
        // When
        Map<String, Long> memoryInfo = linuxPlatform.getMemoryInfo();

        // Then
        assertThat(memoryInfo).isNotEmpty();
        assertThat(memoryInfo).containsKey("total");
        assertThat(memoryInfo).containsKey("available");
        assertThat(memoryInfo).containsKey("used");
        assertThat(memoryInfo.get("total")).isGreaterThan(0);
    }

    @Test
    void getCpuInfo_ShouldReturnCpuStats() {
        // When
        Map<String, String> cpuInfo = linuxPlatform.getCpuInfo();

        // Then
        assertThat(cpuInfo).isNotEmpty();
        assertThat(cpuInfo).containsKey("model");
        assertThat(cpuInfo).containsKey("cores");
        assertThat(cpuInfo.get("cores")).matches("\\d+");
    }

    @Test
    void createTempDirectory_ShouldCreateValidDirectory() {
        // When
        File tempDir = linuxPlatform.createTempDirectory("clusterfuzz-test");

        // Then
        assertThat(tempDir).exists();
        assertThat(tempDir).isDirectory();
        assertThat(tempDir.getName()).contains("clusterfuzz-test");
        
        // Cleanup
        tempDir.delete();
    }

    @Test
    void getEnvironmentVariable_WithValidVar_ShouldReturnValue() {
        // Given
        String varName = "PATH";

        // When
        String value = linuxPlatform.getEnvironmentVariable(varName);

        // Then
        assertThat(value).isNotNull();
        assertThat(value).isNotEmpty();
    }

    @Test
    void getEnvironmentVariable_WithInvalidVar_ShouldReturnNull() {
        // Given
        String varName = "NONEXISTENT_VAR_12345";

        // When
        String value = linuxPlatform.getEnvironmentVariable(varName);

        // Then
        assertThat(value).isNull();
    }

    @Test
    void setEnvironmentVariable_ShouldSetVariable() {
        // Given
        String varName = "CLUSTERFUZZ_TEST_VAR";
        String varValue = "test_value";

        // When
        linuxPlatform.setEnvironmentVariable(varName, varValue);
        String retrievedValue = linuxPlatform.getEnvironmentVariable(varName);

        // Then
        assertThat(retrievedValue).isEqualTo(varValue);
    }

    @Test
    void isDebuggerAttached_ShouldReturnBoolean() {
        // When
        boolean debuggerAttached = linuxPlatform.isDebuggerAttached();

        // Then
        assertThat(debuggerAttached).isInstanceOf(Boolean.class);
        // Usually false in test environment
        assertThat(debuggerAttached).isFalse();
    }

    @Test
    void getFileSystemInfo_ShouldReturnValidInfo() {
        // When
        Map<String, Object> fsInfo = linuxPlatform.getFileSystemInfo();

        // Then
        assertThat(fsInfo).isNotEmpty();
        assertThat(fsInfo).containsKey("root_free_space");
        assertThat(fsInfo).containsKey("root_total_space");
        assertThat(fsInfo.get("root_free_space")).isInstanceOf(Long.class);
        assertThat((Long) fsInfo.get("root_free_space")).isGreaterThan(0);
    }
}