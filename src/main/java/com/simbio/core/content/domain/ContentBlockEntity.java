package com.simbio.core.content.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "content_block", schema = "content")
public class ContentBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "translation_id", nullable = false)
    private PageTranslationEntity translation;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 16)
    private ContentBlockType type;

    @Column(name = "position_no", nullable = false)
    private int position;

    @Column(name = "heading", nullable = false, length = 200)
    private String heading;

    @Column(name = "body_markdown", nullable = false, length = 8000)
    private String bodyMarkdown;

    @Column(name = "target_url", length = 512)
    private String targetUrl;

    @Column(name = "media_public_id")
    private UUID mediaPublicId;

    protected ContentBlockEntity() {
    }

    public ContentBlockEntity(ContentBlockType type, int position, String heading, String bodyMarkdown,
            String targetUrl, UUID mediaPublicId) {
        this.type = type;
        this.position = position;
        this.heading = heading;
        this.bodyMarkdown = bodyMarkdown;
        this.targetUrl = targetUrl;
        this.mediaPublicId = mediaPublicId;
    }

    void attachTo(PageTranslationEntity translation) {
        this.translation = translation;
    }

    public Long getId() {
        return id;
    }

    public ContentBlockType getType() {
        return type;
    }

    public int getPosition() {
        return position;
    }

    public String getHeading() {
        return heading;
    }

    public String getBodyMarkdown() {
        return bodyMarkdown;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public UUID getMediaPublicId() {
        return mediaPublicId;
    }
}
