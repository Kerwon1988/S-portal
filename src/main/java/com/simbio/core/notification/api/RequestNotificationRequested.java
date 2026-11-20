package com.simbio.core.notification.api;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record RequestNotificationRequested(
        UUID recipientUserId,
        String internalPath,
        EmailTemplate template,
        Locale locale,
        String idempotencyKey) {

    public RequestNotificationRequested {
        Objects.requireNonNull(recipientUserId, "recipientUserId must not be null");
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(locale, "locale must not be null");
        if (internalPath == null || !internalPath.startsWith("/") || internalPath.startsWith("//")
                || internalPath.contains("?") || internalPath.contains("#")) {
            throw new IllegalArgumentException("Internal path must remain token-free and on the portal origin");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 512) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        if (!template.isRequestOperational()) {
            throw new IllegalArgumentException("Only request operational templates are accepted");
        }
    }
}
