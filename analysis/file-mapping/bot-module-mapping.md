# Bot Module File Mapping: Python → Java

## Overview
Mapping of all bot-related Python files to their Java Spring Boot equivalents in the `clusterfuzz-bot` module.

## File Mappings

### Startup & Lifecycle
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/python/bot/startup/run_bot.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/BotApplication.java` | Main bot application entry point | ✅ Implemented |
| `src/python/bot/startup/run.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/runner/BotRunner.java` | Bot execution runner | ⏳ Planned |
| `src/python/bot/startup/heartbeat.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/service/HeartbeatService.java` | Heartbeat management | ⏳ Planned |
| `src/python/bot/startup/run_heartbeat.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/scheduler/HeartbeatScheduler.java` | Heartbeat scheduling | ⏳ Planned |
| `src/python/bot/startup/android_heartbeat.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/platform/AndroidHeartbeatService.java` | Android-specific heartbeat | ⏳ Planned |
| `src/python/bot/startup/health_check_responder.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/health/HealthCheckController.java` | Health check endpoint | ⏳ Planned |
| `src/python/bot/startup/run_cron.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/scheduler/CronScheduler.java` | Cron job management | ⏳ Planned |

### Task Management
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/python/bot/tasks/task.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/task/Task.java` | Base task interface | ⏳ Planned |
| `src/python/bot/tasks/fuzz_task.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/task/FuzzTask.java` | Fuzzing task implementation | ⏳ Planned |
| `src/python/bot/tasks/minimize_task.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/task/MinimizeTask.java` | Test case minimization | ⏳ Planned |
| `src/python/bot/tasks/progression_task.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/task/ProgressionTask.java` | Regression testing | ⏳ Planned |
| `src/python/bot/tasks/analyze_task.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/task/AnalyzeTask.java` | Crash analysis task | ⏳ Planned |
| `src/python/bot/tasks/corpus_pruning_task.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/task/CorpusPruningTask.java` | Corpus optimization | ⏳ Planned |
| `src/python/bot/tasks/variant_task.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/task/VariantTask.java` | Variant analysis | ⏳ Planned |

### Fuzzing Engines
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/python/bot/fuzzers/libFuzzer/launcher.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/fuzzer/libfuzzer/LibFuzzerLauncher.java` | LibFuzzer integration | ⏳ Planned |
| `src/python/bot/fuzzers/afl/launcher.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/fuzzer/afl/AflLauncher.java` | AFL integration | ⏳ Planned |
| `src/python/bot/fuzzers/honggfuzz/launcher.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/fuzzer/honggfuzz/HonggfuzzLauncher.java` | Honggfuzz integration | ⏳ Planned |
| `src/python/bot/fuzzers/engine.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/fuzzer/FuzzingEngine.java` | Base fuzzing engine | ⏳ Planned |
| `src/python/bot/fuzzers/engine_common.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/fuzzer/EngineCommon.java` | Common fuzzing utilities | ⏳ Planned |

### Platform Support
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/python/platforms/linux/__init__.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/platform/LinuxPlatform.java` | Linux platform support | ⏳ Planned |
| `src/python/platforms/windows/__init__.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/platform/WindowsPlatform.java` | Windows platform support | ⏳ Planned |
| `src/python/platforms/android/__init__.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/platform/AndroidPlatform.java` | Android platform support | ⏳ Planned |
| `src/python/platforms/fuchsia/__init__.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/platform/FuchsiaPlatform.java` | Fuchsia platform support | ⏳ Planned |

### Utilities & Common
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/python/bot/webserver/http_server.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/web/BotWebServer.java` | Bot web interface | ⏳ Planned |
| `src/python/system/environment.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/system/Environment.java` | Environment management | ⏳ Planned |
| `src/python/system/process_handler.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/system/ProcessHandler.java` | Process management | ⏳ Planned |
| `src/python/system/shell.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/system/ShellExecutor.java` | Shell command execution | ⏳ Planned |
| `src/python/metrics/logs.py` | `clusterfuzz-bot/src/main/java/com/google/clusterfuzz/bot/metrics/LogService.java` | Logging and metrics | ⏳ Planned |

## Implementation Priority

### Phase 1 (Immediate)
1. **BotRunner.java** - Core bot execution logic
2. **HeartbeatService.java** - Essential for bot lifecycle
3. **Task.java** - Base task interface
4. **FuzzingEngine.java** - Core fuzzing abstraction

### Phase 2 (Short-term)
1. **LibFuzzerLauncher.java** - Primary fuzzing engine
2. **LinuxPlatform.java** - Primary platform support
3. **ProcessHandler.java** - Essential system integration
4. **Environment.java** - Configuration management

### Phase 3 (Medium-term)
1. **AflLauncher.java** - Secondary fuzzing engine
2. **WindowsPlatform.java** - Windows support
3. **HealthCheckController.java** - Monitoring
4. **BotWebServer.java** - Management interface

## Architecture Notes

### Design Patterns
- **Strategy Pattern**: For platform-specific implementations
- **Factory Pattern**: For fuzzing engine creation
- **Observer Pattern**: For task status updates
- **Command Pattern**: For task execution

### Spring Boot Integration
- **@Service**: For business logic components
- **@Component**: For utility classes
- **@Controller**: For web endpoints
- **@Scheduled**: For periodic tasks
- **@Async**: For background processing

### Configuration
- **application.yml**: Bot-specific configuration
- **@ConfigurationProperties**: Type-safe configuration binding
- **@Profile**: Environment-specific behavior

## Total Files Mapped: 25+ bot-related Python files