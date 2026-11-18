package com.simbio.core.files.api;

import java.util.UUID;

public record CleanFileMetadata(UUID publicId, String detectedMime, long sizeBytes) {
}
