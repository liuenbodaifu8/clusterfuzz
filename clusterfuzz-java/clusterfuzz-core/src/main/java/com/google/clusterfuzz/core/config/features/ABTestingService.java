package com.google.clusterfuzz.core.config.features;

import com.google.clusterfuzz.core.entity.FeatureFlag;
import com.google.clusterfuzz.core.entity.User;
import com.google.clusterfuzz.core.service.FeatureFlagService;
import com.google.clusterfuzz.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A/B Testing service for managing feature flag experiments.
 * Provides statistical analysis and experiment management capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ABTestingService {

    private final FeatureFlagService featureFlagService;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    // In-memory experiment tracking (in production, this should be persisted)
    private final Map<String, ABExperiment> experiments = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ABTestResult>> userAssignments = new ConcurrentHashMap<>();

    /**
     * Creates a new A/B test experiment.
     */
    public ABExperiment createExperiment(String flagKey, String experimentName, 
                                       int controlPercentage, int treatmentPercentage,
                                       String description, String createdBy) {
        
        if (controlPercentage + treatmentPercentage > 100) {
            throw new IllegalArgumentException("Control and treatment percentages cannot exceed 100%");
        }
        
        Optional<FeatureFlag> flagOpt = featureFlagService.findByKey(flagKey);
        if (flagOpt.isEmpty()) {
            throw new IllegalArgumentException("Feature flag not found: " + flagKey);
        }
        
        ABExperiment experiment = new ABExperiment();
        experiment.setExperimentId(UUID.randomUUID().toString());
        experiment.setFlagKey(flagKey);
        experiment.setExperimentName(experimentName);
        experiment.setDescription(description);
        experiment.setControlPercentage(controlPercentage);
        experiment.setTreatmentPercentage(treatmentPercentage);
        experiment.setStatus(ExperimentStatus.DRAFT);
        experiment.setCreatedBy(createdBy);
        experiment.setCreatedAt(LocalDateTime.now());
        experiment.setMetrics(new ArrayList<>());
        experiment.setResults(new HashMap<>());
        
        experiments.put(experiment.getExperimentId(), experiment);
        
        log.info("Created A/B test experiment: {} for flag: {}", experimentName, flagKey);
        
        return experiment;
    }

    /**
     * Starts an A/B test experiment.
     */
    public void startExperiment(String experimentId, String startedBy) {
        ABExperiment experiment = getExperiment(experimentId);
        
        if (experiment.getStatus() != ExperimentStatus.DRAFT) {
            throw new IllegalStateException("Can only start experiments in DRAFT status");
        }
        
        experiment.setStatus(ExperimentStatus.RUNNING);
        experiment.setStartedAt(LocalDateTime.now());
        experiment.setStartedBy(startedBy);
        
        // Initialize user assignments for this experiment
        userAssignments.put(experimentId, new ConcurrentHashMap<>());
        
        log.info("Started A/B test experiment: {} by {}", experiment.getExperimentName(), startedBy);
    }

    /**
     * Stops an A/B test experiment.
     */
    public void stopExperiment(String experimentId, String stoppedBy) {
        ABExperiment experiment = getExperiment(experimentId);
        
        if (experiment.getStatus() != ExperimentStatus.RUNNING) {
            throw new IllegalStateException("Can only stop running experiments");
        }
        
        experiment.setStatus(ExperimentStatus.STOPPED);
        experiment.setStoppedAt(LocalDateTime.now());
        experiment.setStoppedBy(stoppedBy);
        
        log.info("Stopped A/B test experiment: {} by {}", experiment.getExperimentName(), stoppedBy);
    }

    /**
     * Assigns a user to a test group (control or treatment).
     */
    public ABTestResult assignUserToGroup(String experimentId, String userEmail) {
        ABExperiment experiment = getExperiment(experimentId);
        
        if (experiment.getStatus() != ExperimentStatus.RUNNING) {
            // Return control group for non-running experiments
            return new ABTestResult(experimentId, userEmail, TestGroup.CONTROL, false);
        }
        
        // Check if user is already assigned
        Map<String, ABTestResult> assignments = userAssignments.get(experimentId);
        if (assignments != null && assignments.containsKey(userEmail)) {
            return assignments.get(userEmail);
        }
        
        // Assign user to group based on percentages
        TestGroup group = determineTestGroup(experiment, userEmail);
        boolean featureEnabled = (group == TestGroup.TREATMENT);
        
        ABTestResult result = new ABTestResult(experimentId, userEmail, group, featureEnabled);
        result.setAssignedAt(LocalDateTime.now());
        
        // Store assignment
        if (assignments == null) {
            assignments = new ConcurrentHashMap<>();
            userAssignments.put(experimentId, assignments);
        }
        assignments.put(userEmail, result);
        
        log.debug("Assigned user {} to group {} for experiment {}", userEmail, group, experimentId);
        
        return result;
    }

    /**
     * Records a metric event for an experiment.
     */
    public void recordMetric(String experimentId, String userEmail, String metricName, 
                           double value, Map<String, Object> properties) {
        ABExperiment experiment = getExperiment(experimentId);
        
        // Get user's test group assignment
        ABTestResult assignment = assignUserToGroup(experimentId, userEmail);
        
        MetricEvent event = new MetricEvent();
        event.setExperimentId(experimentId);
        event.setUserEmail(userEmail);
        event.setTestGroup(assignment.getTestGroup());
        event.setMetricName(metricName);
        event.setValue(value);
        event.setProperties(properties);
        event.setTimestamp(LocalDateTime.now());
        
        // Add to experiment metrics
        experiment.getMetrics().add(event);
        
        log.debug("Recorded metric {} = {} for user {} in experiment {}", 
                 metricName, value, userEmail, experimentId);
    }

    /**
     * Analyzes experiment results.
     */
    @Transactional(readOnly = true)
    public ExperimentAnalysis analyzeExperiment(String experimentId) {
        ABExperiment experiment = getExperiment(experimentId);
        
        ExperimentAnalysis analysis = new ExperimentAnalysis();
        analysis.setExperimentId(experimentId);
        analysis.setExperimentName(experiment.getExperimentName());
        analysis.setAnalysisDate(LocalDateTime.now());
        
        // Get all assignments
        Map<String, ABTestResult> assignments = userAssignments.get(experimentId);
        if (assignments == null || assignments.isEmpty()) {
            analysis.setTotalUsers(0);
            analysis.setControlUsers(0);
            analysis.setTreatmentUsers(0);
            return analysis;
        }
        
        // Count users by group
        long controlUsers = assignments.values().stream()
            .mapToLong(a -> a.getTestGroup() == TestGroup.CONTROL ? 1 : 0)
            .sum();
        long treatmentUsers = assignments.values().stream()
            .mapToLong(a -> a.getTestGroup() == TestGroup.TREATMENT ? 1 : 0)
            .sum();
        
        analysis.setTotalUsers(assignments.size());
        analysis.setControlUsers((int) controlUsers);
        analysis.setTreatmentUsers((int) treatmentUsers);
        
        // Analyze metrics
        Map<String, MetricAnalysis> metricAnalyses = analyzeMetrics(experiment, assignments);
        analysis.setMetricAnalyses(metricAnalyses);
        
        // Calculate statistical significance
        analysis.setStatisticallySignificant(calculateStatisticalSignificance(metricAnalyses));
        
        return analysis;
    }

    /**
     * Gets experiment by ID.
     */
    @Transactional(readOnly = true)
    public ABExperiment getExperiment(String experimentId) {
        ABExperiment experiment = experiments.get(experimentId);
        if (experiment == null) {
            throw new IllegalArgumentException("Experiment not found: " + experimentId);
        }
        return experiment;
    }

    /**
     * Gets all experiments.
     */
    @Transactional(readOnly = true)
    public List<ABExperiment> getAllExperiments() {
        return new ArrayList<>(experiments.values());
    }

    /**
     * Gets experiments by status.
     */
    @Transactional(readOnly = true)
    public List<ABExperiment> getExperimentsByStatus(ExperimentStatus status) {
        return experiments.values().stream()
            .filter(exp -> exp.getStatus() == status)
            .toList();
    }

    /**
     * Gets running experiments for a feature flag.
     */
    @Transactional(readOnly = true)
    public List<ABExperiment> getRunningExperimentsForFlag(String flagKey) {
        return experiments.values().stream()
            .filter(exp -> exp.getFlagKey().equals(flagKey) && exp.getStatus() == ExperimentStatus.RUNNING)
            .toList();
    }

    /**
     * Determines test group for a user.
     */
    private TestGroup determineTestGroup(ABExperiment experiment, String userEmail) {
        // Use consistent hashing based on user email and experiment ID
        String hashInput = userEmail + experiment.getExperimentId();
        int hash = Math.abs(hashInput.hashCode());
        int percentage = hash % 100;
        
        if (percentage < experiment.getControlPercentage()) {
            return TestGroup.CONTROL;
        } else if (percentage < experiment.getControlPercentage() + experiment.getTreatmentPercentage()) {
            return TestGroup.TREATMENT;
        } else {
            // User is not in the experiment
            return TestGroup.CONTROL; // Default to control
        }
    }

    /**
     * Analyzes metrics for an experiment.
     */
    private Map<String, MetricAnalysis> analyzeMetrics(ABExperiment experiment, 
                                                      Map<String, ABTestResult> assignments) {
        Map<String, MetricAnalysis> analyses = new HashMap<>();
        
        // Group metrics by name
        Map<String, List<MetricEvent>> metricsByName = experiment.getMetrics().stream()
            .collect(java.util.stream.Collectors.groupingBy(MetricEvent::getMetricName));
        
        for (Map.Entry<String, List<MetricEvent>> entry : metricsByName.entrySet()) {
            String metricName = entry.getKey();
            List<MetricEvent> events = entry.getValue();
            
            MetricAnalysis analysis = analyzeMetric(metricName, events);
            analyses.put(metricName, analysis);
        }
        
        return analyses;
    }

    /**
     * Analyzes a specific metric.
     */
    private MetricAnalysis analyzeMetric(String metricName, List<MetricEvent> events) {
        MetricAnalysis analysis = new MetricAnalysis();
        analysis.setMetricName(metricName);
        
        // Separate events by test group
        List<MetricEvent> controlEvents = events.stream()
            .filter(e -> e.getTestGroup() == TestGroup.CONTROL)
            .toList();
        List<MetricEvent> treatmentEvents = events.stream()
            .filter(e -> e.getTestGroup() == TestGroup.TREATMENT)
            .toList();
        
        // Calculate statistics for control group
        if (!controlEvents.isEmpty()) {
            double controlMean = controlEvents.stream()
                .mapToDouble(MetricEvent::getValue)
                .average()
                .orElse(0.0);
            double controlStdDev = calculateStandardDeviation(
                controlEvents.stream().mapToDouble(MetricEvent::getValue).toArray()
            );
            
            analysis.setControlMean(controlMean);
            analysis.setControlStdDev(controlStdDev);
            analysis.setControlSampleSize(controlEvents.size());
        }
        
        // Calculate statistics for treatment group
        if (!treatmentEvents.isEmpty()) {
            double treatmentMean = treatmentEvents.stream()
                .mapToDouble(MetricEvent::getValue)
                .average()
                .orElse(0.0);
            double treatmentStdDev = calculateStandardDeviation(
                treatmentEvents.stream().mapToDouble(MetricEvent::getValue).toArray()
            );
            
            analysis.setTreatmentMean(treatmentMean);
            analysis.setTreatmentStdDev(treatmentStdDev);
            analysis.setTreatmentSampleSize(treatmentEvents.size());
        }
        
        // Calculate lift and confidence interval
        if (analysis.getControlMean() != null && analysis.getTreatmentMean() != null) {
            double lift = (analysis.getTreatmentMean() - analysis.getControlMean()) / analysis.getControlMean() * 100;
            analysis.setLift(lift);
            
            // Simple confidence interval calculation (95%)
            double confidenceInterval = 1.96 * Math.sqrt(
                (Math.pow(analysis.getControlStdDev(), 2) / analysis.getControlSampleSize()) +
                (Math.pow(analysis.getTreatmentStdDev(), 2) / analysis.getTreatmentSampleSize())
            );
            analysis.setConfidenceInterval(confidenceInterval);
        }
        
        return analysis;
    }

    /**
     * Calculates standard deviation.
     */
    private double calculateStandardDeviation(double[] values) {
        if (values.length <= 1) {
            return 0.0;
        }
        
        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = Arrays.stream(values)
            .map(x -> Math.pow(x - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance);
    }

    /**
     * Calculates statistical significance.
     */
    private boolean calculateStatisticalSignificance(Map<String, MetricAnalysis> metricAnalyses) {
        // Simple significance check - if any metric has confidence interval that doesn't include 0
        return metricAnalyses.values().stream()
            .anyMatch(analysis -> {
                if (analysis.getLift() != null && analysis.getConfidenceInterval() != null) {
                    return Math.abs(analysis.getLift()) > analysis.getConfidenceInterval();
                }
                return false;
            });
    }

    // Data classes for A/B testing

    public static class ABExperiment {
        private String experimentId;
        private String flagKey;
        private String experimentName;
        private String description;
        private int controlPercentage;
        private int treatmentPercentage;
        private ExperimentStatus status;
        private String createdBy;
        private String startedBy;
        private String stoppedBy;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime stoppedAt;
        private List<MetricEvent> metrics;
        private Map<String, Object> results;

        // Getters and setters
        public String getExperimentId() { return experimentId; }
        public void setExperimentId(String experimentId) { this.experimentId = experimentId; }

        public String getFlagKey() { return flagKey; }
        public void setFlagKey(String flagKey) { this.flagKey = flagKey; }

        public String getExperimentName() { return experimentName; }
        public void setExperimentName(String experimentName) { this.experimentName = experimentName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getControlPercentage() { return controlPercentage; }
        public void setControlPercentage(int controlPercentage) { this.controlPercentage = controlPercentage; }

        public int getTreatmentPercentage() { return treatmentPercentage; }
        public void setTreatmentPercentage(int treatmentPercentage) { this.treatmentPercentage = treatmentPercentage; }

        public ExperimentStatus getStatus() { return status; }
        public void setStatus(ExperimentStatus status) { this.status = status; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

        public String getStartedBy() { return startedBy; }
        public void setStartedBy(String startedBy) { this.startedBy = startedBy; }

        public String getStoppedBy() { return stoppedBy; }
        public void setStoppedBy(String stoppedBy) { this.stoppedBy = stoppedBy; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

        public LocalDateTime getStoppedAt() { return stoppedAt; }
        public void setStoppedAt(LocalDateTime stoppedAt) { this.stoppedAt = stoppedAt; }

        public List<MetricEvent> getMetrics() { return metrics; }
        public void setMetrics(List<MetricEvent> metrics) { this.metrics = metrics; }

        public Map<String, Object> getResults() { return results; }
        public void setResults(Map<String, Object> results) { this.results = results; }
    }

    public static class ABTestResult {
        private String experimentId;
        private String userEmail;
        private TestGroup testGroup;
        private boolean featureEnabled;
        private LocalDateTime assignedAt;

        public ABTestResult(String experimentId, String userEmail, TestGroup testGroup, boolean featureEnabled) {
            this.experimentId = experimentId;
            this.userEmail = userEmail;
            this.testGroup = testGroup;
            this.featureEnabled = featureEnabled;
        }

        // Getters and setters
        public String getExperimentId() { return experimentId; }
        public void setExperimentId(String experimentId) { this.experimentId = experimentId; }

        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

        public TestGroup getTestGroup() { return testGroup; }
        public void setTestGroup(TestGroup testGroup) { this.testGroup = testGroup; }

        public boolean isFeatureEnabled() { return featureEnabled; }
        public void setFeatureEnabled(boolean featureEnabled) { this.featureEnabled = featureEnabled; }

        public LocalDateTime getAssignedAt() { return assignedAt; }
        public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    }

    public static class MetricEvent {
        private String experimentId;
        private String userEmail;
        private TestGroup testGroup;
        private String metricName;
        private double value;
        private Map<String, Object> properties;
        private LocalDateTime timestamp;

        // Getters and setters
        public String getExperimentId() { return experimentId; }
        public void setExperimentId(String experimentId) { this.experimentId = experimentId; }

        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

        public TestGroup getTestGroup() { return testGroup; }
        public void setTestGroup(TestGroup testGroup) { this.testGroup = testGroup; }

        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    public static class ExperimentAnalysis {
        private String experimentId;
        private String experimentName;
        private LocalDateTime analysisDate;
        private int totalUsers;
        private int controlUsers;
        private int treatmentUsers;
        private Map<String, MetricAnalysis> metricAnalyses;
        private boolean statisticallySignificant;

        // Getters and setters
        public String getExperimentId() { return experimentId; }
        public void setExperimentId(String experimentId) { this.experimentId = experimentId; }

        public String getExperimentName() { return experimentName; }
        public void setExperimentName(String experimentName) { this.experimentName = experimentName; }

        public LocalDateTime getAnalysisDate() { return analysisDate; }
        public void setAnalysisDate(LocalDateTime analysisDate) { this.analysisDate = analysisDate; }

        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

        public int getControlUsers() { return controlUsers; }
        public void setControlUsers(int controlUsers) { this.controlUsers = controlUsers; }

        public int getTreatmentUsers() { return treatmentUsers; }
        public void setTreatmentUsers(int treatmentUsers) { this.treatmentUsers = treatmentUsers; }

        public Map<String, MetricAnalysis> getMetricAnalyses() { return metricAnalyses; }
        public void setMetricAnalyses(Map<String, MetricAnalysis> metricAnalyses) { this.metricAnalyses = metricAnalyses; }

        public boolean isStatisticallySignificant() { return statisticallySignificant; }
        public void setStatisticallySignificant(boolean statisticallySignificant) { this.statisticallySignificant = statisticallySignificant; }
    }

    public static class MetricAnalysis {
        private String metricName;
        private Double controlMean;
        private Double controlStdDev;
        private Integer controlSampleSize;
        private Double treatmentMean;
        private Double treatmentStdDev;
        private Integer treatmentSampleSize;
        private Double lift;
        private Double confidenceInterval;

        // Getters and setters
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }

        public Double getControlMean() { return controlMean; }
        public void setControlMean(Double controlMean) { this.controlMean = controlMean; }

        public Double getControlStdDev() { return controlStdDev; }
        public void setControlStdDev(Double controlStdDev) { this.controlStdDev = controlStdDev; }

        public Integer getControlSampleSize() { return controlSampleSize; }
        public void setControlSampleSize(Integer controlSampleSize) { this.controlSampleSize = controlSampleSize; }

        public Double getTreatmentMean() { return treatmentMean; }
        public void setTreatmentMean(Double treatmentMean) { this.treatmentMean = treatmentMean; }

        public Double getTreatmentStdDev() { return treatmentStdDev; }
        public void setTreatmentStdDev(Double treatmentStdDev) { this.treatmentStdDev = treatmentStdDev; }

        public Integer getTreatmentSampleSize() { return treatmentSampleSize; }
        public void setTreatmentSampleSize(Integer treatmentSampleSize) { this.treatmentSampleSize = treatmentSampleSize; }

        public Double getLift() { return lift; }
        public void setLift(Double lift) { this.lift = lift; }

        public Double getConfidenceInterval() { return confidenceInterval; }
        public void setConfidenceInterval(Double confidenceInterval) { this.confidenceInterval = confidenceInterval; }
    }

    public enum ExperimentStatus {
        DRAFT, RUNNING, STOPPED, COMPLETED
    }

    public enum TestGroup {
        CONTROL, TREATMENT
    }
}