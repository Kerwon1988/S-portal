package com.simbio.core.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.api.ConversationStatus;

class ConversationEntityTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-20T08:00:00Z");

    @Test
    void initializesRequestConversationAndExposesItsStateWithoutOwnerDataInString() {
        UUID publicId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        ConversationEntity conversation = new ConversationEntity(
                publicId, ConversationContextType.REQUEST, requestId, ownerId, CREATED_AT);

        assertThat(conversation.getId()).isNull();
        assertThat(conversation.getPublicId()).isEqualTo(publicId);
        assertThat(conversation.getContextType()).isEqualTo(ConversationContextType.REQUEST);
        assertThat(conversation.getContextPublicId()).isEqualTo(requestId);
        assertThat(conversation.getOwnerUserId()).isEqualTo(ownerId);
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.OPEN);
        assertThat(conversation.getAssignedTo()).isNull();
        assertThat(conversation.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(conversation.getVersion()).isZero();
        assertThat(conversation.toString()).contains(publicId.toString(), "REQUEST", "OPEN")
                .doesNotContain(requestId.toString(), ownerId.toString());
    }

    @Test
    void assignsUnassignsAndClosesConversation() {
        ConversationEntity conversation = contactConversation();
        UUID assigneeId = UUID.randomUUID();

        conversation.assignTo(assigneeId);
        assertThat(conversation.getAssignedTo()).isEqualTo(assigneeId);
        conversation.unassign();
        assertThat(conversation.getAssignedTo()).isNull();
        conversation.close();

        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.CLOSED);
        assertThat(conversation.toString()).contains("CLOSED");
        assertThatThrownBy(() -> conversation.assignTo(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void contactConversationAllowsNoOwnerButRequiresContextReference() {
        ConversationEntity conversation = new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, CREATED_AT);

        assertThat(conversation.getContextType()).isEqualTo(ConversationContextType.CONTACT);
        assertThat(conversation.getOwnerUserId()).isNull();
        assertThatThrownBy(() -> new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, null, null, CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("context reference");
    }

    @Test
    void requestConversationRequiresBothRequestReferenceAndOwner() {
        assertThatThrownBy(() -> new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.REQUEST, null, UUID.randomUUID(), CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request and owner");
        assertThatThrownBy(() -> new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.REQUEST, UUID.randomUUID(), null, CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request and owner");
    }

    @Test
    void requiresPublicIdContextTypeAndCreationTime() {
        UUID contextId = UUID.randomUUID();
        assertThatThrownBy(() -> new ConversationEntity(
                null, ConversationContextType.CONTACT, contextId, null, CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("publicId");
        assertThatThrownBy(() -> new ConversationEntity(
                UUID.randomUUID(), null, contextId, null, CREATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("contextType");
        assertThatThrownBy(() -> new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, contextId, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("createdAt");
    }

    private static ConversationEntity contactConversation() {
        return new ConversationEntity(
                UUID.randomUUID(), ConversationContextType.CONTACT, UUID.randomUUID(), null, CREATED_AT);
    }
}
