package com.google.clusterfuzz.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Size;

/**
 * Request DTO for updating an existing testcase.
 * All fields are optional for partial updates.
 */
@Schema(description = "Request for updating an existing testcase")
public class TestcaseUpdateRequest {
    
    @JsonProperty("crash_type")
    @Size(max = 255, message = "Crash type must not exceed 255 characters")
    @Schema(description = "Type of crash", example = "heap-buffer-overflow")
    private String crashType;
    
    @JsonProperty("crash_state")
    @Size(max = 1000, message = "Crash state must not exceed 1000 characters")
    @Schema(description = "Crash state information", example = "chrome::function_name")
    private String crashState;
    
    @JsonProperty("security_flag")
    @Schema(description = "Whether this is a security issue", example = "true")
    private Boolean securityFlag;
    
    @JsonProperty("status")
    @Size(max = 50, message = "Status must not exceed 50 characters")
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
    @Size(max = 500, message = "Bug information must not exceed 500 characters")
    @Schema(description = "Bug tracking information", example = "crbug.com/123456")
    private String bugInformation;
    
    @JsonProperty("group_bug_information")
    @Size(max = 500, message = "Group bug information must not exceed 500 characters")
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
    
    @JsonProperty("minimized_arguments")
    @Size(max = 1000, message = "Minimized arguments must not exceed 1000 characters")
    @Schema(description = "Minimized command line arguments", example = "--enable-features=test")
    private String minimizedArguments;
    
    @JsonProperty("window_argument")
    @Size(max = 500, message = "Window argument must not exceed 500 characters")
    @Schema(description = "Window-specific arguments", example = "--window-size=1024,768")
    private String windowArgument;
    
    @JsonProperty("timeout_multiplier")
    @Schema(description = "Timeout multiplier for this testcase", example = "1.5")
    private Double timeoutMultiplier;
    
    @JsonProperty("additional_metadata")
    @Size(max = 2000, message = "Additional metadata must not exceed 2000 characters")
    @Schema(description = "Additional metadata in JSON format")
    private String additionalMetadata;
    
    // Default constructor
    public TestcaseUpdateRequest() {}
    
    // Getters and setters
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
}