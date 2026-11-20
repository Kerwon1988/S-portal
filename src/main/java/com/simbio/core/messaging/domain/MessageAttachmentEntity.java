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
@Table(name = "message_attachment", schema = "messaging")
public class MessageAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "file_public_id", nullable = false, unique = true)
    private UUID filePublicId;

    @Column(name = "detected_mime", nullable = false, length = 100)
    private String detectedMime;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MessageAttachmentEntity() {
    }

    public MessageAttachmentEntity(Long messageId, UUID filePublicId, String detectedMime,
            long sizeBytes, Instant createdAt) {
        this.messageId = Objects.requireNonNull(messageId);
        this.filePublicId = Objects.requireNonNull(filePublicId);
        this.detectedMime = Objects.requireNonNull(detectedMime);
        if (sizeBytes < 1) throw new IllegalArgumentException("Attachment size must be positive");
        this.sizeBytes = sizeBytes;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public Long getId() { return id; }
    public Long getMessageId() { return messageId; }
    public UUID getFilePublicId() { return filePublicId; }
    public String getDetectedMime() { return detectedMime; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
}
