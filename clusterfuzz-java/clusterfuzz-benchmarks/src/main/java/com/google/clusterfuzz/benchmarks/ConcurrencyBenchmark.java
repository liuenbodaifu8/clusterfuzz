package com.google.clusterfuzz.benchmarks;

import com.google.clusterfuzz.core.entity.Testcase;
import com.google.clusterfuzz.core.repository.TestcaseRepository;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Performance benchmarks for concurrent operations.
 * Tests system behavior under high concurrency loads.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
public class ConcurrencyBenchmark {

    private ConfigurableApplicationContext context;
    private TestcaseRepository testcaseRepository;
    
    @Setup(Level.Trial)
    public void setup() {
        // Initialize Spring context with H2 database
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:concurrency;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.show-sql", "false");
        System.setProperty("spring.datasource.hikari.maximum-pool-size", "20");
        
        context = SpringApplication.run(BenchmarkApplication.class);
        testcaseRepository = context.getBean(TestcaseRepository.class);
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Benchmark
    @Threads(1)
    public void benchmarkSingleThreadedWrites(Blackhole bh) {
        IntStream.range(0, 50).forEach(i -> {
            Testcase testcase = createTestcase(i);
            Testcase saved = testcaseRepository.save(testcase);
            bh.consume(saved);
        });
    }

    @Benchmark
    @Threads(4)
    public void benchmarkMultiThreadedWrites(Blackhole bh) {
        IntStream.range(0, 50).forEach(i -> {
            Testcase testcase = createTestcase(i);
            Testcase saved = testcaseRepository.save(testcase);
            bh.consume(saved);
        });
    }

    @Benchmark
    @Threads(8)
    public void benchmarkHighConcurrencyWrites(Blackhole bh) {
        IntStream.range(0, 50).forEach(i -> {
            Testcase testcase = createTestcase(i);
            Testcase saved = testcaseRepository.save(testcase);
            bh.consume(saved);
        });
    }

    @Benchmark
    @Threads(4)
    public void benchmarkMixedReadWrite(Blackhole bh) {
        IntStream.range(0, 25).forEach(i -> {
            if (i % 3 == 0) {
                // Write operation
                Testcase testcase = createTestcase(i);
                Testcase saved = testcaseRepository.save(testcase);
                bh.consume(saved);
            } else {
                // Read operation
                var testcases = testcaseRepository.findByStatus("Processed");
                bh.consume(testcases);
            }
        });
    }

    @Benchmark
    public void benchmarkAsyncOperations(Blackhole bh) {
        CompletableFuture<?>[] futures = IntStream.range(0, 20)
            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                Testcase testcase = createTestcase(i);
                return testcaseRepository.save(testcase);
            }))
            .toArray(CompletableFuture[]::new);
        
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
        bh.consume(allOf.join());
    }

    @Benchmark
    @Threads(2)
    public void benchmarkBatchOperations(Blackhole bh) {
        var testcases = IntStream.range(0, 100)
            .mapToObj(this::createTestcase)
            .toList();
        
        var saved = testcaseRepository.saveAll(testcases);
        bh.consume(saved);
    }

    private Testcase createTestcase(int index) {
        return Testcase.builder()
            .filename("concurrent_test_" + index + "_" + Thread.currentThread().getId() + ".txt")
            .jobType("libfuzzer_asan_chrome")
            .fuzzerName("libfuzzer")
            .projectName("chrome")
            .status(index % 2 == 0 ? "Processed" : "Pending")
            .crashType("heap-buffer-overflow")
            .crashState("concurrent_crash_" + index)
            .securityFlag(index % 3 == 0)
            .reproducible(true)
            .timestamp(LocalDateTime.now())
            .build();
    }

    @SpringBootApplication
    public static class BenchmarkApplication {
        // Minimal Spring Boot application for benchmarking
    }
}