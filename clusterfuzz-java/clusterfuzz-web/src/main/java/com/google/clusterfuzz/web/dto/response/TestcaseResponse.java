package com.google.clusterfuzz.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for testcase data.
 */
@Schema(description = "Testcase response data")
public class TestcaseResponse {
    
    @JsonProperty("id")
    @Schema(description = "Unique testcase identifier", example = "12345")
    private Long id;
    
    @JsonProperty("crash_type")
    @Schema(description = "Type of crash", example = "heap-buffer-overflow")
    private String crashType;
    
    @JsonProperty("crash_state")
    @Schema(description = "Crash state information", example = "chrome::function_name")
    private String crashState;
    
    @JsonProperty("security_flag")
    @Schema(description = "Whether this is a security issue", example = "true")
    private Boolean securityFlag;
    
    @JsonProperty("project_name")
    @Schema(description = "Name of the project", example = "chromium")
    private String projectName;
    
    @JsonProperty("fuzzer_name")
    @Schema(description = "Name of the fuzzer", example = "libfuzzer")
    private String fuzzerName;
    
    @JsonProperty("job_type")
    @Schema(description = "Type of fuzzing job", example = "libfuzzer_chrome_asan")
    private String jobType;
    
    @JsonProperty("platform")
    @Schema(description = "Platform where crash occurred", example = "linux")
    private String platform;
    
    @JsonProperty("timestamp")
    @Schema(description = "When the testcase was created", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;
    
    @JsonProperty("status")
    @Schema(description = "Current status of the testcase", example = "PROCESSED")
    private String status;
    
    @JsonProperty("triaged")
    @Schema(description = "Whether the testcase has been triaged", example = "true")
    private Boolean triaged;
    
    @JsonProperty("open")
    @Schema(description = "Whether the testcase is open", example = "true")
    private Boolean open;
    
    @JsonProperty("fixed")
    @Schema(description = "Whether the testcase has been fixed", example = "false")
    private Boolean fixed;
    
    @JsonProperty("regression")
    @Schema(description = "Whether this is a regression", example = "false")
    private Boolean regression;
    
    @JsonProperty("one_time_crasher_flag")
    @Schema(description = "Whether this is a one-time crasher", example = "false")
    private Boolean oneTimeCrasherFlag;
    
    @JsonProperty("bug_information")
    @Schema(description = "Bug tracking information", example = "crbug.com/123456")
    private String bugInformation;
    
    @JsonProperty("group_bug_information")
    @Schema(description = "Group bug tracking information", example = "crbug.com/789012")
    private String groupBugInformation;
    
    @JsonProperty("group_id")
    @Schema(description = "Group ID for related testcases", example = "12345")
    private Long groupId;
    
    @JsonProperty("impact_extended_stable_version")
    @Schema(description = "Impact on extended stable version", example = "true")
    private Boolean impactExtendedStableVersion;
    
    @JsonProperty("impact_stable_version")
    @Schema(description = "Impact on stable version", example = "true")
    private Boolean impactStableVersion;
    
    @JsonProperty("impact_beta_version")
    @Schema(description = "Impact on beta version", example = "true")
    private Boolean impactBetaVersion;
    
    @JsonProperty("impact_head_version")
    @Schema(description = "Impact on head version", example = "true")
    private Boolean impactHeadVersion;
    
    @JsonProperty("is_impact_set_flag")
    @Schema(description = "Whether impact has been set", example = "true")
    private Boolean isImpactSetFlag;
    
    @JsonProperty("testcase_file_path")
    @Schema(description = "Path to the testcase file", example = "/path/to/testcase.txt")
    private String testcaseFilePath;
    
    @JsonProperty("minimized_arguments")
    @Schema(description = "Minimized command line arguments", example = "--enable-features=test")
    private String minimizedArguments;
    
    @JsonProperty("window_argument")
    @Schema(description = "Window-specific arguments", example = "--window-size=1024,768")
    private String windowArgument;
    
    @JsonProperty("timeout_multiplier")
    @Schema(description = "Timeout multiplier for this testcase", example = "1.5")
    private Double timeoutMultiplier;
    
    @JsonProperty("additional_metadata")
    @Schema(description = "Additional metadata in JSON format")
    private String additionalMetadata;
    
    @JsonProperty("created_at")
    @Schema(description = "When the testcase was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    @Schema(description = "When the testcase was last updated", example = "2024-01-15T15:45:00")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public TestcaseResponse() {}
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCrashType() {
        return crashType;
    }
    
    public void setCrashType(String crashType) {
        this.crashType = crashType;
    }
    
    public String getCrashState() {
        return crashState;
    }
    
    public void setCrashState(String crashState) {
        this.crashState = crashState;
    }
    
    public Boolean getSecurityFlag() {
        return securityFlag;
    }
    
    public void setSecurityFlag(Boolean securityFlag) {
        this.securityFlag = securityFlag;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public String getFuzzerName() {
        return fuzzerName;
    }
    
    public void setFuzzerName(String fuzzerName) {
        this.fuzzerName = fuzzerName;
    }
    
    public String getJobType() {
        return jobType;
    }
    
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Boolean getTriaged() {
        return triaged;
    }
    
    public void setTriaged(Boolean triaged) {
        this.triaged = triaged;
    }
    
    public Boolean getOpen() {
        return open;
    }
    
    public void setOpen(Boolean open) {
        this.open = open;
    }
    
    public Boolean getFixed() {
        return fixed;
    }
    
    public void setFixed(Boolean fixed) {
        this.fixed = fixed;
    }
    
    public Boolean getRegression() {
        return regression;
    }
    
    public void setRegression(Boolean regression) {
        this.regression = regression;
    }
    
    public Boolean getOneTimeCrasherFlag() {
        return oneTimeCrasherFlag;
    }
    
    public void setOneTimeCrasherFlag(Boolean oneTimeCrasherFlag) {
        this.oneTimeCrasherFlag = oneTimeCrasherFlag;
    }
    
    public String getBugInformation() {
        return bugInformation;
    }
    
    public void setBugInformation(String bugInformation) {
        this.bugInformation = bugInformation;
    }
    
    public String getGroupBugInformation() {
        return groupBugInformation;
    }
    
    public void setGroupBugInformation(String groupBugInformation) {
        this.groupBugInformation = groupBugInformation;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public Boolean getImpactExtendedStableVersion() {
        return impactExtendedStableVersion;
    }
    
    public void setImpactExtendedStableVersion(Boolean impactExtendedStableVersion) {
        this.impactExtendedStableVersion = impactExtendedStableVersion;
    }
    
    public Boolean getImpactStableVersion() {
        return impactStableVersion;
    }
    
    public void setImpactStableVersion(Boolean impactStableVersion) {
        this.impactStableVersion = impactStableVersion;
    }
    
    public Boolean getImpactBetaVersion() {
        return impactBetaVersion;
    }
    
    public void setImpactBetaVersion(Boolean impactBetaVersion) {
        this.impactBetaVersion = impactBetaVersion;
    }
    
    public Boolean getImpactHeadVersion() {
        return impactHeadVersion;
    }
    
    public void setImpactHeadVersion(Boolean impactHeadVersion) {
        this.impactHeadVersion = impactHeadVersion;
    }
    
    public Boolean getIsImpactSetFlag() {
        return isImpactSetFlag;
    }
    
    public void setIsImpactSetFlag(Boolean isImpactSetFlag) {
        this.isImpactSetFlag = isImpactSetFlag;
    }
    
    public String getTestcaseFilePath() {
        return testcaseFilePath;
    }
    
    public void setTestcaseFilePath(String testcaseFilePath) {
        this.testcaseFilePath = testcaseFilePath;
    }
    
    public String getMinimizedArguments() {
        return minimizedArguments;
    }
    
    public void setMinimizedArguments(String minimizedArguments) {
        this.minimizedArguments = minimizedArguments;
    }
    
    public String getWindowArgument() {
        return windowArgument;
    }
    
    public void setWindowArgument(String windowArgument) {
        this.windowArgument = windowArgument;
    }
    
    public Double getTimeoutMultiplier() {
        return timeoutMultiplier;
    }
    
    public void setTimeoutMultiplier(Double timeoutMultiplier) {
        this.timeoutMultiplier = timeoutMultiplier;
    }
    
    public String getAdditionalMetadata() {
        return additionalMetadata;
    }
    
    public void setAdditionalMetadata(String additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}