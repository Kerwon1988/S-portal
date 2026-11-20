package com.simbio.core.simbiosphere.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.simbiosphere.domain.ProvisioningEntity;

@Service
public class ProvisioningClaimService {
    static final int LEASE_SECONDS = 60;
    private final ProvisioningStore store;
    private final Clock clock;

    @Autowired
    public ProvisioningClaimService(ProvisioningStore store) {
        this(store, Clock.systemUTC());
    }

    ProvisioningClaimService(ProvisioningStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Transactional
    public Optional<UUID> claim(String worker) {
        Instant now = Instant.now(clock);
        return store.claimNext(now, worker, now.plusSeconds(LEASE_SECONDS))
                .map(ProvisioningEntity::getPublicId);
    }
}
