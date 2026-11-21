package com.simbio.core.simbiosphere.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.identity.api.IdentityOwnerProfile;
import com.simbio.core.identity.api.IdentityOwnerProfileApi;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

class ProvisioningWorkerTest {
    @Test
    void neverCallsRemoteApiWithoutVerifiedOwner() {
        ProvisioningStore store = mock(ProvisioningStore.class);
        IdentityOwnerProfileApi owners = mock(IdentityOwnerProfileApi.class);
        DemoProvisioningClient client = mock(DemoProvisioningClient.class);
        ProvisioningOutcomeService outcomes = mock(ProvisioningOutcomeService.class);
        SimbioSphereWorkerProperties properties = new SimbioSphereWorkerProperties();
        ProvisioningEntity value = processing();
        when(store.findByPublicId(value.getPublicId())).thenReturn(Optional.of(value));
        when(owners.findOwnerProfile(value.getOwnerUserPublicId())).thenReturn(Optional.empty());

        new ProvisioningWorker(mock(ProvisioningClaimService.class), store, owners, client, outcomes,
                properties).process(value.getPublicId());

        verify(client, never()).provision(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(outcomes).technicalFailure(value.getPublicId(), "OWNER_VERIFICATION_UNAVAILABLE");
    }

    @Test
    void callsProviderAndCompletesOutsideClaimTransaction() {
        ProvisioningStore store = mock(ProvisioningStore.class);
        IdentityOwnerProfileApi owners = mock(IdentityOwnerProfileApi.class);
        DemoProvisioningClient client = mock(DemoProvisioningClient.class);
        ProvisioningOutcomeService outcomes = mock(ProvisioningOutcomeService.class);
        ProvisioningEntity value = processing();
        UUID owner = value.getOwnerUserPublicId();
        DemoProvisioningClient.OwnerOnboardingResult result =
                new DemoProvisioningClient.OwnerOnboardingResult("external-account", 10, 20,
                        "synthetic-shop", List.of(), "PENDING_ACTIVATION");
        when(store.findByPublicId(value.getPublicId())).thenReturn(Optional.of(value));
        when(owners.findOwnerProfile(owner)).thenReturn(Optional.of(new IdentityOwnerProfile(owner,
                "synthetic@example.test", "Synthetic Owner", "en", Instant.now())));
        when(client.provision(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(value.getIdempotencyKey()))).thenReturn(result);

        new ProvisioningWorker(mock(ProvisioningClaimService.class), store, owners, client, outcomes,
                new SimbioSphereWorkerProperties()).process(value.getPublicId());

        verify(client).provision(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(value.getIdempotencyKey()));
        verify(outcomes).complete(value.getPublicId(), result);
    }

    private static ProvisioningEntity processing() {
        UUID owner = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-20T18:00:00Z");
        ProvisioningEntity value = new ProvisioningEntity(UUID.randomUUID(), UUID.randomUUID(),
                "SIMBIOSPHERE", UUID.randomUUID().toString(), now);
        value.attachIntent(owner, "Synthetic shop", "synthetic-shop", "SERVICES",
                "Main", "main", "[]", "SIM-TERMS-1", now);
        value.markReady(now);
        value.markProcessing("worker", now, now.plusSeconds(60));
        return value;
    }
}
