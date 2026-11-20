package com.simbio.core.messaging.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portal.messaging.polling")
public record MessagingPollingProperties(Duration interval, Duration errorBackoff) {

    public MessagingPollingProperties {
        interval = requireBounded(interval, Duration.ofSeconds(15), "interval");
        errorBackoff = requireBounded(errorBackoff, Duration.ofSeconds(60), "errorBackoff");
        if (errorBackoff.compareTo(interval) < 0) {
            throw new IllegalArgumentException("Polling error backoff cannot be shorter than interval");
        }
    }

    private static Duration requireBounded(Duration value, Duration fallback, String field) {
        Duration resolved = value == null ? fallback : value;
        if (resolved.compareTo(Duration.ofSeconds(10)) < 0
                || resolved.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("Polling " + field + " must be between 10 seconds and 5 minutes");
        }
        return resolved;
    }
}
