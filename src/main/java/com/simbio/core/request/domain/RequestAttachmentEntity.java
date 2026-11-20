package com.simbio.core.request.domain;

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
@Table(name = "request_attachment", schema = "request")
public class RequestAttachmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_id", nullable = false)
    private Long requestId;
    @Column(name = "file_public_id", nullable = false)
    private UUID filePublicId;
    @Column(name = "added_by", nullable = false)
    private UUID addedBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RequestAttachmentEntity() {
    }

    public RequestAttachmentEntity(Long requestId, UUID filePublicId, UUID addedBy, Instant createdAt) {
        this.requestId = Objects.requireNonNull(requestId);
        this.filePublicId = Objects.requireNonNull(filePublicId);
        this.addedBy = Objects.requireNonNull(addedBy);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public UUID getFilePublicId() { return filePublicId; }
}
