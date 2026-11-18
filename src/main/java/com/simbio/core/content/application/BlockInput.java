package com.simbio.core.content.application;

import java.util.UUID;

public record BlockInput(
        String type,
        int position,
        String heading,
        String bodyMarkdown,
        String targetUrl,
        UUID mediaPublicId) {
}
