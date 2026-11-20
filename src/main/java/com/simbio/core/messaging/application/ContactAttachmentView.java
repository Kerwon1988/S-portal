package com.simbio.core.messaging.application;

import java.util.UUID;

public record ContactAttachmentView(UUID filePublicId, String contentType, long sizeBytes) {
}
