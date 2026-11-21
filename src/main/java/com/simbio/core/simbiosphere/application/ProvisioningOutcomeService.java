package com.simbio.core.simbiosphere.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.simbiosphere.api.ProvisioningStateChanged;
import com.simbio.core.simbiosphere.domain.AccountLinkEntity;
import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

@Service
public class ProvisioningOutcomeService {
    private static final List<Duration> BACKOFF = List.of(
            Duration.ofSeconds(30), Duration.ofMinutes(2), Duration.ofMinutes(10),
            Duration.ofMinutes(30), Duration.ofHours(2), Duration.ofHours(6));

    private final ProvisioningStore store;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Autowired
    public ProvisioningOutcomeService(ProvisioningStore store, ApplicationEventPublisher events) {
        this(store, events, Clock.systemUTC());
    }

    ProvisioningOutcomeService(ProvisioningStore store, ApplicationEventPublisher events, Clock clock) {
        this.store = store;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void complete(UUID provisioningId, DemoProvisioningClient.OwnerOnboardingResult result) {
        ProvisioningEntity value = require(provisioningId);
        Instant now = Instant.now(clock);
        value.complete(result.externalAccountId(), result.customerAccountId(), now);
        store.save(value);
        AccountLinkEntity link = store.save(new AccountLinkEntity(UUID.randomUUID(),
                value.getClientAccountPublicId(), value.getExternalSystem(), result.externalAccountId(), now));
        store.save(new LinkedBusinessEntity(UUID.randomUUID(), link,
                Long.toString(result.tenantId()), value.getBusinessName(), value.getBusinessType(),
                result.tenantSlug(), now));
        publishAfterCommit(value, now);
    }

    @Transactional
    public void actionRequired(UUID provisioningId, String errorCode) {
        ProvisioningEntity value = require(provisioningId);
        Instant now = Instant.now(clock);
        value.actionRequired(errorCode, now);
        store.save(value);
        publishAfterCommit(value, now);
    }

    @Transactional
    public void technicalFailure(UUID provisioningId, String errorCode) {
        ProvisioningEntity value = require(provisioningId);
        Instant now = Instant.now(clock);
        if (value.getAttemptCount() >= BACKOFF.size()) {
            value.fail(errorCode, now);
        } else {
            value.retry(errorCode, now.plus(retryDelay(value.getAttemptCount() - 1)), now);
        }
        store.save(value);
        publishAfterCommit(value, now);
    }

    private void publishAfterCommit(ProvisioningEntity value, Instant now) {
        ProvisioningStateChanged event = new ProvisioningStateChanged(value.getPublicId(),
                value.getClientAccountPublicId(), value.getStatus(), now);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { events.publishEvent(event); }
        });
    }

    static Duration retryDelay(int index) {
        long baseMillis = BACKOFF.get(index).toMillis();
        long jittered = Math.round(baseMillis * java.util.concurrent.ThreadLocalRandom.current().nextDouble(0.9, 1.1));
        return Duration.ofMillis(jittered);
    }

    private ProvisioningEntity require(UUID publicId) {
        return store.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("Provisioning not found"));
    }
}
