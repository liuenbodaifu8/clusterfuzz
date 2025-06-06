package com.google.clusterfuzz.analysis.crash;

import com.google.clusterfuzz.core.entity.Testcase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Analyzes crash severity based on crash type and characteristics.
 */
@Component
@Slf4j
public class SeverityAnalyzer {

    /**
     * Analyzes the severity of a crash.
     */
    public String analyzeSeverity(Testcase testcase) {
        if (testcase == null || testcase.getCrashType() == null) {
            return "UNKNOWN";
        }

        String crashType = testcase.getCrashType().toLowerCase();
        String stacktrace = testcase.getCrashStacktrace();

        // High severity crashes
        if (isHighSeverityCrash(crashType, stacktrace)) {
            return "HIGH";
        }

        // Medium severity crashes
        if (isMediumSeverityCrash(crashType, stacktrace)) {
            return "MEDIUM";
        }

        // Low severity crashes
        if (isLowSeverityCrash(crashType, stacktrace)) {
            return "LOW";
        }

        return "UNKNOWN";
    }

    private boolean isHighSeverityCrash(String crashType, String stacktrace) {
        // Buffer overflows and use-after-free are typically high severity
        if (crashType.contains("buffer-overflow") || 
            crashType.contains("use-after-free") ||
            crashType.contains("double-free")) {
            return true;
        }

        // SEGV with controlled addresses
        if (crashType.contains("segv") && stacktrace != null) {
            if (stacktrace.contains("0x41414141") || 
                stacktrace.contains("0x42424242") ||
                stacktrace.contains("0x43434343")) {
                return true;
            }
        }

        return false;
    }

    private boolean isMediumSeverityCrash(String crashType, String stacktrace) {
        // Regular SEGV crashes
        if (crashType.contains("segv")) {
            return true;
        }

        // Heap corruption
        if (crashType.contains("heap") && !crashType.contains("overflow")) {
            return true;
        }

        return false;
    }

    private boolean isLowSeverityCrash(String crashType, String stacktrace) {
        // Assertion failures
        if (crashType.contains("abrt") || crashType.contains("assert")) {
            return true;
        }

        // Timeouts
        if (crashType.contains("timeout")) {
            return true;
        }

        return false;
    }
}