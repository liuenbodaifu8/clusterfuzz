package com.google.clusterfuzz.integration.issuetracker;

import java.util.List;
import java.util.Optional;

/**
 * Interface for issue tracker integrations.
 * Provides a common API for different issue tracking systems.
 */
public interface IssueTracker {

    /**
     * Creates a new issue in the tracker.
     */
    String createIssue(IssueCreationRequest request);

    /**
     * Updates an existing issue.
     */
    void updateIssue(String issueId, IssueUpdateRequest request);

    /**
     * Adds a comment to an issue.
     */
    void addComment(String issueId, String comment);

    /**
     * Retrieves issue information.
     */
    Optional<IssueInfo> getIssue(String issueId);

    /**
     * Closes an issue with optional reason.
     */
    void closeIssue(String issueId, String reason);

    /**
     * Finds similar issues based on title and description.
     */
    List<IssueInfo> findSimilarIssues(String title, String body);

    /**
     * Checks if the tracker is enabled and configured.
     */
    boolean isEnabled();

    /**
     * Returns the tracker type name.
     */
    String getTrackerType();
}