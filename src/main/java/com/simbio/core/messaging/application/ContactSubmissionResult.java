package com.simbio.core.messaging.application;

import java.util.UUID;

public record ContactSubmissionResult(UUID conversationPublicId, UUID messagePublicId) {
}
