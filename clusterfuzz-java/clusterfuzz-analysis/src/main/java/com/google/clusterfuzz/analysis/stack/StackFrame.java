package com.google.clusterfuzz.analysis.stack;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single stack frame in a stack trace.
 */
@Data
@Builder
public class StackFrame {
    private Integer frameNumber;
    private String address;
    private String function;
    private String file;
    private Integer line;
    private Integer column;
    private String module;
    private Long offset;
}