package com.google.clusterfuzz.fuzzing.engine;

import com.google.clusterfuzz.core.entity.FuzzingTask;
import com.google.clusterfuzz.core.fuzzing.FuzzingEngine;
import com.google.clusterfuzz.core.fuzzing.FuzzingResult;
import com.google.clusterfuzz.platform.common.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuzzingEngineManagerTest {

    @Mock
    private FuzzingEngine libFuzzerEngine;

    @Mock
    private FuzzingEngine aflEngine;

    @Mock
    private FuzzingEngine honggfuzzEngine;

    @Mock
    private Platform platform;

    @Mock
    private FuzzingEngineSelector engineSelector;

    private FuzzingEngineManager engineManager;
    private FuzzingTask testTask;

    @BeforeEach
    void setUp() {
        // Setup mock engines
        when(libFuzzerEngine.getName()).thenReturn("libfuzzer");
        when(aflEngine.getName()).thenReturn("afl");
        when(honggfuzzEngine.getName()).thenReturn("honggfuzz");

        // Setup platform support
        Map<String, Boolean> engineSupport = Map.of(
            "libfuzzer", true,
            "afl", true,
            "honggfuzz", true
        );
        when(platform.getFuzzingEngineSupport()).thenReturn(engineSupport);
        when(platform.getPlatformName()).thenReturn("Linux");

        // Create manager with mocked dependencies
        List<FuzzingEngine> engines = List.of(libFuzzerEngine, aflEngine, honggfuzzEngine);
        engineManager = new FuzzingEngineManager(engines, platform, engineSelector);
        
        // Initialize the manager
        engineManager.initialize();

        // Setup test task
        testTask = new FuzzingTask();
        testTask.setId(123L);
        testTask.setJobType("libfuzzer");
        testTask.setTargetName("test_target");
    }

    @Test
    void initialize_WithSupportedEngines_ShouldRegisterEngines() {
        // When
        Map<String, FuzzingEngine> availableEngines = engineManager.getAvailableEngines();

        // Then
        assertThat(availableEngines).hasSize(3);
        assertThat(availableEngines).containsKeys("libfuzzer", "afl", "honggfuzz");
        assertThat(availableEngines.get("libfuzzer")).isEqualTo(libFuzzerEngine);
        assertThat(availableEngines.get("afl")).isEqualTo(aflEngine);
        assertThat(availableEngines.get("honggfuzz")).isEqualTo(honggfuzzEngine);
    }

    @Test
    void selectEngine_WithValidTask_ShouldReturnSelectedEngine() {
        // Given
        when(engineSelector.selectEngine(any(FuzzingTask.class), anySet()))
            .thenReturn("libfuzzer");

        // When
        Optional<FuzzingEngine> selectedEngine = engineManager.selectEngine(testTask);

        // Then
        assertThat(selectedEngine).isPresent();
        assertThat(selectedEngine.get()).isEqualTo(libFuzzerEngine);
        verify(engineSelector).selectEngine(eq(testTask), any(Set.class));
    }

    @Test
    void selectEngine_WithNoSuitableEngine_ShouldReturnEmpty() {
        // Given
        when(engineSelector.selectEngine(any(FuzzingTask.class), anySet()))
            .thenReturn(null);

        // When
        Optional<FuzzingEngine> selectedEngine = engineManager.selectEngine(testTask);

        // Then
        assertThat(selectedEngine).isEmpty();
    }

    @Test
    void selectEngine_WithUnknownEngine_ShouldReturnEmpty() {
        // Given
        when(engineSelector.selectEngine(any(FuzzingTask.class), anySet()))
            .thenReturn("unknown_engine");

        // When
        Optional<FuzzingEngine> selectedEngine = engineManager.selectEngine(testTask);

        // Then
        assertThat(selectedEngine).isEmpty();
    }

    @Test
    void executeFuzzingTask_WithValidTask_ShouldExecuteSuccessfully() {
        // Given
        when(engineSelector.selectEngine(any(FuzzingTask.class), anySet()))
            .thenReturn("libfuzzer");
        
        FuzzingResult expectedResult = FuzzingResult.builder()
            .taskId(123L)
            .success(true)
            .crashesFound(5)
            .build();
        
        when(libFuzzerEngine.fuzz(testTask)).thenReturn(expectedResult);

        // When
        FuzzingResult result = engineManager.executeFuzzingTask(testTask);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(123L);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCrashesFound()).isEqualTo(5);
        verify(libFuzzerEngine).fuzz(testTask);
    }

    @Test
    void executeFuzzingTask_WithNoSuitableEngine_ShouldReturnFailureResult() {
        // Given
        when(engineSelector.selectEngine(any(FuzzingTask.class), anySet()))
            .thenReturn(null);

        // When
        FuzzingResult result = engineManager.executeFuzzingTask(testTask);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(123L);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("No suitable fuzzing engine available");
    }

    @Test
    void executeFuzzingTask_WithEngineException_ShouldReturnFailureResult() {
        // Given
        when(engineSelector.selectEngine(any(FuzzingTask.class), anySet()))
            .thenReturn("libfuzzer");
        when(libFuzzerEngine.fuzz(testTask))
            .thenThrow(new RuntimeException("Engine execution failed"));

        // When
        FuzzingResult result = engineManager.executeFuzzingTask(testTask);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(123L);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Fuzzing execution failed");
    }

    @Test
    void getEngine_WithValidName_ShouldReturnEngine() {
        // When
        Optional<FuzzingEngine> engine = engineManager.getEngine("libfuzzer");

        // Then
        assertThat(engine).isPresent();
        assertThat(engine.get()).isEqualTo(libFuzzerEngine);
    }

    @Test
    void getEngine_WithInvalidName_ShouldReturnEmpty() {
        // When
        Optional<FuzzingEngine> engine = engineManager.getEngine("nonexistent");

        // Then
        assertThat(engine).isEmpty();
    }

    @Test
    void getEngineStatuses_WithHealthyEngines_ShouldReturnStatuses() {
        // Given
        when(libFuzzerEngine.isHealthy()).thenReturn(true);
        when(libFuzzerEngine.getVersion()).thenReturn("1.0.0");
        when(aflEngine.isHealthy()).thenReturn(true);
        when(aflEngine.getVersion()).thenReturn("2.5.0");
        when(honggfuzzEngine.isHealthy()).thenReturn(false);
        when(honggfuzzEngine.getVersion()).thenReturn("3.0.0");

        // When
        Map<String, EngineStatus> statuses = engineManager.getEngineStatuses();

        // Then
        assertThat(statuses).hasSize(3);
        
        EngineStatus libFuzzerStatus = statuses.get("libfuzzer");
        assertThat(libFuzzerStatus.getName()).isEqualTo("libfuzzer");
        assertThat(libFuzzerStatus.isHealthy()).isTrue();
        assertThat(libFuzzerStatus.getVersion()).isEqualTo("1.0.0");
        assertThat(libFuzzerStatus.isSupported()).isTrue();

        EngineStatus honggfuzzStatus = statuses.get("honggfuzz");
        assertThat(honggfuzzStatus.isHealthy()).isFalse();
    }

    @Test
    void getEngineStatuses_WithEngineException_ShouldHandleGracefully() {
        // Given
        when(libFuzzerEngine.isHealthy()).thenThrow(new RuntimeException("Health check failed"));
        when(aflEngine.isHealthy()).thenReturn(true);
        when(aflEngine.getVersion()).thenReturn("2.5.0");

        // When
        Map<String, EngineStatus> statuses = engineManager.getEngineStatuses();

        // Then
        assertThat(statuses).hasSize(3);
        
        EngineStatus libFuzzerStatus = statuses.get("libfuzzer");
        assertThat(libFuzzerStatus.isHealthy()).isFalse();
        assertThat(libFuzzerStatus.getErrorMessage()).contains("Health check failed");
    }

    @Test
    void validateEngines_WithAllHealthyEngines_ShouldReturnTrue() {
        // Given
        when(libFuzzerEngine.isHealthy()).thenReturn(true);
        when(aflEngine.isHealthy()).thenReturn(true);
        when(honggfuzzEngine.isHealthy()).thenReturn(true);

        // When
        boolean valid = engineManager.validateEngines();

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    void validateEngines_WithUnhealthyEngine_ShouldReturnFalse() {
        // Given
        when(libFuzzerEngine.isHealthy()).thenReturn(true);
        when(aflEngine.isHealthy()).thenReturn(false);
        when(honggfuzzEngine.isHealthy()).thenReturn(true);

        // When
        boolean valid = engineManager.validateEngines();

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    void validateEngines_WithNoEngines_ShouldReturnFalse() {
        // Given - Create manager with no engines
        FuzzingEngineManager emptyManager = new FuzzingEngineManager(
            List.of(), platform, engineSelector);
        emptyManager.initialize();

        // When
        boolean valid = emptyManager.validateEngines();

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    void shutdown_ShouldCleanupAllEngines() {
        // When
        engineManager.shutdown();

        // Then
        verify(libFuzzerEngine).cleanup();
        verify(aflEngine).cleanup();
        verify(honggfuzzEngine).cleanup();
        
        // Verify engines are cleared
        assertThat(engineManager.getAvailableEngines()).isEmpty();
    }

    @Test
    void shutdown_WithEngineException_ShouldContinueCleanup() {
        // Given
        doThrow(new RuntimeException("Cleanup failed")).when(libFuzzerEngine).cleanup();

        // When
        engineManager.shutdown();

        // Then
        verify(libFuzzerEngine).cleanup();
        verify(aflEngine).cleanup();
        verify(honggfuzzEngine).cleanup();
        
        // Should still clear engines despite exception
        assertThat(engineManager.getAvailableEngines()).isEmpty();
    }
}