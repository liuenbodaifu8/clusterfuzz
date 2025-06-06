package com.google.clusterfuzz.analysis.stack;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stack trace analysis service for parsing and analyzing crash stack traces.
 */
@Service
@Slf4j
public class StackAnalyzer {

    // Common stack frame patterns
    private static final Pattern GDB_FRAME_PATTERN = Pattern.compile(
        "#(\\d+)\\s+0x([0-9a-fA-F]+)\\s+in\\s+([^\\s]+)\\s*\\(.*?\\)\\s*(?:at\\s+([^:]+):(\\d+))?");
    
    private static final Pattern ASAN_FRAME_PATTERN = Pattern.compile(
        "#(\\d+)\\s+0x([0-9a-fA-F]+)\\s+in\\s+([^\\s]+)\\s+([^:]+):(\\d+):(\\d+)");
    
    private static final Pattern SIMPLE_FRAME_PATTERN = Pattern.compile(
        "\\s*at\\s+([^\\(]+)\\(([^:]+):(\\d+)\\)");

    /**
     * Analyzes a stack trace and extracts structured information.
     *
     * @param stacktrace Raw stack trace string
     * @param binaryPath Path to the binary (for symbol resolution)
     * @return Structured stack analysis result
     */
    public StackAnalysisResult analyzeStackTrace(String stacktrace, String binaryPath) {
        log.debug("Analyzing stack trace for binary: {}", binaryPath);

        if (stacktrace == null || stacktrace.trim().isEmpty()) {
            return StackAnalysisResult.builder()
                .frames(new ArrayList<>())
                .frameCount(0)
                .hasSymbols(false)
                .build();
        }

        List<StackFrame> frames = parseStackFrames(stacktrace);
        boolean hasSymbols = frames.stream().anyMatch(frame -> 
            frame.getFunction() != null && !frame.getFunction().startsWith("0x"));

        return StackAnalysisResult.builder()
            .frames(frames)
            .frameCount(frames.size())
            .hasSymbols(hasSymbols)
            .rawStacktrace(stacktrace)
            .binaryPath(binaryPath)
            .build();
    }

    /**
     * Parses stack frames from raw stack trace text.
     */
    private List<StackFrame> parseStackFrames(String stacktrace) {
        List<StackFrame> frames = new ArrayList<>();
        String[] lines = stacktrace.split("\n");

        for (String line : lines) {
            StackFrame frame = parseStackFrame(line.trim());
            if (frame != null) {
                frames.add(frame);
            }
        }

        return frames;
    }

    /**
     * Parses a single stack frame from a line of text.
     */
    private StackFrame parseStackFrame(String line) {
        // Try GDB format first
        Matcher gdbMatcher = GDB_FRAME_PATTERN.matcher(line);
        if (gdbMatcher.find()) {
            return StackFrame.builder()
                .frameNumber(Integer.parseInt(gdbMatcher.group(1)))
                .address(gdbMatcher.group(2))
                .function(gdbMatcher.group(3))
                .file(gdbMatcher.group(4))
                .line(gdbMatcher.group(5) != null ? Integer.parseInt(gdbMatcher.group(5)) : null)
                .build();
        }

        // Try AddressSanitizer format
        Matcher asanMatcher = ASAN_FRAME_PATTERN.matcher(line);
        if (asanMatcher.find()) {
            return StackFrame.builder()
                .frameNumber(Integer.parseInt(asanMatcher.group(1)))
                .address(asanMatcher.group(2))
                .function(asanMatcher.group(3))
                .file(asanMatcher.group(4))
                .line(Integer.parseInt(asanMatcher.group(5)))
                .column(Integer.parseInt(asanMatcher.group(6)))
                .build();
        }

        // Try simple format
        Matcher simpleMatcher = SIMPLE_FRAME_PATTERN.matcher(line);
        if (simpleMatcher.find()) {
            return StackFrame.builder()
                .function(simpleMatcher.group(1))
                .file(simpleMatcher.group(2))
                .line(Integer.parseInt(simpleMatcher.group(3)))
                .build();
        }

        // If no pattern matches, try to extract basic information
        if (line.contains("0x") && (line.contains("in ") || line.contains("at "))) {
            return StackFrame.builder()
                .function(extractFunctionName(line))
                .address(extractAddress(line))
                .build();
        }

        return null;
    }

    /**
     * Extracts function name from a stack frame line.
     */
    private String extractFunctionName(String line) {
        // Look for "in function_name" pattern
        Pattern inPattern = Pattern.compile("in\\s+([^\\s\\(]+)");
        Matcher matcher = inPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Look for "at function_name" pattern
        Pattern atPattern = Pattern.compile("at\\s+([^\\s\\(]+)");
        matcher = atPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "unknown";
    }

    /**
     * Extracts memory address from a stack frame line.
     */
    private String extractAddress(String line) {
        Pattern addressPattern = Pattern.compile("0x([0-9a-fA-F]+)");
        Matcher matcher = addressPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Symbolizes stack frames using debug information.
     * This is a placeholder for actual symbolization logic.
     */
    public StackAnalysisResult symbolizeStackTrace(StackAnalysisResult result, String symbolsPath) {
        log.debug("Symbolizing stack trace with symbols from: {}", symbolsPath);
        
        // TODO: Implement actual symbolization logic
        // This would involve:
        // 1. Loading debug symbols from the binary
        // 2. Resolving addresses to function names and line numbers
        // 3. Updating the stack frames with resolved information
        
        return result;
    }

    /**
     * Filters out noise frames from the stack trace.
     */
    public List<StackFrame> filterNoiseFrames(List<StackFrame> frames) {
        List<StackFrame> filtered = new ArrayList<>();
        
        for (StackFrame frame : frames) {
            if (!isNoiseFrame(frame)) {
                filtered.add(frame);
            }
        }
        
        return filtered;
    }

    /**
     * Determines if a stack frame is noise (system/library frames).
     */
    private boolean isNoiseFrame(StackFrame frame) {
        if (frame.getFunction() == null) {
            return false;
        }
        
        String function = frame.getFunction().toLowerCase();
        
        // Common noise patterns
        return function.startsWith("__") ||
               function.contains("libc") ||
               function.contains("libpthread") ||
               function.contains("kernel") ||
               function.equals("main") ||
               function.equals("start");
    }
}