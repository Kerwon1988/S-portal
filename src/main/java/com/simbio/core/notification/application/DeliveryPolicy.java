package com.simbio.core.notification.application;

import java.time.Duration;

public class DeliveryPolicy {

    private final int maximumAttempts;
    private final Duration initialBackoff;
    private final Duration maximumBackoff;

    public DeliveryPolicy(int maximumAttempts, Duration initialBackoff, Duration maximumBackoff) {
        if (maximumAttempts < 1 || initialBackoff == null || initialBackoff.isNegative()
                || initialBackoff.isZero() || maximumBackoff == null
                || maximumBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("Invalid email delivery policy");
        }
        this.maximumAttempts = maximumAttempts;
        this.initialBackoff = initialBackoff;
        this.maximumBackoff = maximumBackoff;
    }

    public boolean hasAttemptsRemaining(int completedAttempts) {
        return completedAttempts < maximumAttempts;
    }

    public Duration backoffAfter(int completedAttempts) {
        if (completedAttempts < 1) {
            throw new IllegalArgumentException("Completed attempts must be positive");
        }
        long multiplier = 1L << Math.min(completedAttempts - 1, 30);
        Duration next;
        try {
            next = initialBackoff.multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            return maximumBackoff;
        }
        return next.compareTo(maximumBackoff) > 0 ? maximumBackoff : next;
    }
}
