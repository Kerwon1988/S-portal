package com.simbio.core.config.errortracking;

import jakarta.servlet.http.HttpServletRequest;

import com.simbio.core.shared.spi.ErrorReporter;
import com.simbio.core.shared.spi.ErrorReporter.Source;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DatabaseErrorTrackingAspect {

    private final ErrorReporter errorReporter;

    public DatabaseErrorTrackingAspect(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Around("@within(org.springframework.stereotype.Repository) || execution(* org.springframework.data.repository.Repository+.*(..))")
    public Object trackDatabaseErrors(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable exception) {
            if (ErrorTrackingRequest.sourceFor(exception) == Source.DATABASE) {
                ErrorTrackingRequest.report(
                        errorReporter,
                        currentRequest(),
                        exception,
                        Source.DATABASE,
                        "PERSISTENCE_FAILURE");
            }
            throw exception;
        }
    }

    private static HttpServletRequest currentRequest() {
        return ErrorTrackingContext.currentRequest();
    }
}
