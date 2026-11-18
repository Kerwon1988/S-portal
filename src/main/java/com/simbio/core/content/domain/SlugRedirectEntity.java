package com.simbio.core.content.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "slug_redirect", schema = "content",
        uniqueConstraints = @UniqueConstraint(name = "uq_slug_redirect_locale_old", columnNames = {"locale", "old_slug"}))
public class SlugRedirectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "locale", nullable = false, length = 2)
    private String locale;

    @Column(name = "old_slug", nullable = false, length = 120)
    private String oldSlug;

    @Column(name = "new_slug", nullable = false, length = 120)
    private String newSlug;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SlugRedirectEntity() {
    }

    public SlugRedirectEntity(String locale, String oldSlug, String newSlug, Instant createdAt) {
        this.locale = locale;
        this.oldSlug = oldSlug;
        this.newSlug = newSlug;
        this.createdAt = createdAt;
    }

    public String getNewSlug() {
        return newSlug;
    }
}
