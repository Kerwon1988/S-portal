package com.simbio.core.content.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simbio.core.content.domain.ContentPageType;

public record PublishedPageView(
        UUID pagePublicId,
        String pageKey,
        ContentPageType pageType,
        String requestedLocale,
        String locale,
        String slug,
        String title,
        String seoTitle,
        String metaDescription,
        String canonicalPath,
        Map<String, String> alternatePaths,
        int version,
        List<ContentBlockView> blocks) {

    public PublishedPageView {
        alternatePaths = Map.copyOf(alternatePaths);
        blocks = List.copyOf(blocks);
    }
}
