package com.simbio.core.config.errortracking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FrontendErrorRequest(
        @NotBlank @Pattern(regexp = "UNCAUGHT_JS|UNHANDLED_REJECTION|RESOURCE_ERROR|HTMX_NETWORK|HTMX_SERVER_ERROR")
        String eventType,
        @NotBlank @Pattern(regexp = "Error|TypeError|ReferenceError|RangeError|SyntaxError|URIError|EvalError|NetworkError|HttpError|PromiseRejection|ResourceError")
        String errorName,
        @Size(max = 2048) String stack,
        @Size(max = 128) String locale,
        @Pattern(regexp = "(?:[A-Za-z0-9-]{8,64})?") String correlationId,
        @Size(max = 1024) String path) {

    public FrontendErrorRequest {
        stack = stack == null ? "" : stack;
        locale = locale == null ? "" : locale;
        correlationId = correlationId == null ? "" : correlationId;
        path = path == null ? "" : path;
    }
}
