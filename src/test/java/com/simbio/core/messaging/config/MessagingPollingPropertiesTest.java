package com.simbio.core.messaging.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class MessagingPollingPropertiesTest {

    @Test
    void rejectsAggressivePollingAndBackoffShorterThanInterval() {
        assertThatThrownBy(() -> new MessagingPollingProperties(Duration.ofSeconds(9), Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MessagingPollingProperties(Duration.ofMinutes(1), Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
