package com.google.clusterfuzz.web.graphql;

import com.google.clusterfuzz.core.entity.Testcase;
import com.google.clusterfuzz.core.service.TestcaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * GraphQL controller for Testcase operations.
 * Provides optimized queries with field-level resolution.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class TestcaseGraphQLController {

    private final TestcaseService testcaseService;

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Optional<Testcase> testcase(@Argument Long id) {
        log.debug("GraphQL query: testcase(id: {})", id);
        return testcaseService.findById(id);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<Testcase> testcases(
            @Argument Optional<String> status,
            @Argument Optional<String> jobType,
            @Argument Optional<Boolean> securityFlag,
            @Argument Optional<Integer> limit
    ) {
        log.debug("GraphQL query: testcases(status: {}, jobType: {}, securityFlag: {}, limit: {})", 
                 status, jobType, securityFlag, limit);
        
        int pageSize = limit.orElse(50);
        Pageable pageable = PageRequest.of(0, Math.min(pageSize, 1000)); // Max 1000 results
        
        if (status.isPresent()) {
            Page<Testcase> page = testcaseService.findByStatus(status.get(), pageable);
            return page.getContent();
        } else if (jobType.isPresent()) {
            return testcaseService.findByJobType(jobType.get());
        } else if (securityFlag.isPresent()) {
            return testcaseService.findBySecurityFlag(securityFlag.get());
        } else {
            Page<Testcase> page = testcaseService.findAll(pageable);
            return page.getContent();
        }
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<Testcase> recentTestcases(
            @Argument Optional<Integer> days,
            @Argument Optional<Boolean> securityOnly,
            @Argument Optional<Integer> limit
    ) {
        log.debug("GraphQL query: recentTestcases(days: {}, securityOnly: {}, limit: {})", 
                 days, securityOnly, limit);
        
        LocalDateTime since = LocalDateTime.now().minusDays(days.orElse(7));
        boolean security = securityOnly.orElse(false);
        int pageSize = limit.orElse(100);
        
        Pageable pageable = PageRequest.of(0, Math.min(pageSize, 1000));
        Page<Testcase> page = testcaseService.findRecentTestcases(since, security, pageable);
        return page.getContent();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public TestcaseStatistics testcaseStatistics() {
        log.debug("GraphQL query: testcaseStatistics");
        
        long total = testcaseService.count();
        long security = testcaseService.countBySecurityFlag(true);
        long processed = testcaseService.countByStatus("Processed");
        long pending = testcaseService.countByStatus("Pending");
        
        return TestcaseStatistics.builder()
                .total(total)
                .security(security)
                .processed(processed)
                .pending(pending)
                .build();
    }

    @SchemaMapping(typeName = "Testcase", field = "relatedTestcases")
    public List<Testcase> relatedTestcases(Testcase testcase) {
        log.debug("GraphQL field resolver: relatedTestcases for testcase {}", testcase.getId());
        
        // Find testcases with same crash state or similar characteristics
        if (testcase.getCrashState() != null) {
            return testcaseService.findByCrashState(testcase.getCrashState())
                    .stream()
                    .filter(t -> !t.getId().equals(testcase.getId()))
                    .limit(10)
                    .toList();
        }
        return List.of();
    }

    @SchemaMapping(typeName = "Testcase", field = "similarTestcases")
    public List<Testcase> similarTestcases(Testcase testcase) {
        log.debug("GraphQL field resolver: similarTestcases for testcase {}", testcase.getId());
        
        // Find testcases with same job type and crash type
        return testcaseService.findByJobTypeAndCrashType(
                testcase.getJobType(), 
                testcase.getCrashType()
        ).stream()
         .filter(t -> !t.getId().equals(testcase.getId()))
         .limit(5)
         .toList();
    }

    /**
     * Statistics DTO for GraphQL responses.
     */
    public static class TestcaseStatistics {
        private final long total;
        private final long security;
        private final long processed;
        private final long pending;

        private TestcaseStatistics(long total, long security, long processed, long pending) {
            this.total = total;
            this.security = security;
            this.processed = processed;
            this.pending = pending;
        }

        public static TestcaseStatisticsBuilder builder() {
            return new TestcaseStatisticsBuilder();
        }

        public long getTotal() { return total; }
        public long getSecurity() { return security; }
        public long getProcessed() { return processed; }
        public long getPending() { return pending; }

        public static class TestcaseStatisticsBuilder {
            private long total;
            private long security;
            private long processed;
            private long pending;

            public TestcaseStatisticsBuilder total(long total) {
                this.total = total;
                return this;
            }

            public TestcaseStatisticsBuilder security(long security) {
                this.security = security;
                return this;
            }

            public TestcaseStatisticsBuilder processed(long processed) {
                this.processed = processed;
                return this;
            }

            public TestcaseStatisticsBuilder pending(long pending) {
                this.pending = pending;
                return this;
            }

            public TestcaseStatistics build() {
                return new TestcaseStatistics(total, security, processed, pending);
            }
        }
    }
}