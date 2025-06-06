package com.google.clusterfuzz.web.controller;

import com.google.clusterfuzz.core.entity.Testcase;
import com.google.clusterfuzz.core.service.TestcaseService;
import com.google.clusterfuzz.web.dto.request.TestcaseCreateRequest;
import com.google.clusterfuzz.web.dto.request.TestcaseUpdateRequest;
import com.google.clusterfuzz.web.dto.response.ApiResponse;
import com.google.clusterfuzz.web.dto.response.TestcaseResponse;
import com.google.clusterfuzz.web.dto.response.TestcaseStatsResponse;
import com.google.clusterfuzz.web.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced REST Controller for Testcase operations.
 * Provides comprehensive CRUD, search, and management operations for testcases.
 * Implements 12+ endpoints as per Week 7 plan with standardized API responses.
 */
@RestController
@RequestMapping("/api/v1/testcases")
@Tag(name = "Testcases", description = "Testcase management operations")
public class TestcaseControllerV2 extends BaseController {

    private final TestcaseService testcaseService;

    @Autowired
    public TestcaseControllerV2(TestcaseService testcaseService) {
        this.testcaseService = testcaseService;
    }

    /**
     * 1. GET /api/v1/testcases - List testcases with filtering
     */
    @Operation(summary = "List testcases", description = "Retrieve a paginated list of testcases with filtering options")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved testcases"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<TestcaseResponse>>> listTestcases(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "timestamp") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Filter by project") @RequestParam(required = false) String project,
            @Parameter(description = "Filter by fuzzer") @RequestParam(required = false) String fuzzer,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by security flag") @RequestParam(required = false) Boolean security,
            HttpServletRequest request) {

        return executeWithLogging(request, "LIST_TESTCASES", () -> {
            Pageable pageable = createPageable(page, size, sortBy, sortDir);
            Page<Testcase> testcasePage = testcaseService.findAllWithFilters(pageable, project, fuzzer, status, security);
            List<TestcaseResponse> responses = testcasePage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            return success(testcasePage.map(t -> convertToResponse(t)));
        });
    }

    /**
     * 2. GET /api/v1/testcases/{id} - Get testcase details
     */
    @Operation(summary = "Get testcase", description = "Retrieve a specific testcase by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<TestcaseResponse>> getTestcase(
            @Parameter(description = "Testcase ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {

        return executeWithLogging(request, "GET_TESTCASE", () -> {
            validateId(id, "testcase");
            Testcase testcase = testcaseService.findById(id)
                .orElseThrow(() -> ApiException.notFound("TESTCASE_NOT_FOUND", "Testcase not found with ID: " + id));
            return success(convertToResponse(testcase));
        });
    }

    /**
     * 3. POST /api/v1/testcases - Create new testcase
     */
    @Operation(summary = "Create testcase", description = "Create a new testcase")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestcaseResponse>> createTestcase(
            @Parameter(description = "Testcase data", required = true) @Valid @RequestBody TestcaseCreateRequest request,
            HttpServletRequest httpRequest) {

        return executeWithLogging(httpRequest, "CREATE_TESTCASE", () -> {
            Testcase testcase = convertFromCreateRequest(request);
            Testcase savedTestcase = testcaseService.save(testcase);
            return created(convertToResponse(savedTestcase));
        });
    }

    /**
     * 4. PUT /api/v1/testcases/{id} - Update testcase
     */
    @Operation(summary = "Update testcase", description = "Update an existing testcase")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestcaseResponse>> updateTestcase(
            @Parameter(description = "Testcase ID", required = true) @PathVariable Long id,
            @Parameter(description = "Updated testcase data", required = true) @Valid @RequestBody TestcaseUpdateRequest request,
            HttpServletRequest httpRequest) {

        return executeWithLogging(httpRequest, "UPDATE_TESTCASE", () -> {
            validateId(id, "testcase");
            if (!testcaseService.existsById(id)) {
                throw ApiException.notFound("TESTCASE_NOT_FOUND", "Testcase not found with ID: " + id);
            }
            
            Testcase testcase = convertFromUpdateRequest(id, request);
            Testcase updatedTestcase = testcaseService.save(testcase);
            return success(convertToResponse(updatedTestcase), "Testcase updated successfully");
        });
    }

    /**
     * 5. DELETE /api/v1/testcases/{id} - Delete testcase
     */
    @Operation(summary = "Delete testcase", description = "Delete a testcase by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTestcase(
            @Parameter(description = "Testcase ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {

        return executeWithLogging(request, "DELETE_TESTCASE", () -> {
            validateId(id, "testcase");
            if (!testcaseService.existsById(id)) {
                throw ApiException.notFound("TESTCASE_NOT_FOUND", "Testcase not found with ID: " + id);
            }
            
            testcaseService.deleteById(id);
            return noContent();
        });
    }

    /**
     * 6. POST /api/v1/testcases/{id}/minimize - Minimize testcase
     */
    @Operation(summary = "Minimize testcase", description = "Start minimization process for a testcase")
    @PostMapping("/{id}/minimize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestcaseResponse>> minimizeTestcase(
            @Parameter(description = "Testcase ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {

        return executeWithLogging(request, "MINIMIZE_TESTCASE", () -> {
            validateId(id, "testcase");
            Testcase testcase = testcaseService.startMinimization(id);
            return success(convertToResponse(testcase), "Minimization started successfully");
        });
    }

    /**
     * 7. POST /api/v1/testcases/{id}/analyze - Analyze testcase
     */
    @Operation(summary = "Analyze testcase", description = "Start analysis process for a testcase")
    @PostMapping("/{id}/analyze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestcaseResponse>> analyzeTestcase(
            @Parameter(description = "Testcase ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {

        return executeWithLogging(request, "ANALYZE_TESTCASE", () -> {
            validateId(id, "testcase");
            Testcase testcase = testcaseService.startAnalysis(id);
            return success(convertToResponse(testcase), "Analysis started successfully");
        });
    }

    /**
     * 8. GET /api/v1/testcases/{id}/file - Download testcase file
     */
    @Operation(summary = "Download testcase file", description = "Download the testcase file content")
    @GetMapping("/{id}/file")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> downloadTestcaseFile(
            @Parameter(description = "Testcase ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {

        return executeWithLogging(request, "DOWNLOAD_TESTCASE_FILE", () -> {
            validateId(id, "testcase");
            Map<String, Object> fileData = testcaseService.getTestcaseFile(id);
            return success(fileData, "File retrieved successfully");
        });
    }

    /**
     * 9. POST /api/v1/testcases/{id}/file - Upload testcase file
     */
    @Operation(summary = "Upload testcase file", description = "Upload a new testcase file")
    @PostMapping("/{id}/file")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TestcaseResponse>> uploadTestcaseFile(
            @Parameter(description = "Testcase ID", required = true) @PathVariable Long id,
            @Parameter(description = "File to upload", required = true) @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        return executeWithLogging(request, "UPLOAD_TESTCASE_FILE", () -> {
            validateId(id, "testcase");
            validateRequired(file, "file");
            
            Testcase testcase = testcaseService.updateTestcaseFile(id, file);
            return success(convertToResponse(testcase), "File uploaded successfully");
        });
    }

    /**
     * 10. POST /api/v1/testcases/batch - Batch operations
     */
    @Operation(summary = "Batch operations", description = "Perform batch operations on multiple testcases")
    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchOperations(
            @Parameter(description = "Batch operation request") @Valid @RequestBody Map<String, Object> batchRequest,
            HttpServletRequest request) {

        return executeWithLogging(request, "BATCH_OPERATIONS", () -> {
            Map<String, Object> result = testcaseService.performBatchOperations(batchRequest);
            return success(result, "Batch operations completed successfully");
        });
    }

    /**
     * 11. GET /api/v1/testcases/{id}/history - Get testcase history
     */
    @Operation(summary = "Get testcase history", description = "Retrieve the history of changes for a testcase")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTestcaseHistory(
            @Parameter(description = "Testcase ID", required = true) @PathVariable Long id,
            HttpServletRequest request) {

        return executeWithLogging(request, "GET_TESTCASE_HISTORY", () -> {
            validateId(id, "testcase");
            List<Map<String, Object>> history = testcaseService.getTestcaseHistory(id);
            return success(history, "History retrieved successfully");
        });
    }

    /**
     * 12. GET /api/v1/testcases/stats - Get testcase statistics
     */
    @Operation(summary = "Get testcase statistics", description = "Retrieve various statistics about testcases")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<TestcaseStatsResponse>> getTestcaseStats(
            @Parameter(description = "Date range filter") @RequestParam(required = false) String dateRange,
            HttpServletRequest request) {

        return executeWithLogging(request, "GET_TESTCASE_STATS", () -> {
            TestcaseStatsResponse stats = testcaseService.getTestcaseStatistics(dateRange);
            return success(stats, "Statistics retrieved successfully");
        });
    }

    // Helper methods for conversion

    private TestcaseResponse convertToResponse(Testcase testcase) {
        TestcaseResponse response = new TestcaseResponse();
        response.setId(testcase.getId());
        response.setCrashType(testcase.getCrashType());
        response.setCrashState(testcase.getCrashState());
        response.setSecurityFlag(testcase.getSecurityFlag());
        response.setProjectName(testcase.getProjectName());
        response.setFuzzerName(testcase.getFuzzerName());
        response.setJobType(testcase.getJobType());
        response.setPlatform(testcase.getPlatform());
        response.setTimestamp(testcase.getTimestamp());
        response.setStatus(testcase.getStatus());
        response.setTriaged(testcase.getTriaged());
        response.setOpen(testcase.getOpen());
        response.setFixed(testcase.getFixed());
        response.setRegression(testcase.getRegression());
        response.setOneTimeCrasherFlag(testcase.getOneTimeCrasherFlag());
        response.setBugInformation(testcase.getBugInformation());
        response.setGroupBugInformation(testcase.getGroupBugInformation());
        response.setGroupId(testcase.getGroupId());
        response.setImpactExtendedStableVersion(testcase.getImpactExtendedStableVersion());
        response.setImpactStableVersion(testcase.getImpactStableVersion());
        response.setImpactBetaVersion(testcase.getImpactBetaVersion());
        response.setImpactHeadVersion(testcase.getImpactHeadVersion());
        response.setIsImpactSetFlag(testcase.getIsImpactSetFlag());
        return response;
    }

    private Testcase convertFromCreateRequest(TestcaseCreateRequest request) {
        Testcase testcase = new Testcase();
        testcase.setCrashType(request.getCrashType());
        testcase.setCrashState(request.getCrashState());
        testcase.setSecurityFlag(request.getSecurityFlag());
        testcase.setProjectName(request.getProjectName());
        testcase.setFuzzerName(request.getFuzzerName());
        testcase.setJobType(request.getJobType());
        testcase.setPlatform(request.getPlatform());
        testcase.setTimestamp(LocalDateTime.now());
        testcase.setStatus("NEW");
        testcase.setTriaged(false);
        testcase.setOpen(true);
        testcase.setFixed(false);
        testcase.setRegression(false);
        testcase.setOneTimeCrasherFlag(request.getOneTimeCrasherFlag());
        return testcase;
    }

    private Testcase convertFromUpdateRequest(Long id, TestcaseUpdateRequest request) {
        Testcase testcase = testcaseService.findById(id)
            .orElseThrow(() -> ApiException.notFound("TESTCASE_NOT_FOUND", "Testcase not found with ID: " + id));
        
        if (request.getCrashType() != null) testcase.setCrashType(request.getCrashType());
        if (request.getCrashState() != null) testcase.setCrashState(request.getCrashState());
        if (request.getSecurityFlag() != null) testcase.setSecurityFlag(request.getSecurityFlag());
        if (request.getStatus() != null) testcase.setStatus(request.getStatus());
        if (request.getTriaged() != null) testcase.setTriaged(request.getTriaged());
        if (request.getOpen() != null) testcase.setOpen(request.getOpen());
        if (request.getFixed() != null) testcase.setFixed(request.getFixed());
        if (request.getRegression() != null) testcase.setRegression(request.getRegression());
        if (request.getBugInformation() != null) testcase.setBugInformation(request.getBugInformation());
        
        return testcase;
    }
}