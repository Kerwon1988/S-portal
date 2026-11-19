package com.simbio.core.messaging.api;

import java.util.Optional;
import java.util.UUID;

import com.simbio.core.shared.security.AuthenticatedUser;

public interface ConversationApi {

    ConversationView openRequestConversation(AuthenticatedUser actor, UUID requestPublicId);

    Optional<ConversationView> findForActor(AuthenticatedUser actor, UUID conversationPublicId);

    MessageView postMessage(
            AuthenticatedUser actor,
            UUID conversationPublicId,
            MessageVisibility visibility,
            String body);
}
