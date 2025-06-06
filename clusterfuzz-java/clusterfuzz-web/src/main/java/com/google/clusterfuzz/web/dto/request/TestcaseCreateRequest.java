package com.google.clusterfuzz.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Request DTO for creating a new testcase.
 */
@Schema(description = "Request for creating a new testcase")
public class TestcaseCreateRequest {
    
    @JsonProperty("crash_type")
    @NotBlank(message = "Crash type is required")
    @Size(max = 255, message = "Crash type must not exceed 255 characters")
    @Schema(description = "Type of crash", example = "heap-buffer-overflow", required = true)
    private String crashType;
    
    @JsonProperty("crash_state")
    @NotBlank(message = "Crash state is required")
    @Size(max = 1000, message = "Crash state must not exceed 1000 characters")
    @Schema(description = "Crash state information", example = "chrome::function_name", required = true)
    private String crashState;
    
    @JsonProperty("security_flag")
    @NotNull(message = "Security flag is required")
    @Schema(description = "Whether this is a security issue", example = "true", required = true)
    private Boolean securityFlag;
    
    @JsonProperty("project_name")
    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    @Schema(description = "Name of the project", example = "chromium", required = true)
    private String projectName;
    
    @JsonProperty("fuzzer_name")
    @NotBlank(message = "Fuzzer name is required")
    @Size(max = 100, message = "Fuzzer name must not exceed 100 characters")
    @Schema(description = "Name of the fuzzer", example = "libfuzzer", required = true)
    private String fuzzerName;
    
    @JsonProperty("job_type")
    @NotBlank(message = "Job type is required")
    @Size(max = 100, message = "Job type must not exceed 100 characters")
    @Schema(description = "Type of fuzzing job", example = "libfuzzer_chrome_asan", required = true)
    private String jobType;
    
    @JsonProperty("platform")
    @NotBlank(message = "Platform is required")
    @Size(max = 50, message = "Platform must not exceed 50 characters")
    @Schema(description = "Platform where crash occurred", example = "linux", required = true)
    private String platform;
    
    @JsonProperty("one_time_crasher_flag")
    @Schema(description = "Whether this is a one-time crasher", example = "false")
    private Boolean oneTimeCrasherFlag = false;
    
    @JsonProperty("testcase_file_path")
    @Size(max = 500, message = "Testcase file path must not exceed 500 characters")
    @Schema(description = "Path to the testcase file", example = "/path/to/testcase.txt")
    private String testcaseFilePath;
    
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
    public TestcaseCreateRequest() {}
    
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
    
    public Boolean getOneTimeCrasherFlag() {
        return oneTimeCrasherFlag;
    }
    
    public void setOneTimeCrasherFlag(Boolean oneTimeCrasherFlag) {
        this.oneTimeCrasherFlag = oneTimeCrasherFlag;
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
}