package com.simbio.core.simbiosphere.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.simbiosphere.api.ProvisioningStateChanged;
import com.simbio.core.simbiosphere.api.ProvisioningSummary;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

import tools.jackson.databind.ObjectMapper;

@Service
public class ProvisioningService {
    private static final String DEMO = "SIMBIOSPHERE";

    private final ProvisioningStore store;
    private final CustomerDirectory customers;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public ProvisioningService(ProvisioningStore store, CustomerDirectory customers,
            ApplicationEventPublisher events, ObjectMapper objectMapper) {
        this(store, customers, events, objectMapper, Clock.systemUTC());
    }

    ProvisioningService(ProvisioningStore store, CustomerDirectory customers,
            ApplicationEventPublisher events, ObjectMapper objectMapper, Clock clock) {
        this.store = store;
        this.customers = customers;
        this.events = events;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public ProvisioningSummary begin(UUID ownerUserPublicId, ProvisioningIntent intent) {
        CustomerAccountSummary account = requireAccount(ownerUserPublicId);
        Optional<ProvisioningEntity> existing = store.findProvisioning(account.publicId());
        if (existing.isPresent()) {
            return summary(existing.get());
        }
        UUID publicId = UUID.randomUUID();
        ProvisioningEntity provisioning = new ProvisioningEntity(
                publicId, account.publicId(), DEMO, publicId.toString(), Instant.now(clock));
        provisioning.attachIntent(ownerUserPublicId, intent.businessName(), intent.businessSlug(),
                intent.businessType(), intent.locationName(), intent.locationSlug(),
                encodeModules(intent.modules()), intent.termsVersion(), intent.termsAcceptedAt());
        return summary(store.save(provisioning));
    }

    @Transactional
    public boolean markReadyAfterVerification(UUID ownerUserPublicId) {
        Optional<CustomerAccountSummary> account = customers.findOwnedBy(ownerUserPublicId);
        if (account.isEmpty()) {
            return false;
        }
        Optional<ProvisioningEntity> found = store.findProvisioning(account.get().publicId());
        if (found.isEmpty() || found.get().getStatus() != com.simbio.core.simbiosphere.api.ProvisioningStatus.PENDING) {
            return false;
        }
        ProvisioningEntity provisioning = found.get();
        Instant now = Instant.now(clock);
        provisioning.markReady(now);
        store.save(provisioning);
        publishAfterCommit(new ProvisioningStateChanged(provisioning.getPublicId(),
                provisioning.getClientAccountPublicId(), provisioning.getStatus(), now));
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<ProvisioningSummary> ownedBy(UUID ownerUserPublicId) {
        return customers.findOwnedBy(ownerUserPublicId)
                .flatMap(account -> store.findProvisioning(account.publicId()))
                .map(ProvisioningService::summary);
    }

    private CustomerAccountSummary requireAccount(UUID ownerUserPublicId) {
        return customers.findOwnedBy(ownerUserPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Active owner account is required"));
    }

    private String encodeModules(java.util.List<String> modules) {
        try {
            return objectMapper.writeValueAsString(modules);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Modules could not be encoded", exception);
        }
    }

    private void publishAfterCommit(ProvisioningStateChanged event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { events.publishEvent(event); }
            });
        } else {
            events.publishEvent(event);
        }
    }

    private static ProvisioningSummary summary(ProvisioningEntity value) {
        return new ProvisioningSummary(value.getPublicId(), value.getClientAccountPublicId(),
                value.getStatus(), value.getErrorCode(), value.getAttemptCount(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}
