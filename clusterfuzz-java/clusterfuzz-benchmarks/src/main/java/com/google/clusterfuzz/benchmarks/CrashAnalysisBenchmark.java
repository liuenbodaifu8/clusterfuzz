package com.google.clusterfuzz.benchmarks;

import com.google.clusterfuzz.analysis.crash.CrashAnalyzer;
import com.google.clusterfuzz.analysis.crash.CrashAnalysisResult;
import com.google.clusterfuzz.core.entity.Testcase;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for crash analysis performance.
 * Tests the performance of crash analysis algorithms under various conditions.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class CrashAnalysisBenchmark {

    private CrashAnalyzer crashAnalyzer;
    private Testcase simpleTestcase;
    private Testcase complexTestcase;
    private Testcase largeStacktraceTestcase;

    @Setup
    public void setup() {
        crashAnalyzer = new CrashAnalyzer();
        
        // Simple crash case
        simpleTestcase = new Testcase();
        simpleTestcase.setId(1L);
        simpleTestcase.setCrashType("SEGV");
        simpleTestcase.setCrashAddress("0x41414141");
        simpleTestcase.setCrashStacktrace(generateSimpleStacktrace());
        
        // Complex crash case with multiple analysis points
        complexTestcase = new Testcase();
        complexTestcase.setId(2L);
        complexTestcase.setCrashType("heap-buffer-overflow");
        complexTestcase.setCrashAddress("0x60200000eff4");
        complexTestcase.setCrashStacktrace(generateComplexStacktrace());
        
        // Large stacktrace case
        largeStacktraceTestcase = new Testcase();
        largeStacktraceTestcase.setId(3L);
        largeStacktraceTestcase.setCrashType("stack-overflow");
        largeStacktraceTestcase.setCrashAddress("0x7fff12345678");
        largeStacktraceTestcase.setCrashStacktrace(generateLargeStacktrace());
    }

    @Benchmark
    public void benchmarkSimpleCrashAnalysis(Blackhole bh) {
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(simpleTestcase);
        bh.consume(result);
    }

    @Benchmark
    public void benchmarkComplexCrashAnalysis(Blackhole bh) {
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(complexTestcase);
        bh.consume(result);
    }

    @Benchmark
    public void benchmarkLargeStacktraceAnalysis(Blackhole bh) {
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(largeStacktraceTestcase);
        bh.consume(result);
    }

    @Benchmark
    @Group("parallel")
    @GroupThreads(4)
    public void benchmarkParallelCrashAnalysis(Blackhole bh) {
        CrashAnalysisResult result = crashAnalyzer.analyzeCrash(simpleTestcase);
        bh.consume(result);
    }

    @Benchmark
    public void benchmarkBatchCrashAnalysis(Blackhole bh) {
        for (int i = 0; i < 10; i++) {
            CrashAnalysisResult result = crashAnalyzer.analyzeCrash(simpleTestcase);
            bh.consume(result);
        }
    }

    private String generateSimpleStacktrace() {
        return """
            #0  0x00401234 in vulnerable_function (input=0x7fffffffe000) at test.c:42
            #1  0x00401156 in main (argc=2, argv=0x7fffffffe1c8) at test.c:15
            #2  0x7ffff7a05b96 in __libc_start_main (/lib/x86_64-linux-gnu/libc.so.6+0x21b96)
            """;
    }

    private String generateComplexStacktrace() {
        return """
            ==1234==ERROR: AddressSanitizer: heap-buffer-overflow on address 0x60200000eff4 at pc 0x000000401234 bp 0x7fff12345678 sp 0x7fff12345670
            READ of size 4 at 0x60200000eff4 thread T0
                #0 0x401233 in vulnerable_function /path/to/test.c:42:5
                #1 0x401155 in parse_input /path/to/test.c:38:3
                #2 0x401100 in process_data /path/to/test.c:25:7
                #3 0x401080 in handle_request /path/to/test.c:20:2
                #4 0x401050 in main /path/to/test.c:15:3
                #5 0x7ffff7a05b96 in __libc_start_main (/lib/x86_64-linux-gnu/libc.so.6+0x21b96)
                #6 0x401009 in _start (/path/to/test+0x1009)

            0x60200000eff4 is located 0 bytes to the right of 4-byte region [0x60200000eff0,0x60200000eff4)
            allocated by thread T0 here:
                #0 0x7ffff7b02e40 in malloc (/usr/lib/x86_64-linux-gnu/libasan.so.4+0xdee40)
                #1 0x401200 in allocate_buffer /path/to/test.c:30:12
                #2 0x401155 in parse_input /path/to/test.c:35:15
            """;
    }

    private String generateLargeStacktrace() {
        StringBuilder sb = new StringBuilder();
        sb.append("Stack overflow detected at 0x7fff12345678\n");
        
        for (int i = 0; i < 100; i++) {
            sb.append(String.format("#%d  0x%08x in recursive_function_%d (depth=%d) at test.c:%d\n", 
                i, 0x401000 + i * 16, i % 10, i, 100 + i));
        }
        
        return sb.toString();
    }
}