package com.simbio.core.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.MessageAuthorType;
import com.simbio.core.messaging.api.MessageVisibility;

class AiReplyDraftTest {

    @Test
    void draftStaysPendingAndDoesNotExposeBodyInItsStringRepresentation() {
        String syntheticBody = "Synthetic confidential draft";
        AiReplyDraft draft = AiReplyDraft.pendingApproval(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), syntheticBody);

        assertThat(draft.status()).isEqualTo(AiDraftStatus.PENDING_APPROVAL);
        assertThat(draft.body()).isEqualTo(syntheticBody);
        assertThat(draft.toString()).doesNotContain(syntheticBody);
    }

    @Test
    void rejectsBlankOrOversizedDraftBody() {
        assertThatThrownBy(() -> AiReplyDraft.pendingApproval(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AiReplyDraft.pendingApproval(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "x".repeat(10_001)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aiAgentMessageMustRemainInternalAndHaveNoUserIdentity() {
        var conversation = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, Instant.now());

        MessageEntity draft = new MessageEntity(
                UUID.randomUUID(), conversation, null, MessageAuthorType.AI_AGENT,
                MessageVisibility.INTERNAL, "Synthetic draft", Instant.now());

        assertThat(draft.getAuthorId()).isNull();
        assertThat(draft.getVisibility()).isEqualTo(MessageVisibility.INTERNAL);
        assertThatThrownBy(() -> new MessageEntity(
                UUID.randomUUID(), conversation, null, MessageAuthorType.AI_AGENT,
                MessageVisibility.PUBLIC, "Synthetic draft", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MessageEntity(
                UUID.randomUUID(), conversation, UUID.randomUUID(), MessageAuthorType.AI_AGENT,
                MessageVisibility.INTERNAL, "Synthetic draft", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
