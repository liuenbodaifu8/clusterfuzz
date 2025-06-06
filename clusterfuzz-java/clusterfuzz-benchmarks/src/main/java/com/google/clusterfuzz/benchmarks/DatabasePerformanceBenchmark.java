package com.google.clusterfuzz.benchmarks;

import com.google.clusterfuzz.core.entity.Testcase;
import com.google.clusterfuzz.core.repository.TestcaseRepository;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for database operations performance.
 * Tests various database access patterns and optimizations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx1G"})
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:benchmark;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class DatabasePerformanceBenchmark {

    private TestcaseRepository testcaseRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private List<Long> testcaseIds;
    private static final int BATCH_SIZE = 100;
    private static final int TOTAL_RECORDS = 1000;

    @Setup
    public void setup() {
        // Create test data
        testcaseIds = new ArrayList<>();
        createTestData();
    }

    @TearDown
    public void tearDown() {
        // Cleanup test data
        testcaseRepository.deleteAll();
    }

    @Benchmark
    public void benchmarkSingleEntityFind(Blackhole bh) {
        Long id = testcaseIds.get(0);
        Testcase testcase = testcaseRepository.findById(id).orElse(null);
        bh.consume(testcase);
    }

    @Benchmark
    public void benchmarkBatchEntityFind(Blackhole bh) {
        List<Long> batchIds = testcaseIds.subList(0, Math.min(BATCH_SIZE, testcaseIds.size()));
        List<Testcase> testcases = testcaseRepository.findAllById(batchIds);
        bh.consume(testcases);
    }

    @Benchmark
    public void benchmarkPagedQuery(Blackhole bh) {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Testcase> page = testcaseRepository.findAll(pageable);
        bh.consume(page);
    }

    @Benchmark
    public void benchmarkCustomQuery(Blackhole bh) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Testcase> testcases = testcaseRepository.findByCreatedAtAfter(since);
        bh.consume(testcases);
    }

    @Benchmark
    public void benchmarkComplexQuery(Blackhole bh) {
        List<Testcase> testcases = testcaseRepository.findByCrashTypeAndStatusOrderByCreatedAtDesc(
            "SEGV", "PROCESSED");
        bh.consume(testcases);
    }

    @Benchmark
    @Transactional
    public void benchmarkSingleInsert(Blackhole bh) {
        Testcase testcase = createTestcase("benchmark_insert");
        Testcase saved = testcaseRepository.save(testcase);
        bh.consume(saved);
    }

    @Benchmark
    @Transactional
    public void benchmarkBatchInsert(Blackhole bh) {
        List<Testcase> testcases = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            testcases.add(createTestcase("batch_insert_" + i));
        }
        List<Testcase> saved = testcaseRepository.saveAll(testcases);
        bh.consume(saved);
    }

    @Benchmark
    @Transactional
    public void benchmarkBulkUpdate(Blackhole bh) {
        int updated = testcaseRepository.updateStatusByCrashType("SEGV", "BULK_UPDATED");
        bh.consume(updated);
    }

    @Benchmark
    public void benchmarkCountQuery(Blackhole bh) {
        long count = testcaseRepository.countByCrashType("SEGV");
        bh.consume(count);
    }

    @Benchmark
    public void benchmarkExistsQuery(Blackhole bh) {
        boolean exists = testcaseRepository.existsByCrashTypeAndStatus("SEGV", "PROCESSED");
        bh.consume(exists);
    }

    @Benchmark
    @Transactional
    public void benchmarkEntityManagerQuery(Blackhole bh) {
        List<Testcase> testcases = entityManager
            .createQuery("SELECT t FROM Testcase t WHERE t.crashType = :crashType", Testcase.class)
            .setParameter("crashType", "SEGV")
            .setMaxResults(50)
            .getResultList();
        bh.consume(testcases);
    }

    @Benchmark
    @Transactional
    public void benchmarkNativeQuery(Blackhole bh) {
        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager
            .createNativeQuery("SELECT id, crash_type, status FROM testcase WHERE crash_type = ?")
            .setParameter(1, "SEGV")
            .setMaxResults(50)
            .getResultList();
        bh.consume(results);
    }

    @Benchmark
    public void benchmarkCachedQuery(Blackhole bh) {
        // Test query caching performance
        List<Testcase> testcases1 = testcaseRepository.findByCrashType("SEGV");
        List<Testcase> testcases2 = testcaseRepository.findByCrashType("SEGV");
        bh.consume(testcases1);
        bh.consume(testcases2);
    }

    private void createTestData() {
        List<Testcase> testcases = new ArrayList<>();
        
        for (int i = 0; i < TOTAL_RECORDS; i++) {
            Testcase testcase = createTestcase("benchmark_" + i);
            testcase.setCrashType(i % 3 == 0 ? "SEGV" : i % 3 == 1 ? "ABRT" : "FPE");
            testcase.setStatus(i % 2 == 0 ? "PROCESSED" : "PENDING");
            testcases.add(testcase);
            
            if (testcases.size() >= BATCH_SIZE) {
                List<Testcase> saved = testcaseRepository.saveAll(testcases);
                saved.forEach(t -> testcaseIds.add(t.getId()));
                testcases.clear();
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        if (!testcases.isEmpty()) {
            List<Testcase> saved = testcaseRepository.saveAll(testcases);
            saved.forEach(t -> testcaseIds.add(t.getId()));
        }
    }

    private Testcase createTestcase(String suffix) {
        Testcase testcase = new Testcase();
        testcase.setTestcaseFilePath("/tmp/testcase_" + suffix);
        testcase.setCrashType("SEGV");
        testcase.setCrashAddress("0x41414141");
        testcase.setCrashStacktrace("Stack trace for " + suffix);
        testcase.setStatus("PENDING");
        testcase.setCreatedAt(LocalDateTime.now().minusHours((long) (Math.random() * 24 * 7)));
        return testcase;
    }
}