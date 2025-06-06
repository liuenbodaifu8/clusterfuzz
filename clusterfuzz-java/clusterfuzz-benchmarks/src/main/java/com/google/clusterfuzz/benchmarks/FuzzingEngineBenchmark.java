package com.google.clusterfuzz.benchmarks;

import com.google.clusterfuzz.core.entity.FuzzingTask;
import com.google.clusterfuzz.core.fuzzing.FuzzingEngine;
import com.google.clusterfuzz.core.fuzzing.FuzzingResult;
import com.google.clusterfuzz.fuzzing.engine.FuzzingEngineManager;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for fuzzing engine performance.
 * Tests the performance of different fuzzing engines and engine management.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms1G", "-Xmx1G"})
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
public class FuzzingEngineBenchmark {

    private FuzzingEngineManager engineManager;
    private FuzzingTask lightweightTask;
    private FuzzingTask mediumTask;
    private FuzzingTask heavyTask;
    private MockFuzzingEngine mockEngine;

    @Setup
    public void setup() {
        // Setup mock fuzzing engine for benchmarking
        mockEngine = new MockFuzzingEngine();
        
        // Create different types of fuzzing tasks
        lightweightTask = createFuzzingTask("lightweight", 100);
        mediumTask = createFuzzingTask("medium", 1000);
        heavyTask = createFuzzingTask("heavy", 10000);
    }

    @Benchmark
    public void benchmarkEngineSelection(Blackhole bh) {
        // Benchmark engine selection algorithm
        String selectedEngine = selectBestEngine(lightweightTask);
        bh.consume(selectedEngine);
    }

    @Benchmark
    public void benchmarkLightweightFuzzing(Blackhole bh) {
        FuzzingResult result = mockEngine.fuzz(lightweightTask);
        bh.consume(result);
    }

    @Benchmark
    public void benchmarkMediumFuzzing(Blackhole bh) {
        FuzzingResult result = mockEngine.fuzz(mediumTask);
        bh.consume(result);
    }

    @Benchmark
    public void benchmarkHeavyFuzzing(Blackhole bh) {
        FuzzingResult result = mockEngine.fuzz(heavyTask);
        bh.consume(result);
    }

    @Benchmark
    @Group("concurrent")
    @GroupThreads(4)
    public void benchmarkConcurrentFuzzing(Blackhole bh) {
        FuzzingResult result = mockEngine.fuzz(lightweightTask);
        bh.consume(result);
    }

    @Benchmark
    public void benchmarkEngineInitialization(Blackhole bh) {
        MockFuzzingEngine engine = new MockFuzzingEngine();
        engine.initialize();
        bh.consume(engine);
    }

    @Benchmark
    public void benchmarkEngineHealthCheck(Blackhole bh) {
        boolean healthy = mockEngine.isHealthy();
        bh.consume(healthy);
    }

    @Benchmark
    public void benchmarkCorpusProcessing(Blackhole bh) {
        // Simulate corpus processing
        int processedFiles = processCorpus(100);
        bh.consume(processedFiles);
    }

    @Benchmark
    public void benchmarkCrashDetection(Blackhole bh) {
        // Simulate crash detection logic
        boolean crashDetected = detectCrash("mock_output_with_crash");
        bh.consume(crashDetected);
    }

    @Benchmark
    public void benchmarkResultAggregation(Blackhole bh) {
        // Benchmark result aggregation from multiple runs
        FuzzingResult aggregated = aggregateResults(
            mockEngine.fuzz(lightweightTask),
            mockEngine.fuzz(lightweightTask),
            mockEngine.fuzz(lightweightTask)
        );
        bh.consume(aggregated);
    }

    private FuzzingTask createFuzzingTask(String type, int complexity) {
        FuzzingTask task = new FuzzingTask();
        task.setId((long) type.hashCode());
        task.setJobType("libfuzzer");
        task.setTargetName("benchmark_target_" + type);
        task.setMaxExecutionTime(complexity);
        task.setMaxIterations(complexity * 10);
        return task;
    }

    private String selectBestEngine(FuzzingTask task) {
        // Mock engine selection logic
        if (task.getMaxExecutionTime() < 500) {
            return "libfuzzer";
        } else if (task.getMaxExecutionTime() < 5000) {
            return "afl";
        } else {
            return "honggfuzz";
        }
    }

    private int processCorpus(int fileCount) {
        // Simulate corpus file processing
        int processed = 0;
        for (int i = 0; i < fileCount; i++) {
            // Simulate file processing work
            String content = "mock_file_content_" + i;
            if (content.length() > 10) {
                processed++;
            }
        }
        return processed;
    }

    private boolean detectCrash(String output) {
        // Simulate crash detection in output
        return output.contains("crash") || 
               output.contains("segmentation fault") ||
               output.contains("AddressSanitizer");
    }

    private FuzzingResult aggregateResults(FuzzingResult... results) {
        FuzzingResult.FuzzingResultBuilder builder = FuzzingResult.builder();
        
        int totalCrashes = 0;
        int totalIterations = 0;
        boolean anySuccess = false;
        
        for (FuzzingResult result : results) {
            totalCrashes += result.getCrashesFound();
            totalIterations += result.getIterationsCompleted();
            anySuccess |= result.isSuccess();
        }
        
        return builder
            .taskId(results[0].getTaskId())
            .success(anySuccess)
            .crashesFound(totalCrashes)
            .iterationsCompleted(totalIterations)
            .build();
    }

    /**
     * Mock fuzzing engine for benchmarking purposes.
     */
    private static class MockFuzzingEngine implements FuzzingEngine {
        
        @Override
        public String getName() {
            return "mock";
        }

        @Override
        public String getVersion() {
            return "1.0.0";
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        @Override
        public void initialize() {
            // Mock initialization
        }

        @Override
        public void cleanup() {
            // Mock cleanup
        }

        @Override
        public FuzzingResult fuzz(FuzzingTask task) {
            // Simulate fuzzing work based on task complexity
            int complexity = task.getMaxExecutionTime();
            
            // Simulate CPU work
            long sum = 0;
            for (int i = 0; i < complexity; i++) {
                sum += i * i;
            }
            
            // Simulate random crash discovery
            int crashes = (int) (Math.random() * 3);
            
            return FuzzingResult.builder()
                .taskId(task.getId())
                .success(true)
                .crashesFound(crashes)
                .iterationsCompleted(complexity * 10)
                .executionTimeMs(complexity / 10)
                .build();
        }
    }
}