package com.google.clusterfuzz.integration.issuetracker;

/**
 * Exception thrown when issue tracker operations fail.
 */
public class IssueTrackerException extends RuntimeException {
    
    public IssueTrackerException(String message) {
        super(message);
    }
    
    public IssueTrackerException(String message, Throwable cause) {
        super(message, cause);
    }
}