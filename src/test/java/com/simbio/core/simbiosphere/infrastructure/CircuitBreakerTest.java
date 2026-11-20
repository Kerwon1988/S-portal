package com.simbio.core.simbiosphere.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class CircuitBreakerTest {
    @Test
    void opensAfterThresholdAndClosesAfterSuccess() {
        MutableClock clock = new MutableClock();
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(30), clock);

        assertThat(breaker.allowRequest()).isTrue();
        breaker.recordFailure();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.CLOSED);
        breaker.recordFailure();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(breaker.allowRequest()).isFalse();

        clock.advance(Duration.ofSeconds(31));
        assertThat(breaker.allowRequest()).isTrue();
        breaker.recordSuccess();
        assertThat(breaker.state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-20T18:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration duration) { now = now.plus(duration); }
    }
}
