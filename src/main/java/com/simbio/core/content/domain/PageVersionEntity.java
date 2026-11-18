package com.simbio.core.content.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "page_version", schema = "content",
        uniqueConstraints = @UniqueConstraint(name = "uq_page_version_translation_version",
                columnNames = {"translation_public_id", "version_no"}))
public class PageVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "translation_public_id", nullable = false)
    private UUID translationPublicId;

    @Column(name = "version_no", nullable = false)
    private int version;

    @Column(name = "snapshot_json", nullable = false, length = 16000)
    private String snapshotJson;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PageVersionEntity() {
    }

    public PageVersionEntity(UUID translationPublicId, int version, String snapshotJson, UUID actorId,
            Instant createdAt) {
        this.translationPublicId = translationPublicId;
        this.version = version;
        this.snapshotJson = snapshotJson;
        this.actorId = actorId;
        this.createdAt = createdAt;
    }

    public UUID getTranslationPublicId() {
        return translationPublicId;
    }

    public int getVersion() {
        return version;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public UUID getActorId() {
        return actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
