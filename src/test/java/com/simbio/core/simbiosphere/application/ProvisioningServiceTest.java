package com.simbio.core.simbiosphere.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.simbiosphere.domain.AccountLinkEntity;
import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

class ProvisioningServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T18:00:00Z");
    private CustomerDirectory customers;
    private InMemoryStore store;
    private ProvisioningService service;
    private UUID owner;
    private UUID account;

    @BeforeEach
    void setUp() {
        customers = mock(CustomerDirectory.class);
        store = new InMemoryStore();
        service = new ProvisioningService(store, customers, mock(ApplicationEventPublisher.class),
                Clock.fixed(NOW, ZoneOffset.UTC));
        owner = UUID.randomUUID();
        account = UUID.randomUUID();
    }

    @Test
    void createsPendingIntentIdempotentlyForStableOwnerAccount() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.of(account()));

        var first = service.begin(owner);
        var replay = service.begin(owner);

        assertThat(first).isEqualTo(replay);
        assertThat(store.provisioning).hasSize(1);
        assertThat(first.clientAccountPublicId()).isEqualTo(account);
        assertThat(first.status().name()).isEqualTo("PENDING");
    }

    @Test
    void rejectsActorWithoutOwnerAccount() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.begin(owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner account");
        assertThat(store.provisioning).isEmpty();
    }

    @Test
    void emailVerificationMakesExistingIntentReadyButDoesNotCallRemoteApi() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.of(account()));
        service.begin(owner);

        assertThat(service.markReadyAfterVerification(owner)).isTrue();
        assertThat(service.markReadyAfterVerification(owner)).isFalse();
        assertThat(service.ownedBy(owner)).get()
                .extracting(value -> value.status().name()).isEqualTo("READY");
    }

    @Test
    void emailVerificationWithoutCustomerIntentIsANoOp() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.empty());

        assertThat(service.markReadyAfterVerification(owner)).isFalse();
    }

    @Test
    void readsOnlyProvisioningForOwnedAccount() {
        UUID foreignOwner = UUID.randomUUID();
        when(customers.findOwnedBy(owner)).thenReturn(Optional.of(account()));
        when(customers.findOwnedBy(foreignOwner)).thenReturn(Optional.empty());
        service.begin(owner);

        assertThat(service.ownedBy(owner)).isPresent();
        assertThat(service.ownedBy(foreignOwner)).isEmpty();
    }

    private CustomerAccountSummary account() {
        return new CustomerAccountSummary(account, "COMPANY", "Synthetic account", "ACTIVE", NOW);
    }

    private static final class InMemoryStore implements ProvisioningStore {
        private final Map<UUID, ProvisioningEntity> provisioning = new HashMap<>();

        @Override public ProvisioningEntity save(ProvisioningEntity value) {
            provisioning.putIfAbsent(value.getClientAccountPublicId(), value);
            return provisioning.get(value.getClientAccountPublicId());
        }
        @Override public Optional<ProvisioningEntity> findProvisioning(UUID id) {
            return Optional.ofNullable(provisioning.get(id));
        }
        @Override public AccountLinkEntity save(AccountLinkEntity value) { return value; }
        @Override public LinkedBusinessEntity save(LinkedBusinessEntity value) { return value; }
    }
}
