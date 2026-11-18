package com.simbio.core.content.application;

import java.util.List;
import java.util.UUID;

public record SavePageCommand(
        UUID pagePublicId,
        String pageKey,
        String pageType,
        String locale,
        String slug,
        String title,
        String seoTitle,
        String metaDescription,
        List<BlockInput> blocks) {

    public SavePageCommand {
        blocks = List.copyOf(blocks);
    }
}
