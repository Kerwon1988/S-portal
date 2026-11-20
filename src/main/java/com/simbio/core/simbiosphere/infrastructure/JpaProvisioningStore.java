package com.simbio.core.simbiosphere.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.simbio.core.simbiosphere.application.ProvisioningStore;
import com.simbio.core.simbiosphere.domain.AccountLinkEntity;
import com.simbio.core.simbiosphere.domain.LinkedBusinessEntity;
import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

@Repository
class JpaProvisioningStore implements ProvisioningStore {
    private final ProvisioningJpaRepository provisioning;
    private final AccountLinkJpaRepository accountLinks;
    private final LinkedBusinessJpaRepository linkedBusinesses;

    JpaProvisioningStore(ProvisioningJpaRepository provisioning,
            AccountLinkJpaRepository accountLinks,
            LinkedBusinessJpaRepository linkedBusinesses) {
        this.provisioning = provisioning;
        this.accountLinks = accountLinks;
        this.linkedBusinesses = linkedBusinesses;
    }

    @Override public ProvisioningEntity save(ProvisioningEntity value) { return provisioning.save(value); }
    @Override public Optional<ProvisioningEntity> findProvisioning(UUID id) {
        return provisioning.findByClientAccountPublicId(id);
    }
    @Override public Optional<ProvisioningEntity> findByPublicId(UUID id) {
        return provisioning.findByPublicId(id);
    }
    @Override public Optional<ProvisioningEntity> claimNext(Instant now, String worker, Instant leaseUntil) {
        return provisioning.findClaimable(now, org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst().map(value -> {
                    value.markProcessing(worker, now, leaseUntil);
                    return provisioning.save(value);
                });
    }
    @Override public AccountLinkEntity save(AccountLinkEntity value) { return accountLinks.save(value); }
    @Override public LinkedBusinessEntity save(LinkedBusinessEntity value) { return linkedBusinesses.save(value); }
}
