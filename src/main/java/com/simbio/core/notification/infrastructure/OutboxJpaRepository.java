package com.simbio.core.notification.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.simbio.core.notification.domain.OutboxMessage;

interface OutboxJpaRepository extends JpaRepository<OutboxMessage, UUID> {

    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select message from OutboxMessage message
            where (message.status = 'PENDING' and message.availableAt <= :now)
               or (message.status = 'PROCESSING' and message.claimedUntil <= :now)
            order by message.availableAt, message.createdAt
            """)
    List<OutboxMessage> findClaimable(@Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("""
            update OutboxMessage message set message.payloadEncrypted = null
            where message.payloadEncrypted is not null
              and message.status in ('SENT', 'FAILED')
              and message.createdAt < :cutoff
            """)
    int purgePayloadsBefore(@Param("cutoff") Instant cutoff);
}
