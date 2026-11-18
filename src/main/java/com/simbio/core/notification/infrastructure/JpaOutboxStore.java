package com.simbio.core.notification.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.notification.application.OutboxStore;
import com.simbio.core.notification.domain.OutboxMessage;

@Repository
class JpaOutboxStore implements OutboxStore {

    private final OutboxJpaRepository messages;

    JpaOutboxStore(OutboxJpaRepository messages) {
        this.messages = messages;
    }

    @Override
    @Transactional
    public void save(OutboxMessage message) {
        messages.save(message);
    }

    @Override
    @Transactional
    public Optional<OutboxMessage> claimNext(Instant now, Instant leaseUntil) {
        return messages.findClaimable(now, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(message -> {
                    message.claimUntil(leaseUntil);
                    return message;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OutboxMessage> findById(UUID id) {
        return messages.findById(id);
    }

    @Override
    @Transactional
    public int purgePayloadsBefore(Instant cutoff) {
        return messages.purgePayloadsBefore(cutoff);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return messages.countByStatus(status);
    }
}
