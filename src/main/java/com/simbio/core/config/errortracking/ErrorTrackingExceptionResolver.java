package com.simbio.core.config.errortracking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.simbio.core.shared.spi.ErrorReporter;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ErrorTrackingExceptionResolver implements HandlerExceptionResolver {

    private final ErrorReporter errorReporter;

    public ErrorTrackingExceptionResolver(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        if ((request.getContextPath() + ErrorTelemetryRequestFilter.ENDPOINT).equals(request.getRequestURI())) {
            return null;
        }
        if (isClientError(exception)) {
            return null;
        }
        ErrorTrackingRequest.report(
                errorReporter,
                request,
                exception,
                ErrorTrackingRequest.sourceFor(exception),
                "MVC_EXCEPTION");
        return null;
    }

    private static boolean isClientError(Throwable exception) {
        Throwable current = exception;
        int depth = 0;
        while (current != null && depth++ < 8) {
            if (current instanceof ErrorResponse errorResponse) {
                HttpStatusCode status = errorResponse.getStatusCode();
                if (status.is4xxClientError()) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
