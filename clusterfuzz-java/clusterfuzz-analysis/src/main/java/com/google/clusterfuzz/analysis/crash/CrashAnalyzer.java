package com.google.clusterfuzz.analysis.crash;

import com.google.clusterfuzz.core.entity.CrashResult;
import com.google.clusterfuzz.core.entity.Testcase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Main crash analysis engine for ClusterFuzz.
 * Analyzes crashes to determine type, severity, and security implications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrashAnalyzer {

    private final StackAnalyzer stackAnalyzer;
    private final SeverityAnalyzer severityAnalyzer;
    private final CrashComparer crashComparer;

    // Common crash type patterns
    private static final Pattern HEAP_BUFFER_OVERFLOW = Pattern.compile(
        "heap-buffer-overflow|AddressSanitizer.*heap-buffer-overflow", Pattern.CASE_INSENSITIVE);
    private static final Pattern STACK_BUFFER_OVERFLOW = Pattern.compile(
        "stack-buffer-overflow|AddressSanitizer.*stack-buffer-overflow", Pattern.CASE_INSENSITIVE);
    private static final Pattern USE_AFTER_FREE = Pattern.compile(
        "heap-use-after-free|AddressSanitizer.*heap-use-after-free", Pattern.CASE_INSENSITIVE);
    private static final Pattern NULL_DEREFERENCE = Pattern.compile(
        "SEGV.*null|segmentation fault.*0x0+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASSERT_FAILURE = Pattern.compile(
        "assertion.*failed|assert.*failed", Pattern.CASE_INSENSITIVE);

    /**
     * Analyzes a crash and returns comprehensive analysis results.
     *
     * @param testcase The testcase containing crash information
     * @param stacktrace Raw stack trace from the crash
     * @param binaryPath Path to the binary that crashed
     * @return CrashAnalysisResult containing all analysis data
     */
    public CrashAnalysisResult analyzeCrash(Testcase testcase, String stacktrace, String binaryPath) {
        log.info("Starting crash analysis for testcase {}", testcase.getId());

        try {
            // Parse and analyze stack trace
            StackAnalysisResult stackResult = stackAnalyzer.analyzeStackTrace(stacktrace, binaryPath);
            
            // Determine crash type
            String crashType = determineCrashType(stacktrace, stackResult);
            
            // Assess severity and security implications
            SeverityAssessment severity = severityAnalyzer.assessSeverity(crashType, stackResult, testcase);
            
            // Generate crash state (unique identifier)
            String crashState = generateCrashState(stackResult);
            
            // Check if crash is reproducible
            boolean reproducible = isReproducible(testcase, stackResult);

            CrashAnalysisResult result = CrashAnalysisResult.builder()
                .testcaseId(testcase.getId())
                .crashType(crashType)
                .crashState(crashState)
                .severity(severity.getSeverity())
                .securityFlag(severity.isSecurityIssue())
                .reproducible(reproducible)
                .stackAnalysis(stackResult)
                .severityDetails(severity)
                .analysisTimestamp(java.time.LocalDateTime.now())
                .build();

            log.info("Crash analysis completed for testcase {}: type={}, severity={}, security={}", 
                testcase.getId(), crashType, severity.getSeverity(), severity.isSecurityIssue());

            return result;

        } catch (Exception e) {
            log.error("Error analyzing crash for testcase {}", testcase.getId(), e);
            throw new CrashAnalysisException("Failed to analyze crash", e);
        }
    }

    /**
     * Compares two crashes to determine if they are duplicates.
     *
     * @param crash1 First crash analysis result
     * @param crash2 Second crash analysis result
     * @return Comparison result with similarity score
     */
    public CrashComparisonResult compareCrashes(CrashAnalysisResult crash1, CrashAnalysisResult crash2) {
        return crashComparer.compareCrashes(crash1, crash2);
    }

    /**
     * Finds similar crashes in the database.
     *
     * @param crashResult The crash to find similarities for
     * @param existingCrashes List of existing crashes to compare against
     * @return List of similar crashes with similarity scores
     */
    public List<SimilarCrash> findSimilarCrashes(CrashAnalysisResult crashResult, 
                                                List<CrashAnalysisResult> existingCrashes) {
        return crashComparer.findSimilarCrashes(crashResult, existingCrashes);
    }

    /**
     * Determines the crash type based on stack trace and analysis.
     */
    private String determineCrashType(String stacktrace, StackAnalysisResult stackResult) {
        if (stacktrace == null || stacktrace.isEmpty()) {
            return "Unknown";
        }

        // Check for specific crash patterns
        if (HEAP_BUFFER_OVERFLOW.matcher(stacktrace).find()) {
            return "Heap-buffer-overflow";
        }
        if (STACK_BUFFER_OVERFLOW.matcher(stacktrace).find()) {
            return "Stack-buffer-overflow";
        }
        if (USE_AFTER_FREE.matcher(stacktrace).find()) {
            return "Heap-use-after-free";
        }
        if (NULL_DEREFERENCE.matcher(stacktrace).find()) {
            return "Null-dereference";
        }
        if (ASSERT_FAILURE.matcher(stacktrace).find()) {
            return "Assert";
        }

        // Check for sanitizer-specific patterns
        if (stacktrace.contains("AddressSanitizer")) {
            return "AddressSanitizer";
        }
        if (stacktrace.contains("MemorySanitizer")) {
            return "MemorySanitizer";
        }
        if (stacktrace.contains("ThreadSanitizer")) {
            return "ThreadSanitizer";
        }
        if (stacktrace.contains("UndefinedBehaviorSanitizer")) {
            return "UndefinedBehaviorSanitizer";
        }

        // Check for common crash types
        if (stacktrace.toLowerCase().contains("segmentation fault") || 
            stacktrace.toLowerCase().contains("sigsegv")) {
            return "Segmentation fault";
        }
        if (stacktrace.toLowerCase().contains("sigabrt") || 
            stacktrace.toLowerCase().contains("abort")) {
            return "Abrt";
        }
        if (stacktrace.toLowerCase().contains("sigfpe")) {
            return "Floating point exception";
        }

        // Analyze based on stack frames
        if (stackResult != null && !stackResult.getFrames().isEmpty()) {
            String topFunction = stackResult.getFrames().get(0).getFunction();
            if (topFunction != null) {
                if (topFunction.contains("malloc") || topFunction.contains("free")) {
                    return "Heap corruption";
                }
                if (topFunction.contains("memcpy") || topFunction.contains("strcpy")) {
                    return "Buffer overflow";
                }
            }
        }

        return "Unknown";
    }

    /**
     * Generates a unique crash state identifier.
     */
    private String generateCrashState(StackAnalysisResult stackResult) {
        if (stackResult == null || stackResult.getFrames().isEmpty()) {
            return "Unknown";
        }

        StringBuilder crashState = new StringBuilder();
        
        // Use top 3 stack frames for crash state
        int frameCount = Math.min(3, stackResult.getFrames().size());
        for (int i = 0; i < frameCount; i++) {
            StackFrame frame = stackResult.getFrames().get(i);
            if (frame.getFunction() != null && !frame.getFunction().isEmpty()) {
                if (crashState.length() > 0) {
                    crashState.append("\n");
                }
                crashState.append(frame.getFunction());
            }
        }

        return crashState.toString();
    }

    /**
     * Determines if a crash is reproducible based on analysis.
     */
    private boolean isReproducible(Testcase testcase, StackAnalysisResult stackResult) {
        // Basic heuristics for reproducibility
        if (stackResult == null || stackResult.getFrames().isEmpty()) {
            return false;
        }

        // Crashes with clear stack traces are more likely to be reproducible
        if (stackResult.getFrames().size() >= 3) {
            return true;
        }

        // Sanitizer crashes are usually reproducible
        if (testcase.getStacktrace() != null && 
            testcase.getStacktrace().contains("Sanitizer")) {
            return true;
        }

        // Default to true for now - actual reproducibility testing would be done separately
        return true;
    }
}