package com.google.clusterfuzz.core.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File utility functions for ClusterFuzz.
 * Provides safe file operations, compression, and path handling.
 */
@UtilityClass
@Slf4j
public class FileUtils {

    /**
     * Safely reads a file to string with size limit.
     */
    public static String readFileToString(Path filePath, long maxSize) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        
        long fileSize = Files.size(filePath);
        if (fileSize > maxSize) {
            throw new IOException("File too large: " + fileSize + " bytes (max: " + maxSize + ")");
        }
        
        return Files.readString(filePath);
    }

    /**
     * Compresses a file using GZIP.
     */
    public static void compressFile(Path source, Path target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source.toFile());
             FileOutputStream fos = new FileOutputStream(target.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, length);
            }
        }
    }

    /**
     * Decompresses a GZIP file.
     */
    public static void decompressFile(Path source, Path target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source.toFile());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(target.toFile())) {
            
            byte[] buffer = new byte[8192];
            int length;
            while ((length = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    /**
     * Creates a temporary directory with proper cleanup.
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * Safely deletes a directory and all its contents.
     */
    public static void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        Files.walk(directory)
            .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.warn("Failed to delete: " + path, e);
                }
            });
    }
}