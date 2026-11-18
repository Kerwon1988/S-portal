package com.simbio.core.content.application;

import java.util.UUID;

import com.simbio.core.content.api.SanitizedHtml;
import com.simbio.core.content.domain.ContentBlockType;

public record ContentBlockView(
        ContentBlockType type,
        int position,
        String heading,
        SanitizedHtml bodyHtml,
        String targetUrl,
        UUID mediaPublicId,
        String imageUrl,
        String imageSrcSet,
        String imageAlt) {
}
