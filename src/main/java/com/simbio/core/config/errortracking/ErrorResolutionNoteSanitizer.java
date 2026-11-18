package com.simbio.core.config.errortracking;

import java.util.regex.Pattern;

final class ErrorResolutionNoteSanitizer {

    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(?i)(password|passwd|secret|token|authorization|cookie|api[_-]?key)\\s*[:=]\\s*[^\\s,;]+");
    private static final Pattern UUID = Pattern.compile(
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    private static final Pattern PHONE = Pattern.compile("(?<!\\w)\\+?[0-9][0-9 .()\\-]{6,}[0-9](?!\\w)");
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<!\\w)[0-9]{5,}(?!\\w)");
    private static final int MAX_LENGTH = 500;

    private ErrorResolutionNoteSanitizer() {
    }

    static String sanitize(String note) {
        if (note == null) {
            throw new IllegalArgumentException("Resolution note is required");
        }
        if (note.length() > 2_000) {
            throw new IllegalArgumentException("Resolution note is too long");
        }
        String sanitized = note.replaceAll("[\\r\\n\\t\\p{Cntrl}]", " ").trim();
        sanitized = EMAIL.matcher(sanitized).replaceAll("[redacted]");
        sanitized = CREDENTIAL.matcher(sanitized).replaceAll("$1=[redacted]");
        sanitized = UUID.matcher(sanitized).replaceAll("{id}");
        sanitized = PHONE.matcher(sanitized).replaceAll("[redacted]");
        sanitized = LONG_NUMBER.matcher(sanitized).replaceAll("[redacted]");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Resolution note must contain text");
        }
        return sanitized.length() <= MAX_LENGTH ? sanitized : sanitized.substring(0, MAX_LENGTH);
    }
}
