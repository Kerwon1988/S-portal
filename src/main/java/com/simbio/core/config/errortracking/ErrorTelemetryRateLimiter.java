package com.simbio.core.config.errortracking;

import org.springframework.stereotype.Component;

@Component
public class ErrorTelemetryRateLimiter {

    private static final double CAPACITY = 30;
    private static final double TOKENS_PER_NANOSECOND = 0.5 / 1_000_000_000d;

    private double availableTokens = CAPACITY;
    private long lastRefillNanos = System.nanoTime();

    public synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        long elapsed = Math.max(0, now - lastRefillNanos);
        availableTokens = Math.min(CAPACITY, availableTokens + elapsed * TOKENS_PER_NANOSECOND);
        lastRefillNanos = now;
        if (availableTokens < 1) {
            return false;
        }
        availableTokens -= 1;
        return true;
    }
}
