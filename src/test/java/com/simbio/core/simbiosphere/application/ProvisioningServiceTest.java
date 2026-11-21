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
import com.simbio.core.identity.api.IdentityOwnerProfile;
import com.simbio.core.identity.api.IdentityOwnerProfileApi;
import com.simbio.core.simbiosphere.domain.AccountLinkEntity;
import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

class ProvisioningServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T18:00:00Z");
    private CustomerDirectory customers;
    private IdentityOwnerProfileApi identities;
    private InMemoryStore store;
    private ProvisioningService service;
    private UUID owner;
    private UUID account;

    @BeforeEach
    void setUp() {
        customers = mock(CustomerDirectory.class);
        identities = mock(IdentityOwnerProfileApi.class);
        store = new InMemoryStore();
        service = new ProvisioningService(store, customers, identities,
                mock(ApplicationEventPublisher.class), new tools.jackson.databind.ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        owner = UUID.randomUUID();
        account = UUID.randomUUID();
        when(identities.findOwnerProfile(owner)).thenReturn(Optional.of(new IdentityOwnerProfile(
                owner, "synthetic@example.test", "Synthetic Owner", "en", NOW)));
    }

    @Test
    void createsPendingIntentIdempotentlyForStableOwnerAccount() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.of(account()));

        var first = service.begin(owner, intent());
        var replay = service.begin(owner, intent());

        assertThat(first).isEqualTo(replay);
        assertThat(store.provisioning).hasSize(1);
        assertThat(first.clientAccountPublicId()).isEqualTo(account);
        assertThat(first.status().name()).isEqualTo("READY");
    }

    @Test
    void rejectsActorWithoutOwnerAccount() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.begin(owner, intent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner account");
        assertThat(store.provisioning).isEmpty();
    }

    @Test
    void emailVerificationMakesExistingIntentReadyButDoesNotCallRemoteApi() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.of(account()));
        ProvisioningEntity value = new ProvisioningEntity(UUID.randomUUID(), account,
                "SIMBIOSPHERE", UUID.randomUUID().toString(), NOW);
        value.attachIntent(owner, "Synthetic shop", "synthetic-shop", "SERVICES",
                "Main", "main", "null", "SIM-TERMS-1", NOW);
        store.save(value);

        assertThat(service.markReadyAfterVerification(owner)).isTrue();
        assertThat(service.markReadyAfterVerification(owner)).isFalse();
        assertThat(service.ownedBy(owner)).get()
                .extracting(summary -> summary.status().name()).isEqualTo("READY");
    }

    @Test
    void ownerCanRotateIdempotencyAfterSlugConflictOnly() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.of(account()));
        service.begin(owner, intent());
        ProvisioningEntity value = store.provisioning.get(account);
        value.markProcessing("worker", NOW, NOW.plusSeconds(60));
        value.actionRequired("SLUG_TAKEN", NOW);
        String originalKey = value.getIdempotencyKey();

        assertThat(service.correctActionRequired(owner, "synthetic-shop-new")).isTrue();
        assertThat(value.getStatus().name()).isEqualTo("READY");
        assertThat(value.getBusinessSlug()).isEqualTo("synthetic-shop-new");
        assertThat(value.getIdempotencyKey()).isNotEqualTo(originalKey);
    }

    @Test
    void onlyProvisionedOwnerCanRequestFreshActivationLink() {
        when(customers.findOwnedBy(owner)).thenReturn(Optional.of(account()));
        var summary = service.begin(owner, intent());
        ProvisioningEntity value = store.provisioning.get(account);
        value.markProcessing("worker", NOW, NOW.plusSeconds(60));
        value.complete("external-account", 10, NOW);
        DemoProvisioningClient client = mock(DemoProvisioningClient.class);
        when(client.createActivationLink(10, summary.publicId() + ":activation"))
                .thenReturn(new DemoProvisioningClient.ActivationLinkResult(
                        "https://orders.simbio.info/activate/synthetic", NOW.plusSeconds(900).toString()));

        assertThat(service.activationLink(owner, client).getHost()).isEqualTo("orders.simbio.info");
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
        service.begin(owner, intent());

        assertThat(service.ownedBy(owner)).isPresent();
        assertThat(service.ownedBy(foreignOwner)).isEmpty();
    }

    private static ProvisioningIntent intent() {
        return new ProvisioningIntent("Synthetic shop", "synthetic-shop", "SERVICES",
                "Main", "main", java.util.List.of(), "SIM-TERMS-1", NOW);
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
        @Override public Optional<ProvisioningEntity> findByPublicId(UUID id) {
            return provisioning.values().stream().filter(value -> id.equals(value.getPublicId())).findFirst();
        }
        @Override public Optional<ProvisioningEntity> claimNext(java.time.Instant now,
                String worker, java.time.Instant leaseUntil) { return Optional.empty(); }
        @Override public java.util.List<ProvisioningEntity> search(
                com.simbio.core.simbiosphere.api.ProvisioningStatus status, int limit) {
            return provisioning.values().stream()
                    .filter(value -> status == null || value.getStatus() == status).limit(limit).toList();
        }
        @Override public AccountLinkEntity save(AccountLinkEntity value) { return value; }
        @Override public LinkedBusinessEntity save(LinkedBusinessEntity value) { return value; }
    }
}
