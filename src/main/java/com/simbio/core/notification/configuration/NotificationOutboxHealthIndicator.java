package com.simbio.core.notification.configuration;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.simbio.core.notification.application.OutboxStore;

@Component("notificationOutbox")
public class NotificationOutboxHealthIndicator implements HealthIndicator {

    private final OutboxStore store;

    public NotificationOutboxHealthIndicator(OutboxStore store) {
        this.store = store;
    }

    @Override
    public Health health() {
        long pending = store.countByStatus("PENDING");
        long failed = store.countByStatus("FAILED");
        if (failed > 0) {
            return Health.down().withDetail("pending", pending).withDetail("failed", failed).build();
        }
        return Health.up().withDetail("pending", pending).withDetail("failed", 0).build();
    }
}
