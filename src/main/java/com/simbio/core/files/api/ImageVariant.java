package com.simbio.core.files.api;

public record ImageVariant(String code, int width, int height, String mime, long sizeBytes) {
}
