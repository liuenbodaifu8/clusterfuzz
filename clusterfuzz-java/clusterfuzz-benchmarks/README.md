# ClusterFuzz Performance Benchmarks

This module contains comprehensive performance benchmarks for ClusterFuzz components using JMH (Java Microbenchmark Harness).

## Overview

The benchmark suite tests critical performance aspects of:
- **Repository Operations**: Database access patterns and query performance
- **Service Layer**: Business logic performance under load
- **Concurrency**: Multi-threaded operations and scalability

## Benchmark Classes

### RepositoryBenchmark
Tests database repository performance including:
- Basic CRUD operations
- Complex queries with joins
- Pagination performance
- Bulk operations
- Index utilization

### ServiceBenchmark
Tests service layer performance including:
- Bot registration and management
- Business logic execution
- Cache utilization
- Transaction performance

### ConcurrencyBenchmark
Tests concurrent operations including:
- Multi-threaded database writes
- Read/write mixed workloads
- Async operation performance
- Batch processing efficiency

## Running Benchmarks

### Prerequisites
- Java 17+
- Maven 3.8+
- At least 4GB RAM available for JVM

### Quick Start

```bash
# Build the benchmark JAR
mvn clean package -pl clusterfuzz-benchmarks

# Run all benchmarks
java -jar clusterfuzz-benchmarks/target/clusterfuzz-benchmarks.jar

# Run specific benchmark
java -jar clusterfuzz-benchmarks/target/clusterfuzz-benchmarks.jar RepositoryBenchmark

# Run with custom JVM options
java -Xmx4g -XX:+UseG1GC -jar clusterfuzz-benchmarks/target/clusterfuzz-benchmarks.jar
```

### Maven Integration

```bash
# Run benchmarks via Maven
mvn exec:java -pl clusterfuzz-benchmarks

# Run specific benchmark class
mvn exec:java -pl clusterfuzz-benchmarks -Dexec.args="RepositoryBenchmark"
```

### Programmatic Execution

```java
// Run repository benchmarks only
BenchmarkRunner.runRepositoryBenchmarks();

// Run service benchmarks only
BenchmarkRunner.runServiceBenchmarks();

// Run concurrency benchmarks only
BenchmarkRunner.runConcurrencyBenchmarks();
```

## Configuration

### JMH Configuration
Benchmarks are configured with:
- **Warmup**: 3 iterations, 1 second each
- **Measurement**: 5 iterations, 2 seconds each
- **Forks**: 1 (can be increased for more accurate results)
- **Mode**: Average time measurement

### Database Configuration
Benchmarks use H2 in-memory database with:
- Connection pool: 20 max connections
- Batch processing enabled
- Query optimization enabled
- Realistic data volumes (1000+ test records)

### JVM Configuration
Recommended JVM settings:
```bash
-Xmx4g                    # 4GB heap
-Xms2g                    # 2GB initial heap
-XX:+UseG1GC              # G1 garbage collector
-XX:MaxGCPauseMillis=100  # Low GC pause target
```

## Benchmark Results

### Interpreting Results
- **Score**: Average execution time (lower is better)
- **Error**: Confidence interval (±)
- **Units**: Microseconds (μs) or milliseconds (ms)
- **Samples**: Number of measurements taken

### Example Output
```
Benchmark                                    Mode  Cnt    Score    Error  Units
RepositoryBenchmark.benchmarkFindAll         avgt    5   45.123 ± 2.456  us/op
RepositoryBenchmark.benchmarkFindByStatus    avgt    5   12.789 ± 1.234  us/op
ServiceBenchmark.benchmarkRegisterBot        avgt    5  156.789 ± 5.678  us/op
ConcurrencyBenchmark.benchmarkMultiThreaded  avgt    5   89.456 ± 3.789  ms/op
```

### Performance Targets
Based on ClusterFuzz requirements:

| Operation | Target | Acceptable |
|-----------|--------|------------|
| Simple Query | < 50μs | < 100μs |
| Complex Query | < 200μs | < 500μs |
| Service Call | < 1ms | < 5ms |
| Concurrent Write | < 100ms | < 500ms |

## Continuous Integration

### Automated Benchmarking
Benchmarks can be integrated into CI/CD pipelines:

```yaml
# GitHub Actions example
- name: Run Performance Benchmarks
  run: |
    mvn clean package -pl clusterfuzz-benchmarks
    java -jar clusterfuzz-benchmarks/target/clusterfuzz-benchmarks.jar > benchmark-results.txt
    
- name: Archive Benchmark Results
  uses: actions/upload-artifact@v3
  with:
    name: benchmark-results
    path: benchmark-results.txt
```

### Performance Regression Detection
Compare results against baseline:
```bash
# Save baseline
java -jar clusterfuzz-benchmarks.jar > baseline.txt

# Compare current results
java -jar clusterfuzz-benchmarks.jar > current.txt
diff baseline.txt current.txt
```

## Customization

### Adding New Benchmarks
1. Create new benchmark class in `com.google.clusterfuzz.benchmarks`
2. Annotate with JMH annotations:
   ```java
   @BenchmarkMode(Mode.AverageTime)
   @OutputTimeUnit(TimeUnit.MICROSECONDS)
   @State(Scope.Benchmark)
   public class MyBenchmark {
       @Benchmark
       public void myBenchmarkMethod(Blackhole bh) {
           // Benchmark code
       }
   }
   ```
3. Add to BenchmarkRunner if needed

### Custom Configuration
Override default settings in `application-benchmark.yml`:
```yaml
jmh:
  benchmark:
    warmup-iterations: 5
    measurement-iterations: 10
    forks: 2
```

## Troubleshooting

### Common Issues

**OutOfMemoryError**
- Increase heap size: `-Xmx8g`
- Reduce data set size in benchmark setup

**Slow Execution**
- Reduce iterations for development: `-wi 1 -i 1`
- Use fewer forks: `-f 1`

**Inconsistent Results**
- Ensure stable system load
- Increase warmup iterations
- Use multiple forks for statistical significance

### Debug Mode
Run with debug logging:
```bash
java -Dlogging.level.com.google.clusterfuzz=DEBUG \
     -jar clusterfuzz-benchmarks.jar
```

## Best Practices

1. **Stable Environment**: Run on dedicated hardware with minimal background processes
2. **Multiple Runs**: Execute benchmarks multiple times and compare results
3. **Baseline Comparison**: Always compare against known baseline performance
4. **Resource Monitoring**: Monitor CPU, memory, and I/O during benchmark execution
5. **Version Control**: Track benchmark results over time to detect regressions

## Integration with Monitoring

Benchmark results can be integrated with monitoring systems:
- Export to Prometheus metrics
- Send to InfluxDB for time-series analysis
- Alert on performance regressions
- Dashboard visualization with Grafana

## Contributing

When adding new benchmarks:
1. Follow existing naming conventions
2. Include comprehensive documentation
3. Add validation tests
4. Update this README
5. Consider performance impact on CI/CD pipeline