package com.google.clusterfuzz.analysis.stack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StackAnalyzerTest {

    private StackAnalyzer stackAnalyzer;

    @BeforeEach
    void setUp() {
        stackAnalyzer = new StackAnalyzer();
    }

    @Test
    void analyzeStackTrace_WithGdbStackTrace_ShouldParseCorrectly() {
        // Given
        String gdbStackTrace = """
            #0  0x00007ffff7a05428 in __GI_raise (sig=sig@entry=6) at ../sysdeps/unix/sysv/linux/raise.c:54
            #1  0x00007ffff7a0702a in __GI_abort () at abort.c:89
            #2  0x0000000000401234 in vulnerable_function (input=0x7fffffffe000) at test.c:42
            #3  0x0000000000401156 in main (argc=2, argv=0x7fffffffe1c8) at test.c:15
            """;

        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace(gdbStackTrace);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFrames()).hasSize(4);
        assertThat(result.getFrameCount()).isEqualTo(4);
        assertThat(result.isHasSymbols()).isTrue();
        
        StackFrame firstFrame = result.getFrames().get(0);
        assertThat(firstFrame.getFrameNumber()).isEqualTo(0);
        assertThat(firstFrame.getAddress()).isEqualTo("0x00007ffff7a05428");
        assertThat(firstFrame.getFunction()).isEqualTo("__GI_raise");
        assertThat(firstFrame.getFile()).isEqualTo("../sysdeps/unix/sysv/linux/raise.c");
        assertThat(firstFrame.getLine()).isEqualTo(54);
    }

    @Test
    void analyzeStackTrace_WithAddressSanitizerTrace_ShouldParseCorrectly() {
        // Given
        String asanStackTrace = """
            ==1234==ERROR: AddressSanitizer: heap-buffer-overflow on address 0x60200000eff4 at pc 0x000000401234 bp 0x7fff12345678 sp 0x7fff12345670
            READ of size 4 at 0x60200000eff4 thread T0
                #0 0x401233 in vulnerable_function /path/to/test.c:42:5
                #1 0x401155 in main /path/to/test.c:15:3
                #2 0x7ffff7a05b96 in __libc_start_main (/lib/x86_64-linux-gnu/libc.so.6+0x21b96)
            """;

        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace(asanStackTrace);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFrames()).hasSize(3);
        assertThat(result.isHasSymbols()).isTrue();
        
        StackFrame firstFrame = result.getFrames().get(0);
        assertThat(firstFrame.getAddress()).isEqualTo("0x401233");
        assertThat(firstFrame.getFunction()).isEqualTo("vulnerable_function");
        assertThat(firstFrame.getFile()).isEqualTo("/path/to/test.c");
        assertThat(firstFrame.getLine()).isEqualTo(42);
        assertThat(firstFrame.getColumn()).isEqualTo(5);
    }

    @Test
    void analyzeStackTrace_WithWindowsStackTrace_ShouldParseCorrectly() {
        // Given
        String windowsStackTrace = """
            ntdll.dll!NtTerminateProcess+0x14
            kernel32.dll!ExitProcess+0x14
            test.exe!vulnerable_function+0x23 [c:\\path\\to\\test.c @ 42]
            test.exe!main+0x15 [c:\\path\\to\\test.c @ 15]
            """;

        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace(windowsStackTrace);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFrames()).hasSize(4);
        assertThat(result.isHasSymbols()).isTrue();
        
        StackFrame thirdFrame = result.getFrames().get(2);
        assertThat(thirdFrame.getModule()).isEqualTo("test.exe");
        assertThat(thirdFrame.getFunction()).isEqualTo("vulnerable_function");
        assertThat(thirdFrame.getFile()).isEqualTo("c:\\path\\to\\test.c");
        assertThat(thirdFrame.getLine()).isEqualTo(42);
    }

    @Test
    void analyzeStackTrace_WithNoSymbols_ShouldParseAddressesOnly() {
        // Given
        String noSymbolsTrace = """
            0x00401234
            0x00401156
            0x7ffff7a05b96
            """;

        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace(noSymbolsTrace);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFrames()).hasSize(3);
        assertThat(result.isHasSymbols()).isFalse();
        
        StackFrame firstFrame = result.getFrames().get(0);
        assertThat(firstFrame.getAddress()).isEqualTo("0x00401234");
        assertThat(firstFrame.getFunction()).isNull();
        assertThat(firstFrame.getFile()).isNull();
    }

    @Test
    void analyzeStackTrace_WithEmptyTrace_ShouldReturnEmptyResult() {
        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace("");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFrames()).isEmpty();
        assertThat(result.getFrameCount()).isEqualTo(0);
        assertThat(result.isHasSymbols()).isFalse();
    }

    @Test
    void analyzeStackTrace_WithNullTrace_ShouldThrowException() {
        // When & Then
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> stackAnalyzer.analyzeStackTrace(null)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "#0  0x401234 in func() at file.c:42",
        "    #1 0x401156 in main() at file.c:15",
        "\t#2  0x7ffff7a05b96 in __libc_start_main"
    })
    void analyzeStackTrace_WithVariousFormats_ShouldParseCorrectly(String singleFrame) {
        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace(singleFrame);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFrames()).hasSize(1);
        assertThat(result.getFrames().get(0).getAddress()).isNotNull();
    }

    @Test
    void extractBinaryPath_FromStackTrace_ShouldIdentifyMainBinary() {
        // Given
        String stackTrace = """
            #0  0x401234 in vulnerable_function() at /path/to/test.c:42
            #1  0x401156 in main() at /path/to/test.c:15
            #2  0x7ffff7a05b96 in __libc_start_main (/lib/x86_64-linux-gnu/libc.so.6+0x21b96)
            """;

        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace(stackTrace);

        // Then
        assertThat(result.getBinaryPath()).isNotNull();
        // Should identify the main binary from the stack trace
    }

    @Test
    void analyzeStackTrace_WithInlinedFunctions_ShouldHandleCorrectly() {
        // Given
        String inlinedStackTrace = """
            #0  0x401234 in inlined_function() at test.c:42
            #1  0x401234 in caller_function() at test.c:38
            #2  0x401156 in main() at test.c:15
            """;

        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace(inlinedStackTrace);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFrames()).hasSize(3);
        // First two frames should have same address (inlined)
        assertThat(result.getFrames().get(0).getAddress())
            .isEqualTo(result.getFrames().get(1).getAddress());
    }

    @Test
    void analyzeStackTrace_WithCorruptedTrace_ShouldHandleGracefully() {
        // Given
        String corruptedTrace = """
            #0  0x401234 in func() at
            #1  corrupted line
            #2  0x401156 in main() at test.c:15
            """;

        // When
        StackAnalysisResult result = stackAnalyzer.analyzeStackTrace(corruptedTrace);

        // Then
        assertThat(result).isNotNull();
        // Should parse what it can and skip corrupted lines
        assertThat(result.getFrames()).hasSizeGreaterThan(0);
    }
}