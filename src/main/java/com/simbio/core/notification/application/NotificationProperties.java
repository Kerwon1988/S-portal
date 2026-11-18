package com.simbio.core.notification.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "portal.notification")
public class NotificationProperties {

    private Duration retention;
    private int maximumAttempts = 6;
    private Duration initialBackoff = Duration.ofSeconds(30);
    private Duration maximumBackoff = Duration.ofHours(1);
    private Duration claimLease = Duration.ofMinutes(2);
    private Duration pollInterval = Duration.ofSeconds(5);
    private String fromAddress;

    public Duration getRetention() {
        return retention;
    }

    public void setRetention(Duration retention) {
        this.retention = positive(retention, "Outbox retention must be positive");
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public void setMaximumAttempts(int maximumAttempts) {
        if (maximumAttempts < 1) {
            throw new IllegalArgumentException("Maximum delivery attempts must be positive");
        }
        this.maximumAttempts = maximumAttempts;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = positive(initialBackoff, "Initial backoff must be positive");
    }

    public Duration getMaximumBackoff() {
        return maximumBackoff;
    }

    public void setMaximumBackoff(Duration maximumBackoff) {
        this.maximumBackoff = positive(maximumBackoff, "Maximum backoff must be positive");
    }

    public Duration getClaimLease() {
        return claimLease;
    }

    public void setClaimLease(Duration claimLease) {
        this.claimLease = positive(claimLease, "Claim lease must be positive");
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = positive(pollInterval, "Poll interval must be positive");
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    private static Duration positive(Duration duration, String message) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(message);
        }
        return duration;
    }
}
