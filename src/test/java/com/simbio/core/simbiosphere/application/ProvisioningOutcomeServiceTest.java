package com.simbio.core.simbiosphere.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.simbiosphere.api.ProvisioningStatus;
import com.simbio.core.simbiosphere.domain.AccountLinkEntity;
import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

class ProvisioningOutcomeServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T18:00:00Z");
    private MemoryStore store;
    private ProvisioningOutcomeService service;
    private ApplicationEventPublisher events;

    @BeforeEach
    void setUp() {
        store = new MemoryStore();
        events = Mockito.mock(ApplicationEventPublisher.class);
        service = new ProvisioningOutcomeService(store, events, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void retriesWithApprovedBackoffAndFailsAfterSixAttempts() {
        ProvisioningEntity value = processing();
        store.value = value;
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.technicalFailure(value.getPublicId(), "TIMEOUT");
            assertThat(value.getStatus()).isEqualTo(ProvisioningStatus.READY);
            assertThat(value.getNextAttemptAt()).isBetween(NOW.plusSeconds(27), NOW.plusSeconds(33));

            for (int attempt = 2; attempt <= 6; attempt++) {
                value.markProcessing("worker", NOW, NOW.plusSeconds(60));
                service.technicalFailure(value.getPublicId(), "TIMEOUT");
            }
            assertThat(value.getStatus()).isEqualTo(ProvisioningStatus.FAILED);
        } finally {
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void completesAndPersistsOpaqueAccountAndLinkedBusiness() {
        ProvisioningEntity value = processing();
        store.value = value;
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.complete(value.getPublicId(), new DemoProvisioningClient.OwnerOnboardingResult(
                    "external-account", 10, 20, "synthetic-shop", java.util.List.of(), "PENDING_ACTIVATION"));
            assertThat(value.getStatus()).isEqualTo(ProvisioningStatus.PROVISIONED);
            assertThat(store.link).isNotNull();
            assertThat(store.link.getExternalAccountId()).isEqualTo("external-account");
            assertThat(store.business.getTenantSlug()).isEqualTo("synthetic-shop");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static ProvisioningEntity processing() {
        ProvisioningEntity value = new ProvisioningEntity(UUID.randomUUID(), UUID.randomUUID(),
                "SIMBIOSPHERE", UUID.randomUUID().toString(), NOW);
        value.attachIntent(UUID.randomUUID(), "Synthetic shop", "synthetic-shop", "SERVICES",
                "Main", "main", "[]", "SIM-TERMS-1", NOW);
        value.markReady(NOW);
        value.markProcessing("worker", NOW, NOW.plusSeconds(60));
        return value;
    }

    private static final class MemoryStore implements ProvisioningStore {
        private ProvisioningEntity value;
        private AccountLinkEntity link;
        private LinkedBusinessEntity business;
        @Override public ProvisioningEntity save(ProvisioningEntity input) { value = input; return input; }
        @Override public Optional<ProvisioningEntity> findProvisioning(UUID id) { return Optional.ofNullable(value); }
        @Override public Optional<ProvisioningEntity> findByPublicId(UUID id) { return Optional.ofNullable(value); }
        @Override public Optional<ProvisioningEntity> claimNext(Instant now, String worker, Instant until) { return Optional.empty(); }
        @Override public java.util.List<ProvisioningEntity> search(
                com.simbio.core.simbiosphere.api.ProvisioningStatus status, int limit) {
            return value == null || status != null && value.getStatus() != status
                    ? java.util.List.of() : java.util.List.of(value);
        }
        @Override public AccountLinkEntity save(AccountLinkEntity input) { link = input; return input; }
        @Override public LinkedBusinessEntity save(LinkedBusinessEntity input) { business = input; return input; }
    }
}
