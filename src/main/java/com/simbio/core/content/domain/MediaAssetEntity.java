package com.simbio.core.content.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "media_asset", schema = "content")
public class MediaAssetEntity {

    @Id
    @Column(name = "file_public_id", nullable = false)
    private UUID filePublicId;

    @Column(name = "alt_es", nullable = false, length = 500)
    private String altEs;

    @Column(name = "alt_en", nullable = false, length = 500)
    private String altEn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaAssetEntity() {
    }

    public MediaAssetEntity(UUID filePublicId, String altEs, String altEn, Instant createdAt) {
        this.filePublicId = filePublicId;
        this.altEs = altEs;
        this.altEn = altEn;
        this.createdAt = createdAt;
    }

    public UUID getFilePublicId() {
        return filePublicId;
    }

    public String altFor(String locale) {
        return locale.equals("es") ? altEs : altEn;
    }

    public String getAltEs() {
        return altEs;
    }

    public String getAltEn() {
        return altEn;
    }
}
