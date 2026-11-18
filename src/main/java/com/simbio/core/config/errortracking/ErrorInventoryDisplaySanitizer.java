package com.simbio.core.config.errortracking;

public final class ErrorInventoryDisplaySanitizer {

    private ErrorInventoryDisplaySanitizer() {
    }

    public static String path(String value) {
        return ErrorTrackingSanitizer.safePath(value, 2_048);
    }

    public static String resolutionNote(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            return ErrorResolutionNoteSanitizer.sanitize(value);
        } catch (IllegalArgumentException exception) {
            return "[redacted]";
        }
    }
}
