package com.simbio.core.simbiosphere.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.simbiosphere.application.ProvisioningStore;
import com.simbio.core.simbiosphere.domain.AccountLinkEntity;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaProvisioningStore.class)
class SimbioSpherePersistenceH2IT {
    @Autowired ProvisioningStore store;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void persistsOneProvisioningPerClientAndStartsPending() {
        UUID account = UUID.randomUUID();
        ProvisioningEntity provisioning = new ProvisioningEntity(UUID.randomUUID(), account,
                "SIMBIOSPHERE", UUID.randomUUID().toString(), Instant.parse("2026-11-20T18:00:00Z"));
        store.save(provisioning);
        entityManager.flush();
        entityManager.clear();

        assertThat(store.findProvisioning(account)).get().satisfies(value -> {
            assertThat(value.getStatus().name()).isEqualTo("PENDING");
            assertThat(value.getClientAccountPublicId()).isEqualTo(account);
        });
    }

    @Test
    void enforcesUniqueExternalAccountRegardlessOfEmail() {
        store.save(new AccountLinkEntity(UUID.randomUUID(), UUID.randomUUID(), "SIMBIOSPHERE",
                "demo-account-10", Instant.now()));
        entityManager.flush();

        assertThatThrownBy(() -> {
            store.save(new AccountLinkEntity(UUID.randomUUID(), UUID.randomUUID(), "SIMBIOSPHERE",
                    "demo-account-10", Instant.now()));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
