package com.simbio.core.config.errortracking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorTelemetryRateLimiterTest {

    @Test
    void allowsThirtyInitialReportsAndRejectsTheNextOne() {
        ErrorTelemetryRateLimiter limiter = new ErrorTelemetryRateLimiter();

        for (int request = 0; request < 30; request++) {
            assertThat(limiter.tryAcquire()).isTrue();
        }

        assertThat(limiter.tryAcquire()).isFalse();
    }
}
