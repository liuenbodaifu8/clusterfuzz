package com.google.clusterfuzz.analysis.crash;

import com.google.clusterfuzz.core.entity.Testcase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Compares crashes to identify duplicates and similar issues.
 */
@Component
@Slf4j
public class CrashComparer {

    /**
     * Calculates similarity score between two crashes (0.0 to 1.0).
     */
    public double calculateSimilarity(Testcase crash1, Testcase crash2) {
        if (crash1 == null || crash2 == null) {
            return 0.0;
        }

        double typeScore = compareCrashTypes(crash1.getCrashType(), crash2.getCrashType());
        double addressScore = compareCrashAddresses(crash1.getCrashAddress(), crash2.getCrashAddress());
        double stackScore = compareStacktraces(crash1.getCrashStacktrace(), crash2.getCrashStacktrace());

        // Weighted average
        return (typeScore * 0.3) + (addressScore * 0.2) + (stackScore * 0.5);
    }

    /**
     * Determines if two crashes are likely duplicates.
     */
    public boolean areDuplicates(Testcase crash1, Testcase crash2) {
        return calculateSimilarity(crash1, crash2) > 0.8;
    }

    private double compareCrashTypes(String type1, String type2) {
        if (type1 == null || type2 == null) {
            return 0.0;
        }
        return type1.equals(type2) ? 1.0 : 0.0;
    }

    private double compareCrashAddresses(String addr1, String addr2) {
        if (addr1 == null || addr2 == null) {
            return 0.0;
        }
        return addr1.equals(addr2) ? 1.0 : 0.0;
    }

    private double compareStacktraces(String stack1, String stack2) {
        if (stack1 == null || stack2 == null) {
            return 0.0;
        }

        // Simple line-based comparison
        String[] lines1 = stack1.split("\n");
        String[] lines2 = stack2.split("\n");

        int matches = 0;
        int maxLines = Math.max(lines1.length, lines2.length);

        for (int i = 0; i < Math.min(lines1.length, lines2.length); i++) {
            if (lines1[i].trim().equals(lines2[i].trim())) {
                matches++;
            }
        }

        return maxLines > 0 ? (double) matches / maxLines : 0.0;
    }
}