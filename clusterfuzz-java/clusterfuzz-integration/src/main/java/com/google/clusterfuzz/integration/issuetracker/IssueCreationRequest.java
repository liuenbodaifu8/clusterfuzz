package com.google.clusterfuzz.integration.issuetracker;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request object for creating new issues in issue trackers.
 */
@Data
@Builder
public class IssueCreationRequest {
    private String title;
    private String description;
    private Long testcaseId;
    private String crashType;
    private String severity;
    private boolean securityIssue;
    private String stacktrace;
    private String reproductionSteps;
    private String assignee;
    private List<String> labels;
    private String priority;
    private String component;
}