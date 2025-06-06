package com.google.clusterfuzz.integration.issuetracker;

import com.google.clusterfuzz.core.entity.Issue;
import com.google.clusterfuzz.core.entity.Testcase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * GitHub integration for issue tracking and management.
 * Handles creating, updating, and managing GitHub issues for ClusterFuzz findings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubIntegration implements IssueTracker {

    @Value("${clusterfuzz.integration.github.token:}")
    private String githubToken;

    @Value("${clusterfuzz.integration.github.organization:}")
    private String organization;

    @Value("${clusterfuzz.integration.github.repository:}")
    private String repository;

    @Value("${clusterfuzz.integration.github.enabled:false}")
    private boolean enabled;

    private GitHub github;
    private GHRepository repo;

    @PostConstruct
    public void initialize() {
        if (!enabled || githubToken.isEmpty()) {
            log.info("GitHub integration disabled or not configured");
            return;
        }

        try {
            github = new GitHubBuilder().withOAuthToken(githubToken).build();
            repo = github.getRepository(organization + "/" + repository);
            log.info("GitHub integration initialized for repository: {}/{}", organization, repository);
        } catch (IOException e) {
            log.error("Failed to initialize GitHub integration", e);
            throw new RuntimeException("GitHub integration initialization failed", e);
        }
    }

    @Override
    public String createIssue(IssueCreationRequest request) {
        if (!isEnabled()) {
            log.warn("GitHub integration not enabled, skipping issue creation");
            return null;
        }

        try {
            GHIssueBuilder issueBuilder = repo.createIssue(request.getTitle())
                .body(formatIssueBody(request))
                .assignee(request.getAssignee());

            // Add labels
            if (request.getLabels() != null && !request.getLabels().isEmpty()) {
                for (String label : request.getLabels()) {
                    issueBuilder.label(label);
                }
            }

            // Add security label for security issues
            if (request.isSecurityIssue()) {
                issueBuilder.label("security");
            }

            // Add severity label
            if (request.getSeverity() != null) {
                issueBuilder.label("severity:" + request.getSeverity().toLowerCase());
            }

            GHIssue issue = issueBuilder.create();
            
            log.info("Created GitHub issue #{} for testcase {}", issue.getNumber(), request.getTestcaseId());
            return String.valueOf(issue.getNumber());

        } catch (IOException e) {
            log.error("Failed to create GitHub issue for testcase {}", request.getTestcaseId(), e);
            throw new IssueTrackerException("Failed to create GitHub issue", e);
        }
    }

    @Override
    public void updateIssue(String issueId, IssueUpdateRequest request) {
        if (!isEnabled()) {
            return;
        }

        try {
            int issueNumber = Integer.parseInt(issueId);
            GHIssue issue = repo.getIssue(issueNumber);

            if (request.getTitle() != null) {
                issue.setTitle(request.getTitle());
            }

            if (request.getBody() != null) {
                issue.setBody(request.getBody());
            }

            if (request.getState() != null) {
                if ("closed".equalsIgnoreCase(request.getState())) {
                    issue.close();
                } else if ("open".equalsIgnoreCase(request.getState())) {
                    issue.reopen();
                }
            }

            if (request.getLabels() != null) {
                issue.setLabels(request.getLabels().toArray(new String[0]));
            }

            log.info("Updated GitHub issue #{}", issueNumber);

        } catch (IOException | NumberFormatException e) {
            log.error("Failed to update GitHub issue {}", issueId, e);
            throw new IssueTrackerException("Failed to update GitHub issue", e);
        }
    }

    @Override
    public void addComment(String issueId, String comment) {
        if (!isEnabled()) {
            return;
        }

        try {
            int issueNumber = Integer.parseInt(issueId);
            GHIssue issue = repo.getIssue(issueNumber);
            issue.comment(comment);

            log.info("Added comment to GitHub issue #{}", issueNumber);

        } catch (IOException | NumberFormatException e) {
            log.error("Failed to add comment to GitHub issue {}", issueId, e);
            throw new IssueTrackerException("Failed to add comment to GitHub issue", e);
        }
    }

    @Override
    public Optional<IssueInfo> getIssue(String issueId) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            int issueNumber = Integer.parseInt(issueId);
            GHIssue issue = repo.getIssue(issueNumber);

            IssueInfo info = IssueInfo.builder()
                .id(String.valueOf(issue.getNumber()))
                .title(issue.getTitle())
                .body(issue.getBody())
                .state(issue.getState().name())
                .url(issue.getHtmlUrl().toString())
                .createdAt(issue.getCreatedAt().toInstant())
                .updatedAt(issue.getUpdatedAt().toInstant())
                .assignee(issue.getAssignee() != null ? issue.getAssignee().getLogin() : null)
                .labels(issue.getLabels().stream().map(GHLabel::getName).toList())
                .build();

            return Optional.of(info);

        } catch (IOException | NumberFormatException e) {
            log.error("Failed to get GitHub issue {}", issueId, e);
            return Optional.empty();
        }
    }

    @Override
    public void closeIssue(String issueId, String reason) {
        if (!isEnabled()) {
            return;
        }

        try {
            int issueNumber = Integer.parseInt(issueId);
            GHIssue issue = repo.getIssue(issueNumber);
            
            if (reason != null && !reason.isEmpty()) {
                issue.comment("Closing issue: " + reason);
            }
            
            issue.close();
            log.info("Closed GitHub issue #{}", issueNumber);

        } catch (IOException | NumberFormatException e) {
            log.error("Failed to close GitHub issue {}", issueId, e);
            throw new IssueTrackerException("Failed to close GitHub issue", e);
        }
    }

    @Override
    public List<IssueInfo> findSimilarIssues(String title, String body) {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            // Search for similar issues using GitHub's search API
            String query = String.format("repo:%s/%s is:issue %s", organization, repository, 
                extractKeywords(title + " " + body));
            
            PagedSearchIterable<GHIssue> searchResults = github.searchIssues()
                .q(query)
                .list();

            return searchResults.toList().stream()
                .limit(10) // Limit to top 10 results
                .map(this::convertToIssueInfo)
                .toList();

        } catch (IOException e) {
            log.error("Failed to search for similar GitHub issues", e);
            return List.of();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && github != null && repo != null;
    }

    @Override
    public String getTrackerType() {
        return "GitHub";
    }

    /**
     * Formats the issue body with ClusterFuzz-specific information.
     */
    private String formatIssueBody(IssueCreationRequest request) {
        StringBuilder body = new StringBuilder();
        
        body.append("## ClusterFuzz Report\n\n");
        body.append("**Testcase ID:** ").append(request.getTestcaseId()).append("\n");
        body.append("**Crash Type:** ").append(request.getCrashType()).append("\n");
        body.append("**Severity:** ").append(request.getSeverity()).append("\n");
        body.append("**Security Issue:** ").append(request.isSecurityIssue() ? "Yes" : "No").append("\n\n");
        
        if (request.getStacktrace() != null && !request.getStacktrace().isEmpty()) {
            body.append("## Stack Trace\n\n");
            body.append("```\n");
            body.append(request.getStacktrace());
            body.append("\n```\n\n");
        }
        
        if (request.getReproductionSteps() != null && !request.getReproductionSteps().isEmpty()) {
            body.append("## Reproduction Steps\n\n");
            body.append(request.getReproductionSteps()).append("\n\n");
        }
        
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            body.append("## Description\n\n");
            body.append(request.getDescription()).append("\n\n");
        }
        
        body.append("---\n");
        body.append("*This issue was automatically created by ClusterFuzz*");
        
        return body.toString();
    }

    /**
     * Extracts keywords from text for search queries.
     */
    private String extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Simple keyword extraction - take first few words
        String[] words = text.split("\\s+");
        StringBuilder keywords = new StringBuilder();
        
        int count = 0;
        for (String word : words) {
            if (count >= 5) break; // Limit to 5 keywords
            if (word.length() > 3) { // Skip short words
                keywords.append(word).append(" ");
                count++;
            }
        }
        
        return keywords.toString().trim();
    }

    /**
     * Converts GHIssue to IssueInfo.
     */
    private IssueInfo convertToIssueInfo(GHIssue issue) {
        try {
            return IssueInfo.builder()
                .id(String.valueOf(issue.getNumber()))
                .title(issue.getTitle())
                .body(issue.getBody())
                .state(issue.getState().name())
                .url(issue.getHtmlUrl().toString())
                .createdAt(issue.getCreatedAt().toInstant())
                .updatedAt(issue.getUpdatedAt().toInstant())
                .assignee(issue.getAssignee() != null ? issue.getAssignee().getLogin() : null)
                .labels(issue.getLabels().stream().map(GHLabel::getName).toList())
                .build();
        } catch (IOException e) {
            log.error("Failed to convert GHIssue to IssueInfo", e);
            return null;
        }
    }
}