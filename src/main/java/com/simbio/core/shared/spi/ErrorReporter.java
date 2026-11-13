package com.simbio.core.shared.spi;

import java.util.Objects;

public interface ErrorReporter {

    void report(ErrorReport report);

    enum Source {
        BACKEND,
        FRONTEND,
        DATABASE
    }

    record ErrorReport(
            Source source,
            String category,
            String errorCode,
            Throwable cause,
            String path,
            String release,
            String locale,
            String correlationId) {

        public ErrorReport {
            Objects.requireNonNull(source, "source must not be null");
            category = category == null ? "UNCLASSIFIED" : category;
            errorCode = errorCode == null ? "" : errorCode;
            path = path == null ? "" : path;
            release = release == null ? "" : release;
            locale = locale == null ? "" : locale;
            correlationId = correlationId == null ? "" : correlationId;
        }
    }
}
