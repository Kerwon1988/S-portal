package com.simbio.core.config.errortracking;

import com.simbio.core.shared.spi.ErrorReporter.Source;

public record ErrorRecord(
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
        String status,
        String sqlState,
        int databaseErrorCode,
        String firstSeen,
        String lastSeen,
        long occurrences) {
}
