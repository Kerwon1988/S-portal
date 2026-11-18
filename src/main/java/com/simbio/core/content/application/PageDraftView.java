package com.simbio.core.content.application;

import java.util.List;
import java.util.UUID;

import com.simbio.core.content.domain.ContentPageType;
import com.simbio.core.content.domain.TranslationState;

public record PageDraftView(
        UUID pagePublicId,
        UUID translationPublicId,
        String pageKey,
        ContentPageType pageType,
        String locale,
        String slug,
        String title,
        String seoTitle,
        String metaDescription,
        TranslationState state,
        int version,
        List<BlockInput> blocks) {

    public PageDraftView {
        blocks = List.copyOf(blocks);
    }

    public String pageTypeCode() {
        return pageType.name();
    }

    public boolean isPublished() {
        return state == TranslationState.PUBLISHED;
    }
}
