package com.google.clusterfuzz.fuzzing.engine;

import com.google.clusterfuzz.core.entity.FuzzingTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Selects the best fuzzing engine for a given task.
 */
@Component
@Slf4j
public class FuzzingEngineSelector {

    /**
     * Selects the best engine for the given task from available engines.
     */
    public String selectEngine(FuzzingTask task, Set<String> availableEngines) {
        if (task == null || availableEngines == null || availableEngines.isEmpty()) {
            return null;
        }

        // If task specifies a job type that matches an engine, use it
        String jobType = task.getJobType();
        if (jobType != null && availableEngines.contains(jobType)) {
            return jobType;
        }

        // Default selection logic based on task characteristics
        if (task.getMaxExecutionTime() != null) {
            if (task.getMaxExecutionTime() < 300) { // < 5 minutes
                if (availableEngines.contains("libfuzzer")) {
                    return "libfuzzer";
                }
            } else if (task.getMaxExecutionTime() < 3600) { // < 1 hour
                if (availableEngines.contains("afl")) {
                    return "afl";
                }
            } else { // Long running tasks
                if (availableEngines.contains("honggfuzz")) {
                    return "honggfuzz";
                }
            }
        }

        // Fallback to first available engine
        return availableEngines.iterator().next();
    }
}