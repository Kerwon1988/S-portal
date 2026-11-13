package com.simbio.core.config.errortracking;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.simbio.core.shared.spi.ErrorReporter;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ErrorTrackingFilter extends OncePerRequestFilter {

    private final ErrorReporter errorReporter;

    public ErrorTrackingFilter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try (ErrorTrackingContext.Scope ignored = ErrorTrackingContext.bind(request)) {
            if (request.getDispatcherType() == DispatcherType.ERROR) {
                Object error = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
                if (error instanceof Throwable throwable) {
                    ErrorTrackingRequest.report(
                            errorReporter, request, throwable, ErrorTrackingRequest.sourceFor(throwable), "SERVLET_ERROR");
                }
            }
            try {
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException exception) {
                ErrorTrackingRequest.report(
                        errorReporter,
                        request,
                        exception,
                        ErrorTrackingRequest.sourceFor(exception),
                        "UNHANDLED_REQUEST");
                throw exception;
            }
        }
    }
}
