package com.google.clusterfuzz.integration.issuetracker;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request object for updating existing issues in issue trackers.
 */
@Data
@Builder
public class IssueUpdateRequest {
    private String title;
    private String body;
    private String state;
    private String assignee;
    private List<String> labels;
    private String priority;
    private String milestone;
}