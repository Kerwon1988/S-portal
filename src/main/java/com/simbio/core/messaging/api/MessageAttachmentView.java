package com.simbio.core.messaging.api;

import java.util.UUID;

public record MessageAttachmentView(UUID publicId, String detectedMime, long sizeBytes) {
}
