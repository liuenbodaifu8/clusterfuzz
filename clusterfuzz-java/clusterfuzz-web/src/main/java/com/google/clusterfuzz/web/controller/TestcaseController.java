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
 * REST Controller for Testcase operations.
 * Provides comprehensive CRUD, search, and management operations for testcases.
 * Implements 12+ endpoints as per Week 7 plan.
 */
@RestController
@RequestMapping("/api/v1/testcases")
@Tag(name = "Testcases", description = "Testcase management operations")
public class TestcaseController extends BaseController {

    private final TestcaseService testcaseService;

    @Autowired
    public TestcaseController(TestcaseService testcaseService) {
        this.testcaseService = testcaseService;
    }

    @Operation(summary = "Get all testcases", description = "Retrieve a paginated list of all testcases")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved testcases",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestcaseDto.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TestcaseDto>> getAllTestcases(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "timestamp")
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @Parameter(description = "Sort direction", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Testcase> testcases = testcaseService.findAll(pageable);
        Page<TestcaseDto> testcaseDtos = testcases.map(testcaseMapper::toDto);
        
        return ResponseEntity.ok(testcaseDtos);
    }

    @Operation(summary = "Get testcase by ID", description = "Retrieve a specific testcase by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Testcase found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestcaseDto.class))),
        @ApiResponse(responseCode = "404", description = "Testcase not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TestcaseDto> getTestcaseById(
            @Parameter(description = "Testcase ID", required = true, example = "1")
            @PathVariable Long id) {
        
        Optional<Testcase> testcase = testcaseService.findById(id);
        if (testcase.isPresent()) {
            TestcaseDto dto = testcaseMapper.toDto(testcase.get());
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Create new testcase", description = "Create a new testcase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Testcase created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestcaseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestcaseDto> createTestcase(
            @Parameter(description = "Testcase data", required = true)
            @Valid @RequestBody TestcaseDto testcaseDto) {
        
        Testcase testcase = testcaseMapper.toEntity(testcaseDto);
        Testcase savedTestcase = testcaseService.save(testcase);
        TestcaseDto responseDto = testcaseMapper.toDto(savedTestcase);
        
        logger.info("Created new testcase with ID: {}", savedTestcase.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Operation(summary = "Update testcase", description = "Update an existing testcase")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Testcase updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestcaseDto.class))),
        @ApiResponse(responseCode = "404", description = "Testcase not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestcaseDto> updateTestcase(
            @Parameter(description = "Testcase ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Updated testcase data", required = true)
            @Valid @RequestBody TestcaseDto testcaseDto) {
        
        if (!testcaseService.exists(id)) {
            return ResponseEntity.notFound().build();
        }
        
        testcaseDto.setId(id);
        Testcase testcase = testcaseMapper.toEntity(testcaseDto);
        Testcase updatedTestcase = testcaseService.save(testcase);
        TestcaseDto responseDto = testcaseMapper.toDto(updatedTestcase);
        
        logger.info("Updated testcase with ID: {}", id);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "Delete testcase", description = "Delete a testcase by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Testcase deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Testcase not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTestcase(
            @Parameter(description = "Testcase ID", required = true, example = "1")
            @PathVariable Long id) {
        
        if (!testcaseService.exists(id)) {
            return ResponseEntity.notFound().build();
        }
        
        testcaseService.deleteById(id);
        logger.info("Deleted testcase with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search testcases", description = "Search testcases by various criteria")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TestcaseDto.class)))
    })
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<TestcaseDto>> searchTestcases(
            @Parameter(description = "Search term", example = "heap-buffer-overflow")
            @RequestParam String query,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Testcase> testcases = testcaseService.searchTestcases(query, pageable);
        Page<TestcaseDto> testcaseDtos = testcases.map(testcaseMapper::toDto);
        
        return ResponseEntity.ok(testcaseDtos);
    }

    @Operation(summary = "Get open testcases", description = "Retrieve all open testcases")
    @GetMapping("/open")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TestcaseDto>> getOpenTestcases() {
        List<Testcase> testcases = testcaseService.findOpenTestcases();
        List<TestcaseDto> dtos = testcases.stream()
                .map(testcaseMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get security bugs", description = "Retrieve all security-related testcases")
    @GetMapping("/security")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TestcaseDto>> getSecurityBugs() {
        List<Testcase> testcases = testcaseService.findSecurityBugs();
        List<TestcaseDto> dtos = testcases.stream()
                .map(testcaseMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get testcases by project", description = "Retrieve testcases for a specific project")
    @GetMapping("/project/{projectName}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TestcaseDto>> getTestcasesByProject(
            @Parameter(description = "Project name", required = true, example = "chromium")
            @PathVariable String projectName) {
        
        List<Testcase> testcases = testcaseService.findByProject(projectName);
        List<TestcaseDto> dtos = testcases.stream()
                .map(testcaseMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Get testcases by fuzzer", description = "Retrieve testcases found by a specific fuzzer")
    @GetMapping("/fuzzer/{fuzzerName}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TestcaseDto>> getTestcasesByFuzzer(
            @Parameter(description = "Fuzzer name", required = true, example = "libfuzzer")
            @PathVariable String fuzzerName) {
        
        List<Testcase> testcases = testcaseService.findByFuzzer(fuzzerName);
        List<TestcaseDto> dtos = testcases.stream()
                .map(testcaseMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Mark testcase as triaged", description = "Mark a testcase as triaged")
    @PostMapping("/{id}/triage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestcaseDto> markAsTriaged(
            @Parameter(description = "Testcase ID", required = true, example = "1")
            @PathVariable Long id) {
        
        try {
            Testcase testcase = testcaseService.markAsTriaged(id);
            TestcaseDto dto = testcaseMapper.toDto(testcase);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Close testcase", description = "Close an open testcase")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestcaseDto> closeTestcase(
            @Parameter(description = "Testcase ID", required = true, example = "1")
            @PathVariable Long id) {
        
        try {
            Testcase testcase = testcaseService.closeTestcase(id);
            TestcaseDto dto = testcaseMapper.toDto(testcase);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Reopen testcase", description = "Reopen a closed testcase")
    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestcaseDto> reopenTestcase(
            @Parameter(description = "Testcase ID", required = true, example = "1")
            @PathVariable Long id) {
        
        try {
            Testcase testcase = testcaseService.reopenTestcase(id);
            TestcaseDto dto = testcaseMapper.toDto(testcase);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Mark as duplicate", description = "Mark a testcase as duplicate of another")
    @PostMapping("/{id}/duplicate/{duplicateOfId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TestcaseDto> markAsDuplicate(
            @Parameter(description = "Testcase ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID of the original testcase", required = true, example = "2")
            @PathVariable Long duplicateOfId) {
        
        try {
            Testcase testcase = testcaseService.markAsDuplicate(id, duplicateOfId);
            TestcaseDto dto = testcaseMapper.toDto(testcase);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get testcase statistics", description = "Get various statistics about testcases")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TestcaseStatsDto> getTestcaseStats() {
        TestcaseStatsDto stats = new TestcaseStatsDto();
        stats.setTotalTestcases(testcaseService.findAll().size());
        stats.setOpenTestcases(testcaseService.countOpenTestcases());
        stats.setSecurityBugs(testcaseService.countSecurityBugs());
        stats.setRecentTestcases(testcaseService.countRecentTestcases(LocalDateTime.now().minusDays(7)));
        
        return ResponseEntity.ok(stats);
    }

    // Inner class for statistics DTO
    public static class TestcaseStatsDto {
        private long totalTestcases;
        private long openTestcases;
        private long securityBugs;
        private long recentTestcases;

        // Getters and setters
        public long getTotalTestcases() { return totalTestcases; }
        public void setTotalTestcases(long totalTestcases) { this.totalTestcases = totalTestcases; }
        
        public long getOpenTestcases() { return openTestcases; }
        public void setOpenTestcases(long openTestcases) { this.openTestcases = openTestcases; }
        
        public long getSecurityBugs() { return securityBugs; }
        public void setSecurityBugs(long securityBugs) { this.securityBugs = securityBugs; }
        
        public long getRecentTestcases() { return recentTestcases; }
        public void setRecentTestcases(long recentTestcases) { this.recentTestcases = recentTestcases; }
    }
}