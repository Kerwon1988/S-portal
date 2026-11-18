package com.simbio.core.legal.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.simbio.core.legal.api.ConsentAcceptance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "consent_record", schema = "legal",
        uniqueConstraints = @UniqueConstraint(name = "uq_legal_consent_public_id", columnNames = "public_id"))
public class ConsentRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @Column(name = "subject_user_id", nullable = false)
    private UUID subjectUserId;

    @Column(name = "purpose_code", nullable = false, length = 40)
    private String purposeCode;

    @Column(name = "document_type", nullable = false, length = 32)
    private String documentType;

    @Column(name = "document_locale", nullable = false, length = 2)
    private String documentLocale;

    @Column(name = "document_version", nullable = false)
    private int documentVersion;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "source_code", nullable = false, length = 32)
    private String sourceCode;

    protected ConsentRecordEntity() {
    }

    public ConsentRecordEntity(UUID publicId, ConsentAcceptance acceptance, Instant acceptedAt) {
        this.publicId = Objects.requireNonNull(publicId);
        this.subjectUserId = acceptance.subjectUserId();
        this.purposeCode = acceptance.purpose().name();
        this.documentType = acceptance.purpose().documentType();
        this.documentLocale = acceptance.locale();
        this.documentVersion = acceptance.documentVersion();
        this.acceptedAt = Objects.requireNonNull(acceptedAt);
        this.sourceCode = acceptance.source().name();
    }

    public UUID getPublicId() {
        return publicId;
    }

    public UUID getSubjectUserId() {
        return subjectUserId;
    }

    public String getPurposeCode() {
        return purposeCode;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentLocale() {
        return documentLocale;
    }

    public int getDocumentVersion() {
        return documentVersion;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public String getSourceCode() {
        return sourceCode;
    }
}
