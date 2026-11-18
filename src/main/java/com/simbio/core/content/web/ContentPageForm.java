package com.simbio.core.content.web;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simbio.core.content.application.BlockInput;
import com.simbio.core.content.application.PageDraftView;
import com.simbio.core.content.application.SavePageCommand;

public class ContentPageForm {

    private UUID pagePublicId;
    private String pageKey = "";
    private String pageType = "PAGE";
    private String locale = "es";
    private String slug = "";
    private String title = "";
    private String seoTitle = "";
    private String metaDescription = "";
    private List<BlockForm> blocks = initialBlocks();

    public UUID getPagePublicId() {
        return pagePublicId;
    }

    public void setPagePublicId(UUID pagePublicId) {
        this.pagePublicId = pagePublicId;
    }

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public List<BlockForm> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BlockForm> blocks) {
        this.blocks = blocks == null ? new ArrayList<>() : blocks;
    }

    SavePageCommand toCommand() {
        List<BlockInput> inputs = blocks.stream()
                .filter(block -> block.getType() != null && !block.getType().isBlank())
                .map(BlockForm::toInput)
                .toList();
        return new SavePageCommand(pagePublicId, pageKey, pageType,
                locale, slug, title, seoTitle, metaDescription, inputs);
    }

    static ContentPageForm from(PageDraftView draft) {
        ContentPageForm form = new ContentPageForm();
        form.pagePublicId = draft.pagePublicId();
        form.pageKey = draft.pageKey();
        form.pageType = draft.pageTypeCode();
        form.locale = draft.locale();
        form.slug = draft.slug();
        form.title = draft.title();
        form.seoTitle = draft.seoTitle();
        form.metaDescription = draft.metaDescription();
        form.blocks = new ArrayList<>();
        for (BlockInput block : draft.blocks()) {
            BlockForm formBlock = new BlockForm();
            formBlock.setType(block.type());
            formBlock.setPosition(block.position());
            formBlock.setHeading(block.heading());
            formBlock.setBodyMarkdown(block.bodyMarkdown());
            formBlock.setTargetUrl(block.targetUrl());
            formBlock.setMediaPublicId(block.mediaPublicId() == null ? "" : block.mediaPublicId().toString());
            form.blocks.add(formBlock);
        }
        while (form.blocks.size() < 8) {
            form.blocks.add(new BlockForm());
        }
        return form;
    }

    private static List<BlockForm> initialBlocks() {
        List<BlockForm> result = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            BlockForm block = new BlockForm();
            block.setPosition(index);
            result.add(block);
        }
        return result;
    }
}
