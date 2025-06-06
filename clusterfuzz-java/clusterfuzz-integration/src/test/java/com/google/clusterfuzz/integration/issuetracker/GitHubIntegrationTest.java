package com.google.clusterfuzz.integration.issuetracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.kohsuke.github.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubIntegrationTest {

    @Mock
    private GitHub github;

    @Mock
    private GHRepository repository;

    @Mock
    private GHIssue issue;

    @Mock
    private GHIssueBuilder issueBuilder;

    private GitHubIntegration githubIntegration;

    @BeforeEach
    void setUp() throws IOException {
        githubIntegration = new GitHubIntegration();
        
        // Use reflection to set private fields for testing
        setPrivateField(githubIntegration, "github", github);
        setPrivateField(githubIntegration, "repo", repository);
        setPrivateField(githubIntegration, "enabled", true);
        setPrivateField(githubIntegration, "organization", "test-org");
        setPrivateField(githubIntegration, "repository", "test-repo");
    }

    @Test
    void createIssue_WithValidRequest_ShouldCreateIssue() throws IOException {
        // Given
        IssueCreationRequest request = IssueCreationRequest.builder()
            .title("Test Issue")
            .testcaseId(123L)
            .crashType("SEGV")
            .severity("HIGH")
            .securityIssue(true)
            .stacktrace("Stack trace content")
            .build();

        when(repository.createIssue(anyString())).thenReturn(issueBuilder);
        when(issueBuilder.body(anyString())).thenReturn(issueBuilder);
        when(issueBuilder.assignee(anyString())).thenReturn(issueBuilder);
        when(issueBuilder.label(anyString())).thenReturn(issueBuilder);
        when(issueBuilder.create()).thenReturn(issue);
        when(issue.getNumber()).thenReturn(42);

        // When
        String issueId = githubIntegration.createIssue(request);

        // Then
        assertThat(issueId).isEqualTo("42");
        verify(repository).createIssue("Test Issue");
        verify(issueBuilder).label("security");
        verify(issueBuilder).label("severity:high");
        verify(issueBuilder).create();
    }

    @Test
    void createIssue_WhenDisabled_ShouldReturnNull() {
        // Given
        setPrivateField(githubIntegration, "enabled", false);
        IssueCreationRequest request = IssueCreationRequest.builder()
            .title("Test Issue")
            .build();

        // When
        String issueId = githubIntegration.createIssue(request);

        // Then
        assertThat(issueId).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void updateIssue_WithValidRequest_ShouldUpdateIssue() throws IOException {
        // Given
        String issueId = "42";
        IssueUpdateRequest request = IssueUpdateRequest.builder()
            .title("Updated Title")
            .body("Updated body")
            .state("closed")
            .build();

        when(repository.getIssue(42)).thenReturn(issue);

        // When
        githubIntegration.updateIssue(issueId, request);

        // Then
        verify(repository).getIssue(42);
        verify(issue).setTitle("Updated Title");
        verify(issue).setBody("Updated body");
        verify(issue).close();
    }

    @Test
    void addComment_WithValidIssueId_ShouldAddComment() throws IOException {
        // Given
        String issueId = "42";
        String comment = "Test comment";

        when(repository.getIssue(42)).thenReturn(issue);

        // When
        githubIntegration.addComment(issueId, comment);

        // Then
        verify(repository).getIssue(42);
        verify(issue).comment(comment);
    }

    @Test
    void getIssue_WithValidIssueId_ShouldReturnIssueInfo() throws IOException {
        // Given
        String issueId = "42";
        
        when(repository.getIssue(42)).thenReturn(issue);
        when(issue.getNumber()).thenReturn(42);
        when(issue.getTitle()).thenReturn("Test Issue");
        when(issue.getBody()).thenReturn("Test body");
        when(issue.getState()).thenReturn(GHIssueState.OPEN);
        when(issue.getHtmlUrl()).thenReturn(new java.net.URL("https://github.com/test/test/issues/42"));
        when(issue.getCreatedAt()).thenReturn(new java.util.Date());
        when(issue.getUpdatedAt()).thenReturn(new java.util.Date());
        when(issue.getAssignee()).thenReturn(null);
        when(issue.getLabels()).thenReturn(List.of());

        // When
        Optional<IssueInfo> result = githubIntegration.getIssue(issueId);

        // Then
        assertThat(result).isPresent();
        IssueInfo issueInfo = result.get();
        assertThat(issueInfo.getId()).isEqualTo("42");
        assertThat(issueInfo.getTitle()).isEqualTo("Test Issue");
        assertThat(issueInfo.getBody()).isEqualTo("Test body");
        assertThat(issueInfo.getState()).isEqualTo("OPEN");
    }

    @Test
    void getIssue_WithInvalidIssueId_ShouldReturnEmpty() throws IOException {
        // Given
        String issueId = "invalid";

        // When
        Optional<IssueInfo> result = githubIntegration.getIssue(issueId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void closeIssue_WithValidIssueId_ShouldCloseIssue() throws IOException {
        // Given
        String issueId = "42";
        String reason = "Fixed";

        when(repository.getIssue(42)).thenReturn(issue);

        // When
        githubIntegration.closeIssue(issueId, reason);

        // Then
        verify(repository).getIssue(42);
        verify(issue).comment("Closing issue: Fixed");
        verify(issue).close();
    }

    @Test
    void findSimilarIssues_WithSearchTerms_ShouldReturnSimilarIssues() throws IOException {
        // Given
        String title = "Buffer overflow in parser";
        String body = "Heap buffer overflow detected";

        PagedSearchIterable<GHIssue> searchResults = mock(PagedSearchIterable.class);
        when(github.searchIssues()).thenReturn(mock(GHIssueSearchBuilder.class));
        when(github.searchIssues().q(anyString())).thenReturn(mock(GHIssueSearchBuilder.class));
        when(github.searchIssues().q(anyString()).list()).thenReturn(searchResults);
        when(searchResults.toList()).thenReturn(List.of(issue));

        when(issue.getNumber()).thenReturn(42);
        when(issue.getTitle()).thenReturn("Similar Issue");
        when(issue.getBody()).thenReturn("Similar body");
        when(issue.getState()).thenReturn(GHIssueState.OPEN);
        when(issue.getHtmlUrl()).thenReturn(new java.net.URL("https://github.com/test/test/issues/42"));
        when(issue.getCreatedAt()).thenReturn(new java.util.Date());
        when(issue.getUpdatedAt()).thenReturn(new java.util.Date());
        when(issue.getAssignee()).thenReturn(null);
        when(issue.getLabels()).thenReturn(List.of());

        // When
        List<IssueInfo> similarIssues = githubIntegration.findSimilarIssues(title, body);

        // Then
        assertThat(similarIssues).hasSize(1);
        assertThat(similarIssues.get(0).getTitle()).isEqualTo("Similar Issue");
    }

    @Test
    void isEnabled_WhenConfigured_ShouldReturnTrue() {
        // When
        boolean enabled = githubIntegration.isEnabled();

        // Then
        assertThat(enabled).isTrue();
    }

    @Test
    void getTrackerType_ShouldReturnGitHub() {
        // When
        String trackerType = githubIntegration.getTrackerType();

        // Then
        assertThat(trackerType).isEqualTo("GitHub");
    }

    @Test
    void createIssue_WithIOException_ShouldThrowIssueTrackerException() throws IOException {
        // Given
        IssueCreationRequest request = IssueCreationRequest.builder()
            .title("Test Issue")
            .testcaseId(123L)
            .build();

        when(repository.createIssue(anyString())).thenThrow(new IOException("Network error"));

        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
            IssueTrackerException.class,
            () -> githubIntegration.createIssue(request)
        );
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}