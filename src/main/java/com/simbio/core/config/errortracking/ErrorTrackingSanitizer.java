package com.simbio.core.config.errortracking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;
import com.simbio.core.shared.spi.ErrorReporter.Source;

final class ErrorTrackingSanitizer {

    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(?i)(password|passwd|secret|token|authorization|cookie|api[_-]?key)\\s*[:=]\\s*[^\\s,;]+");
    private static final Pattern UUID = Pattern.compile(
            "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("(?<=/)[0-9]+(?=/|$)");
    private static final Pattern EMAIL_PATH_SEGMENT = Pattern.compile("(?<=/)[^/]*@[^/]*(?=/|$)");
    private static final int MAX_CAUSES = 8;
    private static final int MAX_FRAMES = 12;

    private ErrorTrackingSanitizer() {
    }

    static SanitizedError sanitize(ErrorReport report, int maxLength) {
        Throwable cause = report.cause();
        String exceptionType = cause == null ? report.category() : cause.getClass().getName();
        String stack = cause == null ? "" : safeStack(cause, maxLength);
        String sqlState = "";
        int databaseErrorCode = 0;
        SQLException sqlException = findSqlException(cause);
        if (sqlException != null) {
            sqlState = bounded(sqlException.getSQLState(), 16);
            databaseErrorCode = sqlException.getErrorCode();
        }

        String path = safePath(report.path(), maxLength);
        String category = safeToken(report.category(), maxLength, "UNCLASSIFIED");
        String errorCode = safeToken(report.errorCode(), 128, "");
        String release = safeToken(report.release(), 128, "");
        String locale = safeToken(report.locale(), 32, "");
        String correlationId = safeToken(report.correlationId(), 64, "");
        String fingerprintInput = Stream.of(
                        report.source().name(),
                        category,
                        errorCode,
                        exceptionType,
                        stack.lines().limit(5).collect(Collectors.joining("\n")))
                .collect(Collectors.joining("|"));

        return new SanitizedError(
                fingerprint(fingerprintInput),
                report.source(),
                category,
                errorCode,
                bounded(exceptionType, maxLength),
                stack,
                path,
                release,
                locale,
                correlationId,
                report.source() == Source.DATABASE ? "HIGH" : severity(cause),
                sqlState,
                databaseErrorCode);
    }

    static String safePath(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String path = input.split("[?#]", 2)[0];
        path = EMAIL_PATH_SEGMENT.matcher(path).replaceAll("{redacted}");
        path = UUID.matcher(path).replaceAll("{id}");
        path = NUMERIC_SEGMENT.matcher(path).replaceAll("{id}");
        path = EMAIL.matcher(path).replaceAll("[redacted]");
        path = CREDENTIAL.matcher(path).replaceAll("$1=[redacted]");
        path = path.replaceAll("[\\r\\n\\t]", "");
        return bounded(path, maxLength);
    }

    private static String safeStack(Throwable cause, int maxLength) {
        StringBuilder safe = new StringBuilder();
        Throwable current = cause;
        int causes = 0;
        while (current != null && causes++ < MAX_CAUSES && safe.length() < maxLength) {
            if (safe.length() > 0) {
                safe.append(" <- ");
            }
            safe.append(current.getClass().getName());
            StackTraceElement[] frames = current.getStackTrace();
            for (int index = 0; index < Math.min(frames.length, MAX_FRAMES) && safe.length() < maxLength; index++) {
                StackTraceElement frame = frames[index];
                safe.append("\n at ").append(frame.getClassName()).append('.').append(frame.getMethodName());
            }
            current = current.getCause();
        }
        return bounded(safe.toString(), maxLength);
    }

    private static SQLException findSqlException(Throwable cause) {
        Throwable current = cause;
        int causes = 0;
        while (current != null && causes++ < MAX_CAUSES) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }

    private static String severity(Throwable cause) {
        if (cause == null) {
            return "MEDIUM";
        }
        String name = cause.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (name.contains("validation")
                || name.contains("notfound")
                || name.contains("noresourcefound")
                || name.contains("nohandlerfound")
                || name.contains("badrequest")
                || name.contains("methodargument")
                || name.contains("missingservletrequestparameter")) {
            return "LOW";
        }
        if (name.contains("accessdenied") || name.contains("authorization")) {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private static String safeToken(String input, int maxLength, String fallback) {
        String value = input == null ? "" : input.replaceAll("[^A-Za-z0-9_.:/-]", "");
        return bounded(value.isBlank() ? fallback : value, maxLength);
    }

    private static String bounded(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String fingerprint(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    record SanitizedError(
            String fingerprint,
            Source source,
            String category,
            String errorCode,
            String exceptionType,
            String sanitizedStack,
            String path,
            String release,
            String locale,
            String correlationId,
            String severity,
            String sqlState,
            int databaseErrorCode) {
    }
}
