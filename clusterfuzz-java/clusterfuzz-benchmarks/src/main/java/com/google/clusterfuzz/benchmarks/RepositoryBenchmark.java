package com.google.clusterfuzz.benchmarks;

import com.google.clusterfuzz.core.entity.Testcase;
import com.google.clusterfuzz.core.repository.TestcaseRepository;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmarks for repository operations.
 * Tests critical database access patterns under load.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class RepositoryBenchmark {

    private ConfigurableApplicationContext context;
    private TestcaseRepository testcaseRepository;
    
    @Setup(Level.Trial)
    public void setup() {
        // Initialize Spring context with H2 database
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:benchmark;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.show-sql", "false");
        
        context = SpringApplication.run(BenchmarkApplication.class);
        testcaseRepository = context.getBean(TestcaseRepository.class);
        
        // Seed test data
        seedTestData();
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }
    
    private void seedTestData() {
        // Create 1000 test cases for benchmarking
        for (int i = 0; i < 1000; i++) {
            Testcase testcase = Testcase.builder()
                .filename("test_" + i + ".txt")
                .jobType("libfuzzer_asan_chrome")
                .fuzzerName("libfuzzer")
                .projectName("chrome")
                .status(i % 2 == 0 ? "Processed" : "Pending")
                .crashType(i % 3 == 0 ? "heap-buffer-overflow" : "null-dereference")
                .crashState("crash_state_" + (i % 10))
                .securityFlag(i % 4 == 0)
                .reproducible(i % 3 != 0)
                .hasMinimizedTestcase(i % 5 == 0)
                .timestamp(LocalDateTime.now().minusDays(i % 30))
                .build();
            testcaseRepository.save(testcase);
        }
    }

    @Benchmark
    public void benchmarkFindAll(Blackhole bh) {
        List<Testcase> testcases = testcaseRepository.findAll();
        bh.consume(testcases);
    }

    @Benchmark
    public void benchmarkFindByStatus(Blackhole bh) {
        List<Testcase> testcases = testcaseRepository.findByStatus("Processed");
        bh.consume(testcases);
    }

    @Benchmark
    public void benchmarkFindByJobType(Blackhole bh) {
        List<Testcase> testcases = testcaseRepository.findByJobType("libfuzzer_asan_chrome");
        bh.consume(testcases);
    }

    @Benchmark
    public void benchmarkFindBySecurityFlag(Blackhole bh) {
        List<Testcase> testcases = testcaseRepository.findBySecurityFlag(true);
        bh.consume(testcases);
    }

    @Benchmark
    public void benchmarkPaginatedQuery(Blackhole bh) {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Testcase> page = testcaseRepository.findAll(pageable);
        bh.consume(page);
    }

    @Benchmark
    public void benchmarkComplexQuery(Blackhole bh) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Testcase> testcases = testcaseRepository.findRecentSecurityIssues(since, true);
        bh.consume(testcases);
    }

    @Benchmark
    public void benchmarkCountQuery(Blackhole bh) {
        long count = testcaseRepository.countByStatusAndSecurityFlag("Processed", true);
        bh.consume(count);
    }

    @Benchmark
    public void benchmarkSaveOperation(Blackhole bh) {
        Testcase testcase = Testcase.builder()
            .filename("benchmark_test.txt")
            .jobType("benchmark_job")
            .fuzzerName("benchmark_fuzzer")
            .projectName("benchmark_project")
            .status("New")
            .timestamp(LocalDateTime.now())
            .build();
        
        Testcase saved = testcaseRepository.save(testcase);
        bh.consume(saved);
        
        // Clean up to avoid memory issues
        testcaseRepository.delete(saved);
    }

    @SpringBootApplication
    public static class BenchmarkApplication {
        // Minimal Spring Boot application for benchmarking
    }
}