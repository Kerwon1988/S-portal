package com.simbio.core.messaging.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portal.messaging.retention")
public record MessagingRetentionProperties(Duration trashGracePeriod) {

    public MessagingRetentionProperties {
        if (trashGracePeriod == null) {
            trashGracePeriod = Duration.ZERO;
        }
        if (trashGracePeriod.isNegative()) {
            throw new IllegalArgumentException("Trash grace period cannot be negative");
        }
    }

    public boolean purgeEnabled() {
        return !trashGracePeriod.isZero();
    }
}
