package com.simbio.core.config.errortracking;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

final class ErrorTrackingContext {

    private static final String REPORTED_ATTRIBUTE = ErrorTrackingContext.class.getName() + ".reported";
    private static final ThreadLocal<HttpServletRequest> CURRENT_REQUEST = new ThreadLocal<>();

    private ErrorTrackingContext() {
    }

    static Scope bind(HttpServletRequest request) {
        HttpServletRequest previous = CURRENT_REQUEST.get();
        CURRENT_REQUEST.set(request);
        return () -> {
            if (previous == null) {
                CURRENT_REQUEST.remove();
            } else {
                CURRENT_REQUEST.set(previous);
            }
        };
    }

    static HttpServletRequest currentRequest() {
        return CURRENT_REQUEST.get();
    }

    @SuppressWarnings("unchecked")
    static boolean markReported(HttpServletRequest request, Throwable throwable) {
        if (request == null || throwable == null) {
            return true;
        }
        Object attribute = request.getAttribute(REPORTED_ATTRIBUTE);
        if (attribute instanceof ReportedExceptions reported) {
            return markCauseChain(reported.exceptions(), throwable);
        }
        Set<Throwable> reported = Collections.newSetFromMap(new IdentityHashMap<>());
        request.setAttribute(REPORTED_ATTRIBUTE, new ReportedExceptions(reported));
        return markCauseChain(reported, throwable);
    }

    private static boolean markCauseChain(Set<Throwable> reported, Throwable throwable) {
        Set<Throwable> chain = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 8 && chain.add(current)) {
            current = current.getCause();
        }
        if (chain.stream().anyMatch(reported::contains)) {
            return false;
        }
        reported.addAll(chain);
        return true;
    }

    private record ReportedExceptions(Set<Throwable> exceptions) {
    }

    @FunctionalInterface
    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
