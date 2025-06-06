package com.google.clusterfuzz.integration.issuetracker;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Information about an issue from an issue tracker.
 */
@Data
@Builder
public class IssueInfo {
    private String id;
    private String title;
    private String body;
    private String state;
    private String url;
    private Instant createdAt;
    private Instant updatedAt;
    private String assignee;
    private List<String> labels;
    private String priority;
    private String milestone;
}