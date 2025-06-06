package com.google.clusterfuzz.fuzzing.engine;

import com.google.clusterfuzz.core.entity.FuzzingTask;
import com.google.clusterfuzz.core.fuzzing.FuzzingEngine;
import com.google.clusterfuzz.core.fuzzing.FuzzingResult;
import com.google.clusterfuzz.platform.common.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central manager for all fuzzing engines.
 * Handles engine selection, initialization, and execution coordination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FuzzingEngineManager {

    private final List<FuzzingEngine> availableEngines;
    private final Platform platform;
    private final FuzzingEngineSelector engineSelector;
    
    private Map<String, FuzzingEngine> engineMap;

    @PostConstruct
    public void initialize() {
        engineMap = new HashMap<>();
        
        for (FuzzingEngine engine : availableEngines) {
            if (isEngineSupported(engine)) {
                engineMap.put(engine.getName(), engine);
                log.info("Registered fuzzing engine: {}", engine.getName());
            } else {
                log.warn("Fuzzing engine {} not supported on platform {}", 
                    engine.getName(), platform.getPlatformName());
            }
        }
        
        log.info("Initialized fuzzing engine manager with {} engines", engineMap.size());
    }

    /**
     * Selects the best fuzzing engine for a given task.
     */
    public Optional<FuzzingEngine> selectEngine(FuzzingTask task) {
        String selectedEngineName = engineSelector.selectEngine(task, engineMap.keySet());
        
        if (selectedEngineName == null) {
            log.warn("No suitable fuzzing engine found for task {}", task.getId());
            return Optional.empty();
        }
        
        FuzzingEngine engine = engineMap.get(selectedEngineName);
        if (engine == null) {
            log.error("Selected engine {} not found in registry", selectedEngineName);
            return Optional.empty();
        }
        
        log.info("Selected fuzzing engine {} for task {}", selectedEngineName, task.getId());
        return Optional.of(engine);
    }

    /**
     * Executes a fuzzing task with the appropriate engine.
     */
    public FuzzingResult executeFuzzingTask(FuzzingTask task) {
        Optional<FuzzingEngine> engineOpt = selectEngine(task);
        
        if (engineOpt.isEmpty()) {
            return FuzzingResult.builder()
                .taskId(task.getId())
                .success(false)
                .errorMessage("No suitable fuzzing engine available")
                .build();
        }
        
        FuzzingEngine engine = engineOpt.get();
        
        try {
            log.info("Starting fuzzing task {} with engine {}", task.getId(), engine.getName());
            return engine.fuzz(task);
            
        } catch (Exception e) {
            log.error("Fuzzing task {} failed with engine {}", task.getId(), engine.getName(), e);
            return FuzzingResult.builder()
                .taskId(task.getId())
                .success(false)
                .errorMessage("Fuzzing execution failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Gets all available fuzzing engines.
     */
    public Map<String, FuzzingEngine> getAvailableEngines() {
        return Map.copyOf(engineMap);
    }

    /**
     * Gets a specific fuzzing engine by name.
     */
    public Optional<FuzzingEngine> getEngine(String engineName) {
        return Optional.ofNullable(engineMap.get(engineName));
    }

    /**
     * Checks if an engine is supported on the current platform.
     */
    private boolean isEngineSupported(FuzzingEngine engine) {
        Map<String, Boolean> platformSupport = platform.getFuzzingEngineSupport();
        return platformSupport.getOrDefault(engine.getName(), false);
    }

    /**
     * Gets engine statistics and health information.
     */
    public Map<String, EngineStatus> getEngineStatuses() {
        Map<String, EngineStatus> statuses = new HashMap<>();
        
        for (Map.Entry<String, FuzzingEngine> entry : engineMap.entrySet()) {
            String engineName = entry.getKey();
            FuzzingEngine engine = entry.getValue();
            
            try {
                boolean healthy = engine.isHealthy();
                String version = engine.getVersion();
                
                statuses.put(engineName, EngineStatus.builder()
                    .name(engineName)
                    .healthy(healthy)
                    .version(version)
                    .supported(true)
                    .build());
                    
            } catch (Exception e) {
                log.warn("Failed to get status for engine {}", engineName, e);
                statuses.put(engineName, EngineStatus.builder()
                    .name(engineName)
                    .healthy(false)
                    .supported(true)
                    .errorMessage(e.getMessage())
                    .build());
            }
        }
        
        return statuses;
    }

    /**
     * Validates that all required engines are available and healthy.
     */
    public boolean validateEngines() {
        if (engineMap.isEmpty()) {
            log.error("No fuzzing engines available");
            return false;
        }
        
        boolean allHealthy = true;
        for (Map.Entry<String, FuzzingEngine> entry : engineMap.entrySet()) {
            String engineName = entry.getKey();
            FuzzingEngine engine = entry.getValue();
            
            try {
                if (!engine.isHealthy()) {
                    log.error("Fuzzing engine {} is not healthy", engineName);
                    allHealthy = false;
                }
            } catch (Exception e) {
                log.error("Failed to check health of engine {}", engineName, e);
                allHealthy = false;
            }
        }
        
        return allHealthy;
    }

    /**
     * Shuts down all fuzzing engines gracefully.
     */
    public void shutdown() {
        log.info("Shutting down fuzzing engine manager");
        
        for (Map.Entry<String, FuzzingEngine> entry : engineMap.entrySet()) {
            String engineName = entry.getKey();
            FuzzingEngine engine = entry.getValue();
            
            try {
                engine.cleanup();
                log.info("Shut down fuzzing engine: {}", engineName);
            } catch (Exception e) {
                log.error("Failed to shut down engine {}", engineName, e);
            }
        }
        
        engineMap.clear();
    }
}