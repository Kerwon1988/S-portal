package com.simbio.core.simbiosphere.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class CircuitBreaker implements com.simbio.core.simbiosphere.application.OutboundCircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int threshold;
    private final Duration openDuration;
    private final Clock clock;
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicReference<Instant> reopenAt = new AtomicReference<>();

    public CircuitBreaker(int threshold, Duration openDuration, Clock clock) {
        this.threshold = threshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public boolean allowRequest() {
        Instant until = reopenAt.get();
        if (until == null) return true;
        if (Instant.now(clock).isBefore(until)) return false;
        return reopenAt.compareAndSet(until, null);
    }

    public void recordSuccess() {
        failures.set(0);
        reopenAt.set(null);
    }

    public void recordFailure() {
        if (failures.incrementAndGet() >= threshold) {
            reopenAt.set(Instant.now(clock).plus(openDuration));
        }
    }

    public State state() {
        Instant until = reopenAt.get();
        if (until == null) return State.CLOSED;
        return Instant.now(clock).isBefore(until) ? State.OPEN : State.HALF_OPEN;
    }
}
