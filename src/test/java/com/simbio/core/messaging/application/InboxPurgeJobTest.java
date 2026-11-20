package com.simbio.core.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.config.MessagingRetentionProperties;
import com.simbio.core.messaging.domain.ConversationEntity;

class InboxPurgeJobTest {

    private static final Instant NOW = Instant.parse("2026-11-20T18:00:00Z");

    @Test
    void disabledRetentionNeverPurges() {
        ConversationStore store = org.mockito.Mockito.mock(ConversationStore.class);
        InboxPurgeJob job = new InboxPurgeJob(store, org.mockito.Mockito.mock(AuditRecorder.class),
                new MessagingRetentionProperties(Duration.ZERO), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(job.purgeBatch()).isZero();
        verify(store, never()).findTrashEligibleForPurge(any(), any());
    }

    @Test
    void enabledRetentionPurgesEligibleBatchAndAuditsEachConversation() {
        ConversationStore store = org.mockito.Mockito.mock(ConversationStore.class);
        AuditRecorder audit = org.mockito.Mockito.mock(AuditRecorder.class);
        ConversationEntity conversation = new ConversationEntity(UUID.randomUUID(),
                ConversationContextType.CONTACT, UUID.randomUUID(), null, NOW.minus(Duration.ofDays(40)));
        org.springframework.test.util.ReflectionTestUtils.setField(conversation, "id", 44L);
        conversation.moveToTrash(NOW.minus(Duration.ofDays(31)));
        when(store.findTrashEligibleForPurge(NOW.minus(Duration.ofDays(30)),
                Pageable.ofSize(100))).thenReturn(List.of(conversation));
        InboxPurgeJob job = new InboxPurgeJob(store, audit,
                new MessagingRetentionProperties(Duration.ofDays(30)), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(job.purgeBatch()).isEqualTo(1);
        verify(store).deleteMessages(44L);
        verify(store).deleteConversation(44L);
        verify(audit).record(org.mockito.ArgumentMatchers.argThat(record ->
                "CONVERSATION_PURGED".equals(record.actionCode())
                        && "RETENTION_JOB".equals(record.correlationId())));
    }
}
