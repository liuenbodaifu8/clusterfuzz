package com.google.clusterfuzz.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single testcase in the ClusterFuzz system.
 * This is the Java equivalent of the Python Testcase model.
 */
@Entity
@Table(name = "testcases", indexes = {
    @Index(name = "idx_testcase_status", columnList = "status"),
    @Index(name = "idx_testcase_job_type", columnList = "jobType"),
    @Index(name = "idx_testcase_fuzzer_name", columnList = "fuzzerName"),
    @Index(name = "idx_testcase_project_name", columnList = "projectName"),
    @Index(name = "idx_testcase_platform", columnList = "platform"),
    @Index(name = "idx_testcase_security_flag", columnList = "securityFlag"),
    @Index(name = "idx_testcase_open", columnList = "open"),
    @Index(name = "idx_testcase_group_id", columnList = "groupId"),
    @Index(name = "idx_testcase_is_leader", columnList = "isLeader"),
    @Index(name = "idx_testcase_has_bug_flag", columnList = "hasBugFlag"),
    @Index(name = "idx_testcase_timestamp", columnList = "timestamp"),
    @Index(name = "idx_testcase_crash_type_status", columnList = "crashType,status"),
    @Index(name = "idx_testcase_created_at", columnList = "createdAt"),
    @Index(name = "idx_testcase_updated_at", columnList = "updatedAt")
})
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Cacheable
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
public class Testcase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Crash information
    @Column(name = "crash_type")
    private String crashType;

    @Lob
    @Column(name = "crash_address")
    private String crashAddress;

    @Column(name = "crash_state")
    private String crashState;

    @Lob
    @Column(name = "crash_stacktrace")
    private String crashStacktrace;

    @Lob
    @Column(name = "last_tested_crash_stacktrace")
    private String lastTestedCrashStacktrace;

    // Blobstore keys for various artifacts
    @Lob
    @Column(name = "fuzzed_keys")
    private String fuzzedKeys;

    @Lob
    @Column(name = "minimized_keys")
    private String minimizedKeys;

    @Lob
    @Column(name = "minidump_keys")
    private String minidumpKeys;

    // Bug tracking information
    @Column(name = "bug_information")
    private String bugInformation;

    @Column(name = "regression")
    private String regression = "";

    @Column(name = "fixed")
    private String fixed = "";

    // Security information
    @Column(name = "security_flag")
    private Boolean securityFlag = false;

    @Column(name = "security_severity")
    private Integer securitySeverity;

    // Crash characteristics
    @Column(name = "one_time_crasher_flag")
    private Boolean oneTimeCrasherFlag = false;

    @Lob
    @Column(name = "comments")
    private String comments = "";

    @Column(name = "crash_revision")
    private Long crashRevision;

    // File and execution information
    @Lob
    @Column(name = "absolute_path")
    private String absolutePath;

    @Lob
    @Column(name = "minimized_arguments")
    private String minimizedArguments = "";

    @Lob
    @Column(name = "window_argument")
    private String windowArgument = "";

    // Job and queue information
    @Column(name = "job_type")
    private String jobType;

    @Lob
    @Column(name = "queue")
    private String queue;

    // Archive information
    @Column(name = "archive_state")
    private Integer archiveState = 0;

    @Lob
    @Column(name = "archive_filename")
    private String archiveFilename;

    // Timing information
    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @CreatedDate
    @Column(name = "created", updatable = false)
    private LocalDateTime created;

    @LastModifiedDate
    @Column(name = "updated")
    private LocalDateTime updated;

    // Testcase characteristics
    @Column(name = "flaky_stack")
    private Boolean flakyStack = false;

    @Column(name = "http_flag")
    private Boolean httpFlag = false;

    @Column(name = "fuzzer_name")
    private String fuzzerName;

    @Column(name = "status")
    private String status = "Processed";

    @Column(name = "duplicate_of")
    private Long duplicateOf;

    @Column(name = "symbolized")
    private Boolean symbolized = false;

    // Group information
    @Column(name = "group_id")
    private Long groupId = 0L;

    @Column(name = "group_bug_information")
    private Long groupBugInformation = 0L;

    // Gestures and interaction
    @ElementCollection
    @CollectionTable(name = "testcase_gestures", joinColumns = @JoinColumn(name = "testcase_id"))
    @Column(name = "gesture")
    private List<String> gestures;

    // ASAN configuration
    @Column(name = "redzone")
    private Integer redzone = 128;

    @Column(name = "disable_ubsan")
    private Boolean disableUbsan = false;

    // Status flags
    @Column(name = "open")
    private Boolean open = true;

    @Column(name = "timeout_multiplier")
    private Double timeoutMultiplier = 1.0;

    // Additional metadata as JSON
    @Lob
    @Column(name = "additional_metadata")
    private String additionalMetadata;

    @Column(name = "triaged")
    private Boolean triaged = false;

    @Column(name = "project_name")
    private String projectName;

    // Search and indexing
    @ElementCollection
    @CollectionTable(name = "testcase_keywords", joinColumns = @JoinColumn(name = "testcase_id"))
    @Column(name = "keyword")
    private List<String> keywords;

    @Column(name = "has_bug_flag")
    private Boolean hasBugFlag;

    @ElementCollection
    @CollectionTable(name = "testcase_bug_indices", joinColumns = @JoinColumn(name = "testcase_id"))
    @Column(name = "bug_index")
    private List<String> bugIndices;

    @Column(name = "overridden_fuzzer_name")
    private String overriddenFuzzerName;

    // Platform information
    @Column(name = "platform")
    private String platform;

    @Column(name = "platform_id")
    private String platformId;

    @ElementCollection
    @CollectionTable(name = "testcase_impact_indices", joinColumns = @JoinColumn(name = "testcase_id"))
    @Column(name = "impact_index")
    private List<String> impactIndices;

    // Duplicate and leadership flags
    @Column(name = "is_a_duplicate_flag")
    private Boolean isADuplicateFlag;

    @Column(name = "is_leader")
    private Boolean isLeader = false;

    // Constructors
    public Testcase() {
        this.timestamp = LocalDateTime.now();
    }

    public Testcase(String crashType, String crashState, String fuzzerName, String jobType) {
        this();
        this.crashType = crashType;
        this.crashState = crashState;
        this.fuzzerName = fuzzerName;
        this.jobType = jobType;
    }

    // Getters and Setters
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

    public String getCrashAddress() {
        return crashAddress;
    }

    public void setCrashAddress(String crashAddress) {
        this.crashAddress = crashAddress;
    }

    public String getCrashState() {
        return crashState;
    }

    public void setCrashState(String crashState) {
        this.crashState = crashState;
    }

    public String getCrashStacktrace() {
        return crashStacktrace;
    }

    public void setCrashStacktrace(String crashStacktrace) {
        this.crashStacktrace = crashStacktrace;
    }

    public String getLastTestedCrashStacktrace() {
        return lastTestedCrashStacktrace;
    }

    public void setLastTestedCrashStacktrace(String lastTestedCrashStacktrace) {
        this.lastTestedCrashStacktrace = lastTestedCrashStacktrace;
    }

    public String getFuzzedKeys() {
        return fuzzedKeys;
    }

    public void setFuzzedKeys(String fuzzedKeys) {
        this.fuzzedKeys = fuzzedKeys;
    }

    public String getMinimizedKeys() {
        return minimizedKeys;
    }

    public void setMinimizedKeys(String minimizedKeys) {
        this.minimizedKeys = minimizedKeys;
    }

    public String getMinidumpKeys() {
        return minidumpKeys;
    }

    public void setMinidumpKeys(String minidumpKeys) {
        this.minidumpKeys = minidumpKeys;
    }

    public String getBugInformation() {
        return bugInformation;
    }

    public void setBugInformation(String bugInformation) {
        this.bugInformation = bugInformation;
    }

    public String getRegression() {
        return regression;
    }

    public void setRegression(String regression) {
        this.regression = regression;
    }

    public String getFixed() {
        return fixed;
    }

    public void setFixed(String fixed) {
        this.fixed = fixed;
    }

    public Boolean getSecurityFlag() {
        return securityFlag;
    }

    public void setSecurityFlag(Boolean securityFlag) {
        this.securityFlag = securityFlag;
    }

    public Integer getSecuritySeverity() {
        return securitySeverity;
    }

    public void setSecuritySeverity(Integer securitySeverity) {
        this.securitySeverity = securitySeverity;
    }

    public Boolean getOneTimeCrasherFlag() {
        return oneTimeCrasherFlag;
    }

    public void setOneTimeCrasherFlag(Boolean oneTimeCrasherFlag) {
        this.oneTimeCrasherFlag = oneTimeCrasherFlag;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Long getCrashRevision() {
        return crashRevision;
    }

    public void setCrashRevision(Long crashRevision) {
        this.crashRevision = crashRevision;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
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

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public Integer getArchiveState() {
        return archiveState;
    }

    public void setArchiveState(Integer archiveState) {
        this.archiveState = archiveState;
    }

    public String getArchiveFilename() {
        return archiveFilename;
    }

    public void setArchiveFilename(String archiveFilename) {
        this.archiveFilename = archiveFilename;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public Boolean getFlakyStack() {
        return flakyStack;
    }

    public void setFlakyStack(Boolean flakyStack) {
        this.flakyStack = flakyStack;
    }

    public Boolean getHttpFlag() {
        return httpFlag;
    }

    public void setHttpFlag(Boolean httpFlag) {
        this.httpFlag = httpFlag;
    }

    public String getFuzzerName() {
        return fuzzerName;
    }

    public void setFuzzerName(String fuzzerName) {
        this.fuzzerName = fuzzerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(Long duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public Boolean getSymbolized() {
        return symbolized;
    }

    public void setSymbolized(Boolean symbolized) {
        this.symbolized = symbolized;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getGroupBugInformation() {
        return groupBugInformation;
    }

    public void setGroupBugInformation(Long groupBugInformation) {
        this.groupBugInformation = groupBugInformation;
    }

    public List<String> getGestures() {
        return gestures;
    }

    public void setGestures(List<String> gestures) {
        this.gestures = gestures;
    }

    public Integer getRedzone() {
        return redzone;
    }

    public void setRedzone(Integer redzone) {
        this.redzone = redzone;
    }

    public Boolean getDisableUbsan() {
        return disableUbsan;
    }

    public void setDisableUbsan(Boolean disableUbsan) {
        this.disableUbsan = disableUbsan;
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
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

    public Boolean getTriaged() {
        return triaged;
    }

    public void setTriaged(Boolean triaged) {
        this.triaged = triaged;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public Boolean getHasBugFlag() {
        return hasBugFlag;
    }

    public void setHasBugFlag(Boolean hasBugFlag) {
        this.hasBugFlag = hasBugFlag;
    }

    public List<String> getBugIndices() {
        return bugIndices;
    }

    public void setBugIndices(List<String> bugIndices) {
        this.bugIndices = bugIndices;
    }

    public String getOverriddenFuzzerName() {
        return overriddenFuzzerName;
    }

    public void setOverriddenFuzzerName(String overriddenFuzzerName) {
        this.overriddenFuzzerName = overriddenFuzzerName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public List<String> getImpactIndices() {
        return impactIndices;
    }

    public void setImpactIndices(List<String> impactIndices) {
        this.impactIndices = impactIndices;
    }

    public Boolean getIsADuplicateFlag() {
        return isADuplicateFlag;
    }

    public void setIsADuplicateFlag(Boolean isADuplicateFlag) {
        this.isADuplicateFlag = isADuplicateFlag;
    }

    public Boolean getIsLeader() {
        return isLeader;
    }

    public void setIsLeader(Boolean isLeader) {
        this.isLeader = isLeader;
    }

    // Utility methods
    public boolean isSecurityBug() {
        return Boolean.TRUE.equals(securityFlag);
    }

    public boolean isOpen() {
        return Boolean.TRUE.equals(open);
    }

    public boolean isLeader() {
        return Boolean.TRUE.equals(isLeader);
    }

    public boolean hasBug() {
        return Boolean.TRUE.equals(hasBugFlag);
    }

    public boolean isDuplicate() {
        return Boolean.TRUE.equals(isADuplicateFlag);
    }

    // equals, hashCode, and toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Testcase testcase = (Testcase) o;
        return Objects.equals(id, testcase.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Testcase{" +
                "id=" + id +
                ", crashType='" + crashType + '\'' +
                ", crashState='" + crashState + '\'' +
                ", fuzzerName='" + fuzzerName + '\'' +
                ", jobType='" + jobType + '\'' +
                ", status='" + status + '\'' +
                ", projectName='" + projectName + '\'' +
                ", platform='" + platform + '\'' +
                ", securityFlag=" + securityFlag +
                ", open=" + open +
                ", timestamp=" + timestamp +
                '}';
    }
}