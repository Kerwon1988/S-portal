package com.simbio.core.files.api;

import java.util.UUID;

public record StoredFile(UUID publicId, String detectedMime, long sizeBytes, ScanState scanState) {
}
