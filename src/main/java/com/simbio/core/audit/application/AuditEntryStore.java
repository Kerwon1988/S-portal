package com.simbio.core.audit.application;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.simbio.core.audit.domain.AuditEntryEntity;

public interface AuditEntryStore {

    AuditEntryEntity save(AuditEntryEntity entry);

    Page<AuditEntryEntity> findAll(Pageable pageable);

    Page<AuditEntryEntity> search(AuditFilter filter, Pageable pageable);

    List<AuditEntryEntity> findByEntity(String entityType, java.util.UUID entityPublicId, Pageable pageable);

    long deleteBefore(Instant cutoff);
}
