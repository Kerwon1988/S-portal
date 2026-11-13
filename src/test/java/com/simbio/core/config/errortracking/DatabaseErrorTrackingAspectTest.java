package com.simbio.core.config.errortracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.simbio.core.shared.spi.ErrorReporter;
import com.simbio.core.shared.spi.ErrorReporter.ErrorReport;
import com.simbio.core.shared.spi.ErrorReporter.Source;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DatabaseErrorTrackingAspectTest {

    @Test
    void preservesSqlExceptionAndPreventsDuplicateCaptureByMvcResolver() throws Throwable {
        List<ErrorReport> reports = new ArrayList<>();
        ErrorReporter reporter = reports::add;
        DatabaseErrorTrackingAspect aspect = new DatabaseErrorTrackingAspect(reporter);
        ErrorTrackingExceptionResolver resolver = new ErrorTrackingExceptionResolver(reporter);
        MockHttpServletRequest request = new MockHttpServletRequest();
        SQLException sqlException = new SQLException("row value should not be stored", "23000", 2627);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenThrow(sqlException);

        try (ErrorTrackingContext.Scope ignored = ErrorTrackingContext.bind(request)) {
            assertThatThrownBy(() -> aspect.trackDatabaseErrors(joinPoint))
                    .isSameAs(sqlException);
            resolver.resolveException(request, new MockHttpServletResponse(), null, sqlException);
        }

        assertThat(reports).singleElement().satisfies(report -> {
            assertThat(report.source()).isEqualTo(Source.DATABASE);
            assertThat(report.cause()).isSameAs(sqlException);
        });
    }
}
