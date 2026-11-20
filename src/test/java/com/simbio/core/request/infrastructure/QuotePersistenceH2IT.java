package com.simbio.core.request.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.request.application.QuoteStore;
import com.simbio.core.request.domain.QuoteEntity;
import com.simbio.core.request.domain.QuoteVersionEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaQuoteStore.class)
class QuotePersistenceH2IT {
    @Autowired QuoteStore quotes;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void keepsImmutableSequentialQuoteVersions() {
        QuoteEntity quote = quotes.save(new QuoteEntity(UUID.randomUUID(), 41L, Instant.now()));
        entityManager.flush();
        quotes.saveVersion(new QuoteVersionEntity(quote.getId(), 1, new BigDecimal("50.0000"),
                "EUR", new BigDecimal("21"), LocalDate.of(2026, 12, 31), UUID.randomUUID(), Instant.now()));
        quotes.saveVersion(new QuoteVersionEntity(quote.getId(), 2, new BigDecimal("60.0000"),
                "EUR", new BigDecimal("21"), LocalDate.of(2026, 12, 31), UUID.randomUUID(), Instant.now()));
        entityManager.flush(); entityManager.clear();

        assertThat(quotes.versions(quote.getId())).extracting(QuoteVersionEntity::getVersionNumber)
                .containsExactly(2, 1);
        assertThatThrownBy(() -> {
            quotes.saveVersion(new QuoteVersionEntity(quote.getId(), 2, BigDecimal.ONE, "EUR",
                    BigDecimal.ZERO, LocalDate.of(2026, 12, 31), UUID.randomUUID(), Instant.now()));
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }
}
