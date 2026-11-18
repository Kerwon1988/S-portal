package com.simbio.core.notification.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.notification.domain.OutboxMessage;

public interface OutboxStore {

    void save(OutboxMessage message);

    Optional<OutboxMessage> claimNext(Instant now, Instant leaseUntil);

    Optional<OutboxMessage> findById(UUID id);

    int purgePayloadsBefore(Instant cutoff);

    long countByStatus(String status);
}
