package com.google.clusterfuzz.analysis.crash;

import com.google.clusterfuzz.analysis.stack.StackAnalysisResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Result of crash analysis containing all analysis data.
 */
@Data
@Builder
public class CrashAnalysisResult {
    private Long testcaseId;
    private String crashType;
    private String crashState;
    private String severity;
    private boolean securityFlag;
    private boolean reproducible;
    private StackAnalysisResult stackAnalysis;
    private SeverityAssessment severityDetails;
    private LocalDateTime analysisTimestamp;
    private String analysisVersion;
    private Double confidenceScore;
}