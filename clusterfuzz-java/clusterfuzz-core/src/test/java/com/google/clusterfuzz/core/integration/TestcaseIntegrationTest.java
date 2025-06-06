package com.google.clusterfuzz.core.integration;

import com.google.clusterfuzz.core.entity.Testcase;
import com.google.clusterfuzz.core.repository.TestcaseRepository;
import com.google.clusterfuzz.core.service.TestcaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Testcase entity and related services.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class TestcaseIntegrationTest {

    @Autowired
    private TestcaseRepository testcaseRepository;

    @Autowired
    private TestcaseService testcaseService;

    @Test
    void testCreateAndRetrieveTestcase() {
        // Given
        Testcase testcase = createSampleTestcase();

        // When
        Testcase saved = testcaseRepository.save(testcase);

        // Then
        assertThat(saved.getId()).isNotNull();
        
        Optional<Testcase> retrieved = testcaseRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCrashType()).isEqualTo("SEGV");
    }

    @Test
    void testFindByCrashType() {
        // Given
        Testcase testcase1 = createSampleTestcase();
        testcase1.setCrashType("SEGV");
        
        Testcase testcase2 = createSampleTestcase();
        testcase2.setCrashType("ABRT");
        
        testcaseRepository.save(testcase1);
        testcaseRepository.save(testcase2);

        // When
        List<Testcase> segvCrashes = testcaseRepository.findByCrashType("SEGV");

        // Then
        assertThat(segvCrashes).hasSize(1);
        assertThat(segvCrashes.get(0).getCrashType()).isEqualTo("SEGV");
    }

    @Test
    void testFindByCreatedAtAfter() {
        // Given
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        
        Testcase oldTestcase = createSampleTestcase();
        oldTestcase.setCreatedAt(LocalDateTime.now().minusHours(2));
        
        Testcase newTestcase = createSampleTestcase();
        newTestcase.setCreatedAt(LocalDateTime.now());
        
        testcaseRepository.save(oldTestcase);
        testcaseRepository.save(newTestcase);

        // When
        List<Testcase> recentTestcases = testcaseRepository.findByCreatedAtAfter(cutoff);

        // Then
        assertThat(recentTestcases).hasSize(1);
        assertThat(recentTestcases.get(0).getCreatedAt()).isAfter(cutoff);
    }

    @Test
    void testServiceLayerOperations() {
        // Given
        Testcase testcase = createSampleTestcase();

        // When
        Testcase created = testcaseService.createTestcase(testcase);
        Optional<Testcase> found = testcaseService.findById(created.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(created.getId());
    }

    private Testcase createSampleTestcase() {
        Testcase testcase = new Testcase();
        testcase.setTestcaseFilePath("/tmp/testcase_integration");
        testcase.setCrashType("SEGV");
        testcase.setCrashAddress("0x41414141");
        testcase.setCrashStacktrace("Integration test stack trace");
        testcase.setStatus("PENDING");
        testcase.setCreatedAt(LocalDateTime.now());
        return testcase;
    }
}