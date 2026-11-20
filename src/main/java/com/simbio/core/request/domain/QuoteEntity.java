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
@Table(name = "quote", schema = "request")
public class QuoteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(name = "request_id", nullable = false, unique = true)
    private Long requestId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected QuoteEntity() {
    }

    public QuoteEntity(UUID publicId, Long requestId, Instant createdAt) {
        this.publicId = Objects.requireNonNull(publicId);
        this.requestId = Objects.requireNonNull(requestId);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Long getRequestId() { return requestId; }
}
