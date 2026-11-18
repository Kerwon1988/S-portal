package com.simbio.core.content.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "page_translation", schema = "content", uniqueConstraints = {
        @UniqueConstraint(name = "uq_page_translation_locale_slug", columnNames = {"locale", "slug"}),
        @UniqueConstraint(name = "uq_page_translation_page_locale", columnNames = {"page_id", "locale"})
})
public class PageTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity page;

    @Column(name = "locale", nullable = false, length = 2)
    private String locale;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "seo_title", nullable = false, length = 200)
    private String seoTitle;

    @Column(name = "meta_description", nullable = false, length = 320)
    private String metaDescription;

    @Column(name = "published_slug", length = 120)
    private String publishedSlug;

    @Column(name = "published_snapshot_json", length = 16000)
    private String publishedSnapshotJson;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_code", nullable = false, length = 16)
    private TranslationState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_state", nullable = false, length = 16)
    private ApprovalState approvalState;

    @Column(name = "version_no", nullable = false)
    private int version;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "translation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentBlockEntity> blocks = new ArrayList<>();

    protected PageTranslationEntity() {
    }

    PageTranslationEntity(PageEntity page, String locale) {
        this.publicId = UUID.randomUUID();
        this.page = page;
        this.locale = locale;
        this.slug = "";
        this.title = "";
        this.seoTitle = "";
        this.metaDescription = "";
        this.state = TranslationState.DRAFT;
        this.approvalState = ApprovalState.PENDING;
        this.updatedBy = new UUID(0, 0);
        this.updatedAt = Instant.EPOCH;
    }

    public void edit(String slug, String title, String seoTitle, String metaDescription, ApprovalState approvalState,
            UUID updatedBy, Instant updatedAt) {
        state = TranslationState.DRAFT;
        this.slug = slug;
        this.title = title;
        this.seoTitle = seoTitle;
        this.metaDescription = metaDescription;
        this.approvalState = approvalState;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public void replaceBlocks(List<ContentBlockEntity> newBlocks) {
        blocks.clear();
        for (ContentBlockEntity block : newBlocks) {
            block.attachTo(this);
            blocks.add(block);
        }
    }

    public void publish(String snapshotJson, UUID actorId, Instant publishedAt) {
        state = TranslationState.PUBLISHED;
        version++;
        publishedSlug = slug;
        publishedSnapshotJson = snapshotJson;
        this.publishedAt = publishedAt;
        updatedBy = actorId;
        updatedAt = publishedAt;
    }

    public void restoreDraft(String restoredSlug, String restoredTitle, String restoredSeoTitle,
            String restoredMetaDescription, List<ContentBlockEntity> restoredBlocks, UUID actorId, Instant restoredAt) {
        state = TranslationState.DRAFT;
        edit(restoredSlug, restoredTitle, restoredSeoTitle, restoredMetaDescription, approvalState, actorId, restoredAt);
        replaceBlocks(restoredBlocks);
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public PageEntity getPage() {
        return page;
    }

    public String getLocale() {
        return locale;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public TranslationState getState() {
        return state;
    }

    public ApprovalState getApprovalState() {
        return approvalState;
    }

    public int getVersion() {
        return version;
    }

    public String getPublishedSlug() {
        return publishedSlug;
    }

    public String getPublishedSnapshotJson() {
        return publishedSnapshotJson;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ContentBlockEntity> getBlocks() {
        return blocks.stream().sorted(java.util.Comparator.comparingInt(ContentBlockEntity::getPosition)).toList();
    }
}
