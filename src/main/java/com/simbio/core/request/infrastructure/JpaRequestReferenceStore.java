package com.simbio.core.request.infrastructure;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.request.application.RequestReferenceStore;
import com.simbio.core.request.domain.PublicReferenceCounterEntity;

@Repository
class JpaRequestReferenceStore implements RequestReferenceStore {
    private final ReferenceCounterJpaRepository repository;

    JpaRequestReferenceStore(ReferenceCounterJpaRepository repository) { this.repository = repository; }

    @Override
    @Transactional
    public long next(int year) {
        PublicReferenceCounterEntity counter = repository.findByIdForUpdate(year)
                .orElseGet(() -> repository.saveAndFlush(new PublicReferenceCounterEntity(year)));
        return counter.next();
    }
}
