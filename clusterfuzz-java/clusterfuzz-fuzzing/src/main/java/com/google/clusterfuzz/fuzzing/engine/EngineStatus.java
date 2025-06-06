package com.google.clusterfuzz.fuzzing.engine;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Status information for a fuzzing engine.
 */
@Data
@Builder
public class EngineStatus {
    private String name;
    private String version;
    private boolean healthy;
    private boolean supported;
    private String errorMessage;
    private Instant lastHealthCheck;
    private long totalTasksExecuted;
    private long successfulTasks;
    private long failedTasks;
}