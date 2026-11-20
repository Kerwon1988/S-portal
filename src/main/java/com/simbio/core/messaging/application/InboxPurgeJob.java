package com.simbio.core.messaging.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.messaging.config.MessagingRetentionProperties;
import com.simbio.core.messaging.domain.ConversationEntity;

@Component
@ConditionalOnProperty(name = "portal.messaging.retention.purge-job-enabled",
        havingValue = "true", matchIfMissing = false)
public class InboxPurgeJob {

    private static final int BATCH_SIZE = 100;
    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final ConversationStore store;
    private final AuditRecorder audit;
    private final MessagingRetentionProperties retention;
    private final Clock clock;

    @Autowired
    public InboxPurgeJob(
            ConversationStore store,
            AuditRecorder audit,
            MessagingRetentionProperties retention) {
        this(store, audit, retention, Clock.systemUTC());
    }

    InboxPurgeJob(
            ConversationStore store,
            AuditRecorder audit,
            MessagingRetentionProperties retention,
            Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.audit = Objects.requireNonNull(audit);
        this.retention = Objects.requireNonNull(retention);
        this.clock = Objects.requireNonNull(clock);
    }

    @Scheduled(cron = "${portal.messaging.retention.purge-cron:0 30 3 * * *}")
    @Transactional
    public int purgeBatch() {
        if (!retention.purgeEnabled()) {
            return 0;
        }
        Instant cutoff = Instant.now(clock).minus(retention.trashGracePeriod());
        int purged = 0;
        for (ConversationEntity conversation : store.findTrashEligibleForPurge(
                cutoff, PageRequest.of(0, BATCH_SIZE))) {
            audit.record(new AuditRecord(SYSTEM_ACTOR, "CONVERSATION_PURGED", "CONVERSATION",
                    conversation.getPublicId(), "RETENTION_JOB"));
            store.deleteMessages(conversation.getId());
            store.deleteConversation(conversation.getId());
            purged += 1;
        }
        return purged;
    }
}
