package com.google.clusterfuzz.analysis.stack;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of stack trace analysis.
 */
@Data
@Builder
public class StackAnalysisResult {
    private List<StackFrame> frames;
    private int frameCount;
    private boolean hasSymbols;
    private String rawStacktrace;
    private String binaryPath;
}