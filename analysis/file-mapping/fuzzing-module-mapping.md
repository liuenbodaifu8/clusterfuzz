# Fuzzing Module File Mapping: Python → Java

## Overview
Mapping of all fuzzing-related Python files to their Java Spring Boot equivalents in the `clusterfuzz-fuzzing` module.

## File Mappings

### Core Fuzzing Engine
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/fuzzing/fuzzer_selection.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/FuzzerSelector.java` | Fuzzer selection logic | ✅ Interface Defined |
| `src/clusterfuzz/_internal/fuzzing/strategy.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/FuzzingStrategy.java` | Fuzzing strategies | ✅ Interface Defined |
| `src/clusterfuzz/_internal/fuzzing/corpus_manager.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/CorpusManager.java` | Corpus management | ✅ Interface Defined |
| `src/clusterfuzz/_internal/fuzzing/gesture_handler.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/GestureHandler.java` | UI gesture handling | ⏳ Planned |
| `src/clusterfuzz/_internal/fuzzing/leak_blacklist.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/LeakBlacklist.java` | Memory leak filtering | ⏳ Planned |

### LibFuzzer Integration
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/bot/fuzzers/libFuzzer/engine.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/libfuzzer/LibFuzzerEngine.java` | LibFuzzer engine | ✅ Interface Defined |
| `src/clusterfuzz/_internal/bot/fuzzers/libFuzzer/launcher.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/libfuzzer/LibFuzzerLauncher.java` | LibFuzzer launcher | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/libFuzzer/stats.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/libfuzzer/LibFuzzerStats.java` | LibFuzzer statistics | ⏳ Planned |

### AFL Integration
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/bot/fuzzers/afl/engine.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/afl/AflEngine.java` | AFL engine | ✅ Interface Defined |
| `src/clusterfuzz/_internal/bot/fuzzers/afl/launcher.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/afl/AflLauncher.java` | AFL launcher | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/afl/stats.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/afl/AflStats.java` | AFL statistics | ⏳ Planned |

### Honggfuzz Integration
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/bot/fuzzers/honggfuzz/engine.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/honggfuzz/HonggfuzzEngine.java` | Honggfuzz engine | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/honggfuzz/launcher.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/honggfuzz/HonggfuzzLauncher.java` | Honggfuzz launcher | ⏳ Planned |

### Centipede Integration
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/bot/fuzzers/centipede/engine.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/centipede/CentipedeEngine.java` | Centipede engine | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/centipede/launcher.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/centipede/CentipedeLauncher.java` | Centipede launcher | ⏳ Planned |

### Common Fuzzing Utilities
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/bot/fuzzers/engine_common.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/common/EngineCommon.java` | Common engine utilities | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/dictionary_manager.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/common/DictionaryManager.java` | Dictionary management | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/options.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/common/FuzzingOptions.java` | Fuzzing options | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/utils.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/common/FuzzingUtils.java` | Fuzzing utilities | ⏳ Planned |

### Mutation & Generation
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/bot/fuzzers/ml/rnn/generator.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/ml/RnnGenerator.java` | ML-based generation | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/mutator.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/mutation/Mutator.java` | Input mutation | ⏳ Planned |

### Corpus Management
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/google_cloud_utils/gsutil.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/corpus/GcsCorpusManager.java` | GCS corpus storage | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/corpus_manager.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/corpus/LocalCorpusManager.java` | Local corpus management | ⏳ Planned |

### Performance & Monitoring
| Python File | Java Equivalent | Purpose | Status |
|-------------|----------------|---------|---------|
| `src/clusterfuzz/_internal/metrics/fuzzer_stats.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/metrics/FuzzerMetrics.java` | Fuzzer performance metrics | ⏳ Planned |
| `src/clusterfuzz/_internal/bot/fuzzers/engine_stats.py` | `clusterfuzz-fuzzing/src/main/java/com/google/clusterfuzz/fuzzing/metrics/EngineStats.java` | Engine statistics | ⏳ Planned |

## Implementation Priority

### Phase 1 (Immediate - Core Interfaces)
1. **FuzzingEngine.java** - Base engine interface ✅ Done
2. **LibFuzzerEngine.java** - Primary fuzzer ✅ Done
3. **AflEngine.java** - Secondary fuzzer ✅ Done
4. **FuzzerSelector.java** - Engine selection logic

### Phase 2 (Short-term - Core Implementation)
1. **LibFuzzerLauncher.java** - LibFuzzer execution
2. **CorpusManager.java** - Corpus handling
3. **FuzzingStrategy.java** - Strategy patterns
4. **EngineCommon.java** - Shared utilities

### Phase 3 (Medium-term - Advanced Engines)
1. **HonggfuzzEngine.java** - Additional fuzzer
2. **CentipedeEngine.java** - Google's fuzzer
3. **DictionaryManager.java** - Dictionary support
4. **FuzzingOptions.java** - Configuration

### Phase 4 (Long-term - ML & Advanced Features)
1. **RnnGenerator.java** - ML-based generation
2. **Mutator.java** - Advanced mutation
3. **GcsCorpusManager.java** - Cloud storage
4. **FuzzerMetrics.java** - Performance monitoring

## Architecture Notes

### Design Patterns
- **Strategy Pattern**: For different fuzzing engines
- **Factory Pattern**: For engine creation
- **Observer Pattern**: For progress monitoring
- **Command Pattern**: For fuzzing operations

### Spring Integration
- **@Service**: For fuzzing services
- **@Component**: For engine implementations
- **@Async**: For background fuzzing
- **@Scheduled**: For periodic tasks

### Configuration
- **@ConfigurationProperties**: Engine-specific settings
- **@Profile**: Environment-specific engines
- **@ConditionalOnProperty**: Feature toggles

## Total Files Mapped: 25+ fuzzing-related Python files