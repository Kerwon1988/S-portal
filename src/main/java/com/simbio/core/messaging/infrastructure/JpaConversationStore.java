package com.simbio.core.messaging.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.simbio.core.messaging.application.ConversationStore;
import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageEntity;

@Repository
class JpaConversationStore implements ConversationStore {

    private final ConversationJpaRepository conversations;
    private final MessageJpaRepository messages;

    JpaConversationStore(
            ConversationJpaRepository conversations,
            MessageJpaRepository messages) {
        this.conversations = conversations;
        this.messages = messages;
    }

    @Override
    public ConversationEntity saveConversation(ConversationEntity conversation) {
        return conversations.save(conversation);
    }

    @Override
    public Optional<ConversationEntity> findConversation(UUID publicId) {
        return conversations.findByPublicId(publicId);
    }

    @Override
    public Optional<ConversationEntity> findByContext(
            ConversationContextType contextType,
            UUID contextPublicId) {
        return conversations.findByContextTypeAndContextPublicId(contextType, contextPublicId);
    }

    @Override
    public MessageEntity saveMessage(MessageEntity message) {
        return messages.save(message);
    }

    @Override
    public List<MessageEntity> findMessages(Long conversationId, boolean includeInternal) {
        return messages.findForConversation(conversationId, includeInternal);
    }
}
