package com.google.clusterfuzz.benchmarks;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Main class to run all ClusterFuzz performance benchmarks.
 * 
 * Usage:
 * - Run all benchmarks: java -jar clusterfuzz-benchmarks.jar
 * - Run specific benchmark: java -jar clusterfuzz-benchmarks.jar RepositoryBenchmark
 * - Run with custom options: java -jar clusterfuzz-benchmarks.jar -wi 5 -i 10
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(".*Benchmark.*")
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .jvmArgs("-Xmx4g", "-XX:+UseG1GC")
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();

        new Runner(opt).run();
    }

    /**
     * Run only repository benchmarks for quick testing.
     */
    public static void runRepositoryBenchmarks() throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(RepositoryBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(2)
            .measurementIterations(3)
            .jvmArgs("-Xmx2g")
            .build();

        new Runner(opt).run();
    }

    /**
     * Run only service benchmarks for quick testing.
     */
    public static void runServiceBenchmarks() throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ServiceBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(2)
            .measurementIterations(3)
            .jvmArgs("-Xmx2g")
            .build();

        new Runner(opt).run();
    }

    /**
     * Run concurrency benchmarks with extended measurement time.
     */
    public static void runConcurrencyBenchmarks() throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ConcurrencyBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .jvmArgs("-Xmx4g", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100")
            .build();

        new Runner(opt).run();
    }
}