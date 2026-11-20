package com.simbio.core.messaging.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contact_submission", schema = "messaging")
public class ContactSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "conversation_id", nullable = false, unique = true)
    private Long conversationId;

    @Column(name = "person_name_encrypted", nullable = false, length = 2048)
    private String personNameEncrypted;

    @Column(name = "email_encrypted", nullable = false, length = 2048)
    private String emailEncrypted;

    @Column(name = "phone_encrypted", length = 2048)
    private String phoneEncrypted;

    @Column(name = "reason", nullable = false, length = 160)
    private String reason;

    @Column(name = "locale", nullable = false, length = 2)
    private String locale;

    @Column(name = "consent_version", nullable = false)
    private int consentVersion;

    @Column(name = "consent_text_sha256", nullable = false, length = 64)
    private String consentTextSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ContactSubmissionEntity() {
    }

    public ContactSubmissionEntity(UUID publicId, Long conversationId, String personNameEncrypted,
            String emailEncrypted, String phoneEncrypted, String reason, String locale,
            int consentVersion, String consentTextSha256, Instant createdAt) {
        this.publicId = Objects.requireNonNull(publicId);
        this.conversationId = Objects.requireNonNull(conversationId);
        this.personNameEncrypted = Objects.requireNonNull(personNameEncrypted);
        this.emailEncrypted = Objects.requireNonNull(emailEncrypted);
        this.phoneEncrypted = phoneEncrypted;
        this.reason = Objects.requireNonNull(reason);
        this.locale = Objects.requireNonNull(locale);
        if (consentVersion < 1) throw new IllegalArgumentException("Consent version must be positive");
        if (consentTextSha256 == null || !consentTextSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Consent text digest is invalid");
        }
        this.consentVersion = consentVersion;
        this.consentTextSha256 = consentTextSha256;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public UUID getPublicId() { return publicId; }
    public Long getConversationId() { return conversationId; }
    public int getConsentVersion() { return consentVersion; }
    public String getConsentTextSha256() { return consentTextSha256; }
    public String getPersonNameEncrypted() { return personNameEncrypted; }
    public String getEmailEncrypted() { return emailEncrypted; }
    public String getPhoneEncrypted() { return phoneEncrypted; }
    public String getReason() { return reason; }
    public String getLocale() { return locale; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "ContactSubmissionEntity[publicId=" + publicId + ", conversationId=" + conversationId + "]";
    }
}
