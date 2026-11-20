package com.simbio.core.messaging.application;

import java.util.Optional;

import com.simbio.core.messaging.api.MessageReceived;
import com.simbio.core.messaging.domain.AiReplyDraft;

public interface AiReplyAssistant {

    Optional<AiReplyDraft> proposeReply(MessageReceived receivedMessage);
}
