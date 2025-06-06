package com.google.clusterfuzz.analysis.crash;

import com.google.clusterfuzz.core.entity.Testcase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrashAnalyzerTest {

    @Mock
    private SeverityAnalyzer severityAnalyzer;

    @Mock
    private CrashComparer crashComparer;

    @InjectMocks
    private CrashAnalyzer crashAnalyzer;

    private Testcase testcase;

    @BeforeEach
    void setUp() {
        testcase = new Testcase();
        testcase.setId(1L);
        testcase.setCrashType("SEGV");
        testcase.setCrashAddress("0x41414141");
        testcase.setCrashStacktrace("Stack trace content");
    }

    @Test
    void analyzeCrash_WithValidTestcase_ShouldReturnAnalysisResult() {
        // Given
        when(severityAnalyzer.analyzeSeverity(any())).thenReturn("HIGH");

        // When
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(testcase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTestcaseId()).isEqualTo(1L);
        assertThat(result.getCrashType()).isEqualTo("SEGV");
        assertThat(result.getCrashAddress()).isEqualTo("0x41414141");
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.isSecurityIssue()).isTrue(); // SEGV is typically security-related
    }

    @Test
    void analyzeCrash_WithNullPointerCrash_ShouldDetectNullPointer() {
        // Given
        testcase.setCrashType("SEGV");
        testcase.setCrashAddress("0x00000000");
        when(severityAnalyzer.analyzeSeverity(any())).thenReturn("MEDIUM");

        // When
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(testcase);

        // Then
        assertThat(result.getCrashType()).isEqualTo("SEGV");
        assertThat(result.getCrashAddress()).isEqualTo("0x00000000");
        assertThat(result.isNullPointerDereference()).isTrue();
    }

    @Test
    void analyzeCrash_WithHeapBufferOverflow_ShouldDetectBufferOverflow() {
        // Given
        testcase.setCrashType("heap-buffer-overflow");
        testcase.setCrashStacktrace("AddressSanitizer: heap-buffer-overflow");
        when(severityAnalyzer.analyzeSeverity(any())).thenReturn("HIGH");

        // When
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(testcase);

        // Then
        assertThat(result.getCrashType()).isEqualTo("heap-buffer-overflow");
        assertThat(result.isBufferOverflow()).isTrue();
        assertThat(result.isSecurityIssue()).isTrue();
    }

    @Test
    void analyzeCrash_WithUseAfterFree_ShouldDetectUseAfterFree() {
        // Given
        testcase.setCrashType("heap-use-after-free");
        testcase.setCrashStacktrace("AddressSanitizer: heap-use-after-free");
        when(severityAnalyzer.analyzeSeverity(any())).thenReturn("HIGH");

        // When
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(testcase);

        // Then
        assertThat(result.getCrashType()).isEqualTo("heap-use-after-free");
        assertThat(result.isUseAfterFree()).isTrue();
        assertThat(result.isSecurityIssue()).isTrue();
    }

    @Test
    void analyzeCrash_WithAssertionFailure_ShouldNotBeSecurityIssue() {
        // Given
        testcase.setCrashType("ABRT");
        testcase.setCrashStacktrace("Assertion failed");
        when(severityAnalyzer.analyzeSeverity(any())).thenReturn("LOW");

        // When
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(testcase);

        // Then
        assertThat(result.getCrashType()).isEqualTo("ABRT");
        assertThat(result.isSecurityIssue()).isFalse();
        assertThat(result.getSeverity()).isEqualTo("LOW");
    }

    @Test
    void analyzeCrash_WithEmptyStacktrace_ShouldHandleGracefully() {
        // Given
        testcase.setCrashStacktrace("");
        when(severityAnalyzer.analyzeSeverity(any())).thenReturn("UNKNOWN");

        // When
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(testcase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStacktrace()).isEmpty();
        assertThat(result.getSeverity()).isEqualTo("UNKNOWN");
    }

    @Test
    void analyzeCrash_WithNullTestcase_ShouldThrowException() {
        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> crashAnalyzer.analyzeCrash(null)
        );
    }
}