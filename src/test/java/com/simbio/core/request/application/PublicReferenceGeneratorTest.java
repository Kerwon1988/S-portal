package com.simbio.core.request.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class PublicReferenceGeneratorTest {
    @Test
    void formatsSixDigitReferenceForUtcBusinessYear() {
        PublicReferenceGenerator generator = new PublicReferenceGenerator(
                year -> 42, Clock.fixed(Instant.parse("2026-12-31T23:59:59Z"), ZoneOffset.UTC));
        assertThat(generator.next()).isEqualTo("SOL-2026-000042");
    }
}
