package com.simbio.core.simbiosphere.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.simbiosphere.api.ProvisioningStatus;

class ProvisioningEntityTest {
    private static final Instant NOW = Instant.parse("2026-11-20T18:00:00Z");

    @Test
    void startsPendingAndRecoversThroughExplicitStates() {
        ProvisioningEntity provisioning = provisioning();

        assertThat(provisioning.getStatus()).isEqualTo(ProvisioningStatus.PENDING);
        assertThat(provisioning.markReady(NOW)).isTrue();
        provisioning.markProcessing("worker-a", NOW, NOW.plusSeconds(60));
        provisioning.retry("TIMEOUT", NOW.plusSeconds(30), NOW);
        assertThat(provisioning.getStatus()).isEqualTo(ProvisioningStatus.READY);
        assertThat(provisioning.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(provisioning.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void rejectsRemoteCompletionWhileStillPending() {
        ProvisioningEntity provisioning = provisioning();

        assertThatThrownBy(() -> provisioning.complete("external-account", NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void neverCreatesASecondOwnerLinkFromEmailOrMissingStableIds() {
        assertThatThrownBy(() -> new AccountLinkEntity(UUID.randomUUID(),
                UUID.randomUUID(), "SIMBIOSPHERE", " ", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalAccountId");
        assertThat(provisioning().getClientAccountPublicId()).isNotNull();
    }

    private static ProvisioningEntity provisioning() {
        return new ProvisioningEntity(UUID.randomUUID(), UUID.randomUUID(), "SIMBIOSPHERE",
                UUID.randomUUID().toString(), NOW);
    }
}
