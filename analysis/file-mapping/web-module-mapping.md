# Web Module File Mapping: Python → Java

## Overview
Mapping of all web-related Python files to their Java Spring Boot equivalents in the `clusterfuzz-web` module.

## File Mappings

### Core Handlers (Controllers)
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/appengine/handlers/base_handler.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/BaseController.java` | Base controller functionality | ✅ Implemented |
| `src/appengine/handlers/testcase.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/TestcaseController.java` | Testcase management | ✅ Implemented |
| `src/appengine/handlers/testcase_list.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/TestcaseListController.java` | Testcase listing | ⏳ Planned |
| `src/appengine/handlers/jobs.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/JobController.java` | Job management | ✅ Implemented |
| `src/appengine/handlers/fuzzers.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/FuzzerController.java` | Fuzzer management | ✅ Implemented |
| `src/appengine/handlers/bots.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/BotController.java` | Bot management | ✅ Implemented |
| `src/appengine/handlers/configuration.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/ConfigurationController.java` | System configuration | ⏳ Planned |
| `src/appengine/handlers/home.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/HomeController.java` | Dashboard/home page | ⏳ Planned |

### Authentication & Security
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/appengine/handlers/auth.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/AuthController.java` | Authentication | ✅ Implemented |
| `src/appengine/handlers/domain_verifier.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/security/DomainVerifier.java` | Domain verification | ⏳ Planned |
| `src/appengine/handlers/report_csp_failure.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/security/CspFailureController.java` | CSP violation reporting | ⏳ Planned |

### File & Data Management
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/appengine/handlers/upload_testcase.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/UploadController.java` | File upload handling | ⏳ Planned |
| `src/appengine/handlers/viewer.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/ViewerController.java` | File viewing | ⏳ Planned |
| `src/appengine/handlers/download.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/DownloadController.java` | File download | ⏳ Planned |
| `src/appengine/handlers/gcs_redirect.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/GcsRedirectController.java` | GCS file redirection | ⏳ Planned |

### Statistics & Reporting
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/appengine/handlers/crash_stats.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/CrashStatsController.java` | Crash statistics | ⏳ Planned |
| `src/appengine/handlers/fuzzer_stats.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/FuzzerStatsController.java` | Fuzzer statistics | ⏳ Planned |
| `src/appengine/handlers/coverage_report.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/CoverageController.java` | Code coverage reports | ⏳ Planned |
| `src/appengine/handlers/crash_query.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/CrashQueryController.java` | Crash querying | ⏳ Planned |

### External Integrations
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/appengine/handlers/external_update.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/ExternalUpdateController.java` | External system updates | ⏳ Planned |
| `src/appengine/handlers/issue_tracker.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/IssueTrackerController.java` | Issue tracker integration | ⏳ Planned |
| `src/appengine/handlers/bisection.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/BisectionController.java` | Bisection management | ⏳ Planned |

### Utilities & Helpers
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/appengine/handlers/help_redirector.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/HelpController.java` | Help system | ⏳ Planned |
| `src/appengine/handlers/commit_range.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/CommitRangeController.java` | Commit range analysis | ⏳ Planned |
| `src/appengine/handlers/corpora.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/controller/CorporaController.java` | Corpus management | ⏳ Planned |

### GraphQL Layer
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| N/A (New) | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/graphql/TestcaseGraphQLController.java` | GraphQL testcase queries | ✅ Implemented |
| N/A (New) | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/graphql/BotGraphQLController.java` | GraphQL bot queries | ⏳ Planned |
| N/A (New) | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/graphql/JobGraphQLController.java` | GraphQL job queries | ⏳ Planned |

### Services & Business Logic
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/appengine/libs/auth.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/service/AuthenticationService.java` | Authentication service | ✅ Implemented |
| `src/appengine/libs/helpers.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/service/HelperService.java` | Common utilities | ⏳ Planned |
| `src/appengine/libs/form.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/service/FormValidationService.java` | Form validation | ⏳ Planned |
| `src/appengine/libs/issue_management.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/service/IssueManagementService.java` | Issue management | ⏳ Planned |

### Security & Filters
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/appengine/libs/access.py` | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/security/AccessControlService.java` | Access control | ⏳ Planned |
| N/A (New) | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/security/JwtAuthenticationFilter.java` | JWT authentication filter | ✅ Implemented |
| N/A (New) | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/service/JwtService.java` | JWT token service | ✅ Implemented |

### DTOs & Data Transfer
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| N/A (New) | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/dto/AuthenticationRequest.java` | Auth request DTO | ✅ Implemented |
| N/A (New) | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/dto/AuthenticationResponse.java` | Auth response DTO | ✅ Implemented |
| N/A (New) | `clusterfuzz-web/src/main/java/com/google/clusterfuzz/web/dto/RefreshTokenRequest.java` | Token refresh DTO | ✅ Implemented |

## Implementation Priority

### Phase 1 (Immediate - Core Controllers)
1. **TestcaseListController.java** - Essential for testcase management
2. **ConfigurationController.java** - System configuration
3. **HomeController.java** - Dashboard functionality
4. **UploadController.java** - File upload handling

### Phase 2 (Short-term - Statistics & Reporting)
1. **CrashStatsController.java** - Crash analytics
2. **FuzzerStatsController.java** - Performance metrics
3. **CoverageController.java** - Code coverage
4. **CrashQueryController.java** - Search functionality

### Phase 3 (Medium-term - Advanced Features)
1. **IssueTrackerController.java** - External integrations
2. **BisectionController.java** - Advanced analysis
3. **CorporaController.java** - Corpus management
4. **ExternalUpdateController.java** - System updates

## Architecture Notes

### Spring Boot Patterns
- **@RestController**: For REST API endpoints
- **@Controller**: For traditional MVC endpoints
- **@Service**: For business logic
- **@Repository**: For data access
- **@Component**: For utilities

### Security Integration
- **@PreAuthorize**: Method-level security
- **@Secured**: Role-based access
- **JWT Filter**: Token-based authentication
- **CORS Configuration**: Cross-origin support

### Validation & Error Handling
- **@Valid**: Input validation
- **@ControllerAdvice**: Global exception handling
- **Custom Validators**: Business rule validation
- **Error DTOs**: Structured error responses

## Total Files Mapped: 35+ web-related Python files