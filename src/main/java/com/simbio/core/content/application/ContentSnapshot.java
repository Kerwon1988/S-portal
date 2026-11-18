package com.simbio.core.content.application;

import java.util.List;

import com.simbio.core.content.domain.ContentBlockType;

public record ContentSnapshot(String slug, String title, String seoTitle, String metaDescription,
        boolean approved, List<BlockSnapshot> blocks) {

    public ContentSnapshot {
        blocks = List.copyOf(blocks);
    }

    public static ContentSnapshot from(PageDraftView page) {
        return from(page, false);
    }

    public static ContentSnapshot from(PageDraftView page, boolean approved) {
        return new ContentSnapshot(page.slug(), page.title(), page.seoTitle(), page.metaDescription(), approved,
                page.blocks().stream()
                        .map(block -> new BlockSnapshot(ContentBlockType.valueOf(block.type()),
                                block.position(), block.heading(),
                                block.bodyMarkdown(), block.targetUrl(), block.mediaPublicId()))
                        .toList());
    }

    public record BlockSnapshot(ContentBlockType type, int position, String heading, String bodyMarkdown,
            String targetUrl, java.util.UUID mediaPublicId) {
    }
}
