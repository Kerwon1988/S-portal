package com.simbio.core.messaging.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.messaging.api.ConversationContextType;
import com.simbio.core.messaging.domain.ConversationEntity;
import com.simbio.core.messaging.domain.MessageEntity;

public interface ConversationStore {

    ConversationEntity saveConversation(ConversationEntity conversation);

    Optional<ConversationEntity> findConversation(UUID publicId);

    Optional<ConversationEntity> findByContext(ConversationContextType contextType, UUID contextPublicId);

    MessageEntity saveMessage(MessageEntity message);

    List<MessageEntity> findMessages(Long conversationId, boolean includeInternal);
}
