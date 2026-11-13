package com.simbio.core.config.errortracking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.Valid;

import com.simbio.core.shared.spi.ErrorReporter;
import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;
import com.simbio.core.shared.spi.ErrorReporter.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorTelemetryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorTelemetryController.class);
    private static final Pattern SAFE_FRAME = Pattern.compile(
            "^at ((?:app|htmx|alpine-csp|theme|menu|htmx-config|error-reporter)\\.[0-9a-f]{16}\\.js):(\\d{1,6}):(\\d{1,6})$");
    private static final int MAX_FRAMES = 12;

    private final ErrorReporter errorReporter;
    private final ErrorTrackingProperties properties;
    private final ErrorTelemetryRateLimiter rateLimiter;

    public ErrorTelemetryController(
            ErrorReporter errorReporter,
            ErrorTrackingProperties properties,
            ErrorTelemetryRateLimiter rateLimiter) {
        this.errorReporter = errorReporter;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping(path = ErrorTelemetryRequestFilter.ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> report(@Valid @RequestBody FrontendErrorRequest request) {
        if (!rateLimiter.tryAcquire()) {
            return ResponseEntity.status(429).header(HttpHeaders.RETRY_AFTER, "2").build();
        }
        try {
            errorReporter.report(new ErrorReport(
                    Source.FRONTEND,
                    request.eventType(),
                    request.errorName(),
                    frontendFailure(request.stack()),
                    request.path(),
                    properties.release(),
                    request.locale(),
                    request.correlationId()));
        } catch (ErrorInventoryPersistenceException exception) {
            LOGGER.warn("Frontend telemetry could not be persisted", exception);
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.noContent().build();
    }

    private static Throwable frontendFailure(String stack) {
        Throwable failure = new FrontendReportedException();
        List<StackTraceElement> frames = new ArrayList<>();
        if (stack != null) {
            for (String line : stack.lines().limit(MAX_FRAMES).toList()) {
                Matcher matcher = SAFE_FRAME.matcher(line);
                if (matcher.matches()) {
                    frames.add(new StackTraceElement(
                            "frontend.assets",
                            matcher.group(1).substring(0, matcher.group(1).length() - 3),
                            "error-reporter.js",
                            Integer.parseInt(matcher.group(2))));
                }
            }
        }
        failure.setStackTrace(frames.toArray(StackTraceElement[]::new));
        return failure;
    }

    private static final class FrontendReportedException extends RuntimeException {

        private FrontendReportedException() {
            super("Frontend error");
        }
    }
}
