package com.simbio.core.messaging.api;

import java.util.Optional;
import java.util.UUID;

public interface MessageNotificationApi {

    Optional<MessageNotificationTarget> notificationTarget(UUID conversationPublicId);
}
