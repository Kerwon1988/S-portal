package com.simbio.core.messaging.application;

import java.io.InputStream;

public record ContactAttachmentDownload(String contentType, long sizeBytes, InputStream content) {
}
