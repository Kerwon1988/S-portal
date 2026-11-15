package com.simbio.core.identity.infrastructure;

import java.time.Instant;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.application.BootstrapStateStore;
import com.simbio.core.identity.domain.BootstrapStateEntity;

@Repository
class JpaBootstrapStateStore implements BootstrapStateStore {

    private final BootstrapStateJpaRepository repository;

    JpaBootstrapStateStore(BootstrapStateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean wasConsumed() {
        return repository.existsById((byte) 1);
    }

    @Override
    @Transactional
    public void consume(Instant consumedAt) {
        if (!wasConsumed()) {
            repository.save(new BootstrapStateEntity(consumedAt));
        }
    }
}
