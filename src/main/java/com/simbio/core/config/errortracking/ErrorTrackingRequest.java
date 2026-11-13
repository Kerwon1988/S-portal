package com.simbio.core.config.errortracking;

import jakarta.servlet.http.HttpServletRequest;

import com.simbio.core.shared.spi.ErrorReporter;
import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;
import com.simbio.core.shared.spi.ErrorReporter.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerMapping;

final class ErrorTrackingRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorTrackingRequest.class);

    private ErrorTrackingRequest() {
    }

    static void report(
            ErrorReporter reporter,
            HttpServletRequest request,
            Throwable cause,
            Source source,
            String category) {
        if (!ErrorTrackingContext.markReported(request, cause)) {
            return;
        }
        Object routePattern = request == null
                ? null
                : request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String path = routePattern instanceof String pattern ? pattern : "/{unmatched}";
        ErrorReport report = new ErrorReport(
                source,
                category,
                cause == null ? "" : cause.getClass().getSimpleName(),
                cause,
                path,
                "",
                request == null ? "" : request.getLocale().toLanguageTag(),
                MDC.get("correlationId"));
        try {
            reporter.report(report);
        } catch (ErrorInventoryPersistenceException exception) {
            LOGGER.error("Error inventory is unavailable; preserving the original application error", exception);
        }
    }

    static Source sourceFor(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 8) {
            if (current instanceof java.sql.SQLException
                    || current.getClass().getName().startsWith("jakarta.persistence.")
                    || current.getClass().getName().startsWith("org.hibernate.")
                    || current.getClass().getName().startsWith("org.springframework.dao.")) {
                return Source.DATABASE;
            }
            current = current.getCause();
        }
        return Source.BACKEND;
    }
}
