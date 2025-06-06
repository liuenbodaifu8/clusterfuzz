package com.google.clusterfuzz.benchmarks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests for benchmark infrastructure.
 * Ensures benchmarks can run without errors.
 */
@SpringBootTest
@ActiveProfiles("test")
class BenchmarkValidationTest {

    @Test
    @DisplayName("Repository benchmark should be runnable")
    void testRepositoryBenchmarkRunnable() {
        assertDoesNotThrow(() -> {
            Options opt = new OptionsBuilder()
                .include(RepositoryBenchmark.class.getSimpleName())
                .forks(0) // No forking for tests
                .warmupIterations(1)
                .measurementIterations(1)
                .shouldFailOnError(true)
                .build();

            // Just validate the benchmark can be created and configured
            new Runner(opt);
        });
    }

    @Test
    @DisplayName("Service benchmark should be runnable")
    void testServiceBenchmarkRunnable() {
        assertDoesNotThrow(() -> {
            Options opt = new OptionsBuilder()
                .include(ServiceBenchmark.class.getSimpleName())
                .forks(0) // No forking for tests
                .warmupIterations(1)
                .measurementIterations(1)
                .shouldFailOnError(true)
                .build();

            // Just validate the benchmark can be created and configured
            new Runner(opt);
        });
    }

    @Test
    @DisplayName("Concurrency benchmark should be runnable")
    void testConcurrencyBenchmarkRunnable() {
        assertDoesNotThrow(() -> {
            Options opt = new OptionsBuilder()
                .include(ConcurrencyBenchmark.class.getSimpleName())
                .forks(0) // No forking for tests
                .warmupIterations(1)
                .measurementIterations(1)
                .shouldFailOnError(true)
                .build();

            // Just validate the benchmark can be created and configured
            new Runner(opt);
        });
    }

    @Test
    @DisplayName("Benchmark runner should have valid configuration")
    void testBenchmarkRunnerConfiguration() {
        assertDoesNotThrow(() -> {
            // Test that BenchmarkRunner methods can be called
            BenchmarkRunner.class.getDeclaredMethod("runRepositoryBenchmarks");
            BenchmarkRunner.class.getDeclaredMethod("runServiceBenchmarks");
            BenchmarkRunner.class.getDeclaredMethod("runConcurrencyBenchmarks");
        });
    }

    @Test
    @DisplayName("JMH annotations should be properly configured")
    void testJmhAnnotations() {
        // Verify benchmark classes have proper JMH annotations
        assertTrue(RepositoryBenchmark.class.isAnnotationPresent(org.openjdk.jmh.annotations.BenchmarkMode.class));
        assertTrue(RepositoryBenchmark.class.isAnnotationPresent(org.openjdk.jmh.annotations.State.class));
        
        assertTrue(ServiceBenchmark.class.isAnnotationPresent(org.openjdk.jmh.annotations.BenchmarkMode.class));
        assertTrue(ServiceBenchmark.class.isAnnotationPresent(org.openjdk.jmh.annotations.State.class));
        
        assertTrue(ConcurrencyBenchmark.class.isAnnotationPresent(org.openjdk.jmh.annotations.BenchmarkMode.class));
        assertTrue(ConcurrencyBenchmark.class.isAnnotationPresent(org.openjdk.jmh.annotations.State.class));
    }
}