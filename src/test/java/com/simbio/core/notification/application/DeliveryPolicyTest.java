package com.simbio.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class DeliveryPolicyTest {

    @Test
    void usesBoundedExponentialBackoffAndFiniteAttempts() {
        DeliveryPolicy policy = new DeliveryPolicy(4, Duration.ofSeconds(10), Duration.ofSeconds(25));

        assertThat(policy.backoffAfter(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.backoffAfter(2)).isEqualTo(Duration.ofSeconds(20));
        assertThat(policy.backoffAfter(3)).isEqualTo(Duration.ofSeconds(25));
        assertThat(policy.hasAttemptsRemaining(3)).isTrue();
        assertThat(policy.hasAttemptsRemaining(4)).isFalse();
    }

    @Test
    void rejectsInvalidAttemptNumber() {
        DeliveryPolicy policy = new DeliveryPolicy(2, Duration.ofSeconds(1), Duration.ofSeconds(2));

        assertThatThrownBy(() -> policy.backoffAfter(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
