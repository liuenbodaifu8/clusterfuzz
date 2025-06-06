package com.google.clusterfuzz.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for testcase statistics.
 */
@Schema(description = "Testcase statistics response")
public class TestcaseStatsResponse {
    
    @JsonProperty("total_testcases")
    @Schema(description = "Total number of testcases", example = "15432")
    private Long totalTestcases;
    
    @JsonProperty("open_testcases")
    @Schema(description = "Number of open testcases", example = "1234")
    private Long openTestcases;
    
    @JsonProperty("closed_testcases")
    @Schema(description = "Number of closed testcases", example = "14198")
    private Long closedTestcases;
    
    @JsonProperty("security_bugs")
    @Schema(description = "Number of security-related testcases", example = "567")
    private Long securityBugs;
    
    @JsonProperty("triaged_testcases")
    @Schema(description = "Number of triaged testcases", example = "12345")
    private Long triagedTestcases;
    
    @JsonProperty("fixed_testcases")
    @Schema(description = "Number of fixed testcases", example = "11890")
    private Long fixedTestcases;
    
    @JsonProperty("regression_testcases")
    @Schema(description = "Number of regression testcases", example = "234")
    private Long regressionTestcases;
    
    @JsonProperty("one_time_crashers")
    @Schema(description = "Number of one-time crasher testcases", example = "3456")
    private Long oneTimeCrashers;
    
    @JsonProperty("recent_testcases")
    @Schema(description = "Number of recent testcases (last 7 days)", example = "89")
    private Long recentTestcases;
    
    @JsonProperty("testcases_by_project")
    @Schema(description = "Breakdown of testcases by project")
    private Map<String, Long> testcasesByProject;
    
    @JsonProperty("testcases_by_fuzzer")
    @Schema(description = "Breakdown of testcases by fuzzer")
    private Map<String, Long> testcasesByFuzzer;
    
    @JsonProperty("testcases_by_platform")
    @Schema(description = "Breakdown of testcases by platform")
    private Map<String, Long> testcasesByPlatform;
    
    @JsonProperty("testcases_by_crash_type")
    @Schema(description = "Breakdown of testcases by crash type")
    private Map<String, Long> testcasesByCrashType;
    
    @JsonProperty("testcases_by_status")
    @Schema(description = "Breakdown of testcases by status")
    private Map<String, Long> testcasesByStatus;
    
    @JsonProperty("daily_stats")
    @Schema(description = "Daily statistics for the last 30 days")
    private Map<String, Long> dailyStats;
    
    @JsonProperty("weekly_stats")
    @Schema(description = "Weekly statistics for the last 12 weeks")
    private Map<String, Long> weeklyStats;
    
    @JsonProperty("monthly_stats")
    @Schema(description = "Monthly statistics for the last 12 months")
    private Map<String, Long> monthlyStats;
    
    @JsonProperty("average_time_to_triage")
    @Schema(description = "Average time to triage in hours", example = "24.5")
    private Double averageTimeToTriage;
    
    @JsonProperty("average_time_to_fix")
    @Schema(description = "Average time to fix in hours", example = "168.0")
    private Double averageTimeToFix;
    
    @JsonProperty("top_crash_types")
    @Schema(description = "Top 10 crash types by frequency")
    private Map<String, Long> topCrashTypes;
    
    @JsonProperty("top_projects")
    @Schema(description = "Top 10 projects by testcase count")
    private Map<String, Long> topProjects;
    
    @JsonProperty("top_fuzzers")
    @Schema(description = "Top 10 fuzzers by testcase count")
    private Map<String, Long> topFuzzers;
    
    @JsonProperty("generated_at")
    @Schema(description = "When these statistics were generated", example = "2024-01-15T10:30:00")
    private LocalDateTime generatedAt;
    
    @JsonProperty("date_range")
    @Schema(description = "Date range for the statistics", example = "last_30_days")
    private String dateRange;
    
