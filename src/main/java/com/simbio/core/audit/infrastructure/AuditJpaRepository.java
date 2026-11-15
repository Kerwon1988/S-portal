package com.simbio.core.audit.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.audit.application.AuditFilter;
import com.simbio.core.audit.domain.AuditEntryEntity;

interface AuditJpaRepository extends JpaRepository<AuditEntryEntity, Long> {

    List<AuditEntryEntity> findByEntityTypeAndEntityPublicIdOrderByOccurredAtDesc(
            String entityType, UUID entityPublicId);

    @Query("""
            select entry from AuditEntryEntity entry
            where (:actorId is null or entry.actorId = :actorId)
              and (:actionCode is null or entry.actionCode = :actionCode)
              and (:fromInclusive is null or entry.occurredAt >= :fromInclusive)
              and (:toExclusive is null or entry.occurredAt < :toExclusive)
            """)
    Page<AuditEntryEntity> search(
            @Param("actorId") UUID actorId,
            @Param("actionCode") String actionCode,
            @Param("fromInclusive") java.time.Instant fromInclusive,
            @Param("toExclusive") java.time.Instant toExclusive,
            Pageable pageable);

    Page<AuditEntryEntity> findByEntityTypeAndEntityPublicId(
            String entityType, UUID entityPublicId, Pageable pageable);

    @Modifying
    @Query("delete from AuditEntryEntity entry where entry.occurredAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);

    @Override
    Page<AuditEntryEntity> findAll(Pageable pageable);
}
