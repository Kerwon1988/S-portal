package com.simbio.core.config.errortracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.simbio.core.shared.spi.ErrorReporter;
import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;
import com.simbio.core.shared.spi.ErrorReporter.Source;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ErrorTrackingExceptionResolverTest {

    @Test
    void capturesDatabaseExceptionOnceAndLeavesMvcResolutionAvailable() {
        List<ErrorReport> reports = new ArrayList<>();
        ErrorReporter reporter = reports::add;
        ErrorTrackingExceptionResolver resolver = new ErrorTrackingExceptionResolver(reporter);
        MockHttpServletRequest request = new MockHttpServletRequest();
        SQLException exception = new SQLException("database unavailable", "08001", 10054);

        Object firstResult = resolver.resolveException(request, new MockHttpServletResponse(), null, exception);
        Object secondResult = resolver.resolveException(request, new MockHttpServletResponse(), null, exception);

        assertThat(firstResult).isNull();
        assertThat(secondResult).isNull();
        assertThat(reports).singleElement().satisfies(report -> {
            assertThat(report.source()).isEqualTo(Source.DATABASE);
            assertThat(report.cause()).isSameAs(exception);
        });
    }

    @Test
    void trackerPersistenceFailureDoesNotReplaceTheMvcException() {
        ErrorReporter unavailableTracker = report -> {
            throw new ErrorInventoryPersistenceException("storage unavailable");
        };
        ErrorTrackingExceptionResolver resolver = new ErrorTrackingExceptionResolver(unavailableTracker);
        MockHttpServletRequest request = new MockHttpServletRequest();

        Object result = resolver.resolveException(
                request,
                new MockHttpServletResponse(),
                null,
                new IllegalStateException("original failure"));

        assertThat(result).isNull();
    }

    @Test
    void doesNotTrackRejectedTelemetryRequestsAsBackendErrors() {
        List<ErrorReport> reports = new ArrayList<>();
        ErrorTrackingExceptionResolver resolver = new ErrorTrackingExceptionResolver(reports::add);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", ErrorTelemetryRequestFilter.ENDPOINT);
        request.setRequestURI(ErrorTelemetryRequestFilter.ENDPOINT);

        resolver.resolveException(
                request,
                new MockHttpServletResponse(),
                null,
                new IllegalArgumentException("invalid telemetry payload"));

        assertThat(reports).isEmpty();
    }
}
