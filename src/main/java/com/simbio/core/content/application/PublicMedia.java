package com.simbio.core.content.application;

import java.io.InputStream;
import java.util.UUID;

public record PublicMedia(UUID publicId, String detectedMime, InputStream content) {
}
