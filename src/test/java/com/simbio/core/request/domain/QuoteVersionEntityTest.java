package com.simbio.core.request.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.request.api.QuoteStatus;

class QuoteVersionEntityTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Test
    void preservesVersionEvidenceAndComputesTax() {
        var version = quote(BigDecimal.valueOf(100));

        version.send(NOW);
        version.decide(QuoteStatus.ACCEPTED, UUID.randomUUID(), NOW.plusSeconds(60));

        assertThat(version.getTaxAmount()).isEqualByComparingTo("21.0000");
        assertThat(version.getGrossAmount()).isEqualByComparingTo("121.0000");
        assertThat(version.getStatus()).isEqualTo(QuoteStatus.ACCEPTED);
        assertThatThrownBy(() -> version.send(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidSendAndIdempotentExpiry() {
        var version = new QuoteVersionEntity(1L, 1, BigDecimal.TEN, "EUR", null,
                LocalDate.of(2026, 11, 19), UUID.randomUUID(), NOW);

        assertThatThrownBy(() -> version.send(NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThat(version.getStatus()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(version.expire(NOW)).isFalse();
    }

    @Test
    void sentQuoteCannotBeDecidedTwice() {
        var version = quote(BigDecimal.ONE);
        version.send(NOW);
        version.decide(QuoteStatus.REJECTED, UUID.randomUUID(), NOW);

        assertThatThrownBy(() -> version.decide(QuoteStatus.ACCEPTED, UUID.randomUUID(), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    private static QuoteVersionEntity quote(BigDecimal amount) {
        return new QuoteVersionEntity(1L, 1, amount, "EUR", new BigDecimal("21.0000"),
                LocalDate.of(2026, 11, 30), UUID.randomUUID(), NOW);
    }
}
