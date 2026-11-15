package com.simbio.core.audit.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.application.AuditEntryStore;
import com.simbio.core.audit.application.AuditFilter;
import com.simbio.core.audit.domain.AuditEntryEntity;

@Repository
class JpaAuditEntryStore implements AuditEntryStore {

    private final AuditJpaRepository repository;

    JpaAuditEntryStore(AuditJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AuditEntryEntity save(AuditEntryEntity entry) {
        return repository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEntryEntity> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEntryEntity> search(AuditFilter filter, Pageable pageable) {
        return repository.search(filter.actorId(), filter.actionCode(),
                filter.fromInclusive(), filter.toExclusive(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntryEntity> findByEntity(String entityType, UUID entityPublicId, Pageable pageable) {
        return repository.findByEntityTypeAndEntityPublicId(entityType, entityPublicId, pageable).getContent();
    }

    @Override
    @Transactional
    public long deleteBefore(Instant cutoff) {
        return repository.deleteExpired(cutoff);
    }
}
