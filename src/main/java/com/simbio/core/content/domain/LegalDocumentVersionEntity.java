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
@Table(name = "legal_document_version", schema = "content",
        uniqueConstraints = @UniqueConstraint(name = "uq_content_legal_version",
                columnNames = {"document_type", "locale", "version_no"}))
public class LegalDocumentVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "document_type", nullable = false, length = 32)
    private String documentType;

    @Column(name = "locale", nullable = false, length = 2)
    private String locale;

    @Column(name = "version_no", nullable = false)
    private int version;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content_markdown", nullable = false, length = 16000)
    private String contentMarkdown;

    @Column(name = "approval_state", nullable = false, length = 16)
    private String approvalState;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LegalDocumentVersionEntity() {
    }

    public LegalDocumentVersionEntity(UUID publicId, String documentType, String locale, int version,
            String title, String contentMarkdown, String approvalState, Instant effectiveFrom,
            UUID actorId, Instant createdAt) {
        this.publicId = publicId;
        this.documentType = documentType;
        this.locale = locale;
        this.version = version;
        this.title = title;
        this.contentMarkdown = contentMarkdown;
        this.approvalState = approvalState;
        this.effectiveFrom = effectiveFrom;
        this.actorId = actorId;
        this.createdAt = createdAt;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getLocale() {
        return locale;
    }

    public int getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public String getApprovalState() {
        return approvalState;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public UUID getActorId() {
        return actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
