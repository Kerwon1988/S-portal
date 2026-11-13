package com.simbio.core.config.errortracking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.simbio.core.shared.spi.ErrorReporter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
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
        ErrorTrackingRequest.report(
                errorReporter,
                request,
                exception,
                ErrorTrackingRequest.sourceFor(exception),
                "MVC_EXCEPTION");
        return null;
    }
}
