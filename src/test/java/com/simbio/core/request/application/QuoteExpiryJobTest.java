package com.simbio.core.request.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.request.api.RequestType;
import com.simbio.core.request.domain.QuoteEntity;
import com.simbio.core.request.domain.QuoteVersionEntity;
import com.simbio.core.request.domain.RequestEntity;

class QuoteExpiryJobTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private final RequestStore requests = org.mockito.Mockito.mock(RequestStore.class);
    private final QuoteStore quotes = org.mockito.Mockito.mock(QuoteStore.class);
    private final AuditRecorder audit = org.mockito.Mockito.mock(AuditRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final QuoteExpiryJob job = new QuoteExpiryJob(requests, quotes, audit, events,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach void transaction() { TransactionSynchronizationManager.initSynchronization(); }
    @AfterEach void cleanup() { TransactionSynchronizationManager.clearSynchronization(); }

    @Test
    void expiresSentQuoteOnceAndPublishesAfterCommit() {
        RequestEntity request = request();
        QuoteEntity quote = quote(request.getId());
        QuoteVersionEntity version = version(quote.getId());
        version.send(NOW.minusSeconds(172800));
        when(quotes.findSentExpired(any(), any())).thenReturn(List.of(version));
        when(quotes.findById(quote.getId())).thenReturn(Optional.of(quote));
        when(requests.findById(request.getId())).thenReturn(Optional.of(request));

        int expired = job.expireBatch();

        assertThat(expired).isEqualTo(1);
        assertThat(version.getStatus()).isEqualTo(com.simbio.core.request.api.QuoteStatus.EXPIRED);
        verify(events, never()).publishEvent(any(Object.class));
    }

    private static RequestEntity request() {
        var value = new RequestEntity(UUID.randomUUID(), "SOL-2026-000001", RequestType.CUSTOM_DEVELOPMENT,
                "WEB_APPLICATION", "Synthetic", "Synthetic", null, null, null, null, null,
                UUID.randomUUID(), UUID.randomUUID(), NOW.minusSeconds(86400));
        setId(value, 7L); return value;
    }
    private static QuoteEntity quote(Long requestId) { var q = new QuoteEntity(UUID.randomUUID(), requestId, NOW); setId(q, 9L); return q; }
    private static QuoteVersionEntity version(Long quoteId) {
        return new QuoteVersionEntity(quoteId, 1, BigDecimal.TEN, "EUR", BigDecimal.ZERO,
                LocalDate.of(2026, 11, 19), UUID.randomUUID(), NOW.minusSeconds(259200));
    }
    private static void setId(Object object, long id) {
        try { var f = object.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(object, id); }
        catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
    }
}
