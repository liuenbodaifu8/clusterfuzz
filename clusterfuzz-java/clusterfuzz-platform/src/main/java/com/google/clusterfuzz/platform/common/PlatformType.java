package com.google.clusterfuzz.platform.common;

/**
 * Enumeration of supported platform types.
 */
public enum PlatformType {
    LINUX("Linux"),
    WINDOWS("Windows"),
    ANDROID("Android"),
    MACOS("macOS"),
    UNKNOWN("Unknown");

    private final String displayName;

    PlatformType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Detects the current platform type.
     */
    public static PlatformType detectCurrent() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("linux")) {
            return LINUX;
        } else if (osName.contains("windows")) {
            return WINDOWS;
        } else if (osName.contains("mac")) {
            return MACOS;
        } else if (osName.contains("android")) {
            return ANDROID;
        } else {
            return UNKNOWN;
        }
    }
}