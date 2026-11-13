package com.simbio.core.config.errortracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;
import com.simbio.core.shared.spi.ErrorReporter.Source;

class ErrorTrackingSanitizerTest {

    @Test
    void fingerprintsAreStableAndDoNotPersistThrowableMessages() {
        IllegalStateException first = new IllegalStateException("user=ana@example.com token=top-secret");
        IllegalStateException second = new IllegalStateException("different private message");
        first.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.simbio.Service", "save", "Service.java", 10)
        });
        second.setStackTrace(first.getStackTrace());

        ErrorReport reportOne = report(first);
        ErrorReport reportTwo = report(second);

        ErrorTrackingSanitizer.SanitizedError sanitizedOne = ErrorTrackingSanitizer.sanitize(reportOne, 2048);
        ErrorTrackingSanitizer.SanitizedError sanitizedTwo = ErrorTrackingSanitizer.sanitize(reportTwo, 2048);

        assertThat(sanitizedOne.fingerprint()).isEqualTo(sanitizedTwo.fingerprint());
        assertThat(sanitizedOne.sanitizedStack())
                .doesNotContain("ana@example.com")
                .doesNotContain("top-secret")
                .doesNotContain("different private message");
    }

    @Test
    void recordsOnlySafeSqlMetadataAndRedactsPrivatePathSegments() {
        SQLException sqlException = new SQLException(
                "duplicate row for ana@example.com password=secret", "23000", 2627);
        ErrorReport report = new ErrorReport(
                Source.DATABASE,
                "PERSISTENCE_FAILURE",
                "SQLException",
                sqlException,
                "/customers/ana@example.com?token=private",
                "release-1",
                "es-ES",
                "corr-12345678");

        ErrorTrackingSanitizer.SanitizedError sanitized = ErrorTrackingSanitizer.sanitize(report, 2048);

        assertThat(sanitized.sqlState()).isEqualTo("23000");
        assertThat(sanitized.databaseErrorCode()).isEqualTo(2627);
        assertThat(sanitized.path()).isEqualTo("/customers/{redacted}");
        assertThat(sanitized.sanitizedStack())
                .doesNotContain("ana@example.com")
                .doesNotContain("secret")
                .doesNotContain("duplicate row");
    }

    private static ErrorReport report(Throwable cause) {
        return new ErrorReport(
                Source.BACKEND,
                "UNHANDLED_REQUEST",
                "",
                cause,
                "/save",
                "",
                "es-ES",
                "");
    }
}