    // Default constructor
    public TestcaseStatsResponse() {
        this.generatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getTotalTestcases() {
        return totalTestcases;
    }
    
    public void setTotalTestcases(Long totalTestcases) {
        this.totalTestcases = totalTestcases;
    }
    
    public Long getOpenTestcases() {
        return openTestcases;
    }
    
    public void setOpenTestcases(Long openTestcases) {
        this.openTestcases = openTestcases;
    }
    
    public Long getClosedTestcases() {
        return closedTestcases;
    }
    
    public void setClosedTestcases(Long closedTestcases) {
        this.closedTestcases = closedTestcases;
    }
    
    public Long getSecurityBugs() {
        return securityBugs;
    }
    
    public void setSecurityBugs(Long securityBugs) {
        this.securityBugs = securityBugs;
    }
    
    public Long getTriagedTestcases() {
        return triagedTestcases;
    }
    
    public void setTriagedTestcases(Long triagedTestcases) {
        this.triagedTestcases = triagedTestcases;
    }
    
    public Long getFixedTestcases() {
        return fixedTestcases;
    }
    
    public void setFixedTestcases(Long fixedTestcases) {
        this.fixedTestcases = fixedTestcases;
    }
    
    public Long getRegressionTestcases() {
        return regressionTestcases;
    }
    
    public void setRegressionTestcases(Long regressionTestcases) {
        this.regressionTestcases = regressionTestcases;
    }
    
    public Long getOneTimeCrashers() {
        return oneTimeCrashers;
    }
    
    public void setOneTimeCrashers(Long oneTimeCrashers) {
        this.oneTimeCrashers = oneTimeCrashers;
    }
    
    public Long getRecentTestcases() {
        return recentTestcases;
    }
    
    public void setRecentTestcases(Long recentTestcases) {
        this.recentTestcases = recentTestcases;
    }
    
    public Map<String, Long> getTestcasesByProject() {
        return testcasesByProject;
    }
    
    public void setTestcasesByProject(Map<String, Long> testcasesByProject) {
        this.testcasesByProject = testcasesByProject;
    }
    
    public Map<String, Long> getTestcasesByFuzzer() {
        return testcasesByFuzzer;
    }
    
    public void setTestcasesByFuzzer(Map<String, Long> testcasesByFuzzer) {
        this.testcasesByFuzzer = testcasesByFuzzer;
    }
    
    public Map<String, Long> getTestcasesByPlatform() {
        return testcasesByPlatform;
    }
    
    public void setTestcasesByPlatform(Map<String, Long> testcasesByPlatform) {
        this.testcasesByPlatform = testcasesByPlatform;
    }
    
    public Map<String, Long> getTestcasesByCrashType() {
        return testcasesByCrashType;
    }
    
    public void setTestcasesByCrashType(Map<String, Long> testcasesByCrashType) {
        this.testcasesByCrashType = testcasesByCrashType;
    }
    
    public Map<String, Long> getTestcasesByStatus() {
        return testcasesByStatus;
    }
    
    public void setTestcasesByStatus(Map<String, Long> testcasesByStatus) {
        this.testcasesByStatus = testcasesByStatus;
    }
    
    public Map<String, Long> getDailyStats() {
        return dailyStats;
    }
    
    public void setDailyStats(Map<String, Long> dailyStats) {
        this.dailyStats = dailyStats;
    }
    
    public Map<String, Long> getWeeklyStats() {
        return weeklyStats;
    }
    
    public void setWeeklyStats(Map<String, Long> weeklyStats) {
        this.weeklyStats = weeklyStats;
    }
    
    public Map<String, Long> getMonthlyStats() {
        return monthlyStats;
    }
    
    public void setMonthlyStats(Map<String, Long> monthlyStats) {
        this.monthlyStats = monthlyStats;
    }
    
    public Double getAverageTimeToTriage() {
        return averageTimeToTriage;
    }
    
    public void setAverageTimeToTriage(Double averageTimeToTriage) {
        this.averageTimeToTriage = averageTimeToTriage;
    }
    
    public Double getAverageTimeToFix() {
        return averageTimeToFix;
    }
    
    public void setAverageTimeToFix(Double averageTimeToFix) {
        this.averageTimeToFix = averageTimeToFix;
    }
    
    public Map<String, Long> getTopCrashTypes() {
        return topCrashTypes;
    }
    
    public void setTopCrashTypes(Map<String, Long> topCrashTypes) {
        this.topCrashTypes = topCrashTypes;
    }
    
    public Map<String, Long> getTopProjects() {
        return topProjects;
    }
    
    public void setTopProjects(Map<String, Long> topProjects) {
        this.topProjects = topProjects;
    }
    
    public Map<String, Long> getTopFuzzers() {
        return topFuzzers;
    }
    
    public void setTopFuzzers(Map<String, Long> topFuzzers) {
        this.topFuzzers = topFuzzers;
    }
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public String getDateRange() {
        return dateRange;
    }
    
    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }
}