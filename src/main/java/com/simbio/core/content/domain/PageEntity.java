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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "page", schema = "content")
public class PageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "page_key", nullable = false, unique = true, length = 120)
    private String pageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", nullable = false, length = 32)
    private ContentPageType pageType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PageTranslationEntity> translations = new ArrayList<>();

    protected PageEntity() {
    }

    public PageEntity(UUID publicId, String pageKey, ContentPageType pageType, Instant createdAt) {
        this.publicId = publicId;
        this.pageKey = pageKey;
        this.pageType = pageType;
        this.createdAt = createdAt;
    }

    public PageTranslationEntity translation(String locale) {
        return translations.stream()
                .filter(translation -> translation.getLocale().equals(locale))
                .findFirst()
                .orElseGet(() -> {
                    PageTranslationEntity translation = new PageTranslationEntity(this, locale);
                    translations.add(translation);
                    return translation;
                });
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getPageKey() {
        return pageKey;
    }

    public ContentPageType getPageType() {
        return pageType;
    }

    public List<PageTranslationEntity> getTranslations() {
        return List.copyOf(translations);
    }
}
