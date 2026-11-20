package com.simbio.core.request.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.request.api.QuoteExpired;
import com.simbio.core.request.domain.QuoteEntity;
import com.simbio.core.request.domain.QuoteVersionEntity;

@Component
@ConditionalOnProperty(name = "portal.request.quote-expiry.enabled", havingValue = "true", matchIfMissing = true)
public class QuoteExpiryJob {
    private static final int BATCH_SIZE = 100;
    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final RequestStore requests;
    private final QuoteStore quotes;
    private final AuditRecorder audit;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public QuoteExpiryJob(RequestStore requests, QuoteStore quotes, AuditRecorder audit,
            ApplicationEventPublisher events) {
        this(requests, quotes, audit, events, Clock.systemUTC());
    }

    QuoteExpiryJob(RequestStore requests, QuoteStore quotes, AuditRecorder audit,
            ApplicationEventPublisher events, Clock clock) {
        this.requests = Objects.requireNonNull(requests);
        this.quotes = Objects.requireNonNull(quotes);
        this.audit = Objects.requireNonNull(audit);
        this.events = Objects.requireNonNull(events);
        this.clock = Objects.requireNonNull(clock);
    }

    @Scheduled(cron = "${portal.request.quote-expiry.cron:0 0 * * * *}")
    @Transactional
    public int expireBatch() {
        Instant now = Instant.now(clock);
        int expired = 0;
        for (QuoteVersionEntity version : quotes.findSentExpired(now, PageRequest.of(0, BATCH_SIZE))) {
            if (!version.expire(now)) continue;
            QuoteEntity quote = quotes.findById(version.getQuoteId()).orElseThrow();
            var request = requests.findById(quote.getRequestId()).orElseThrow();
            quotes.saveVersion(version);
            audit.record(new AuditRecord(SYSTEM_ACTOR, "QUOTE_EXPIRED", "QUOTE", quote.getPublicId(),
                    Integer.toString(version.getVersionNumber())));
            Object event = new QuoteExpired(request.getPublicId(), quote.getPublicId(),
                    version.getVersionNumber());
            publishAfterCommit(event);
            expired += 1;
        }
        return expired;
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { events.publishEvent(event); }
        });
    }
}
