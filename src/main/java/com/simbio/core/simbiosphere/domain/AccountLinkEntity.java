package com.simbio.core.simbiosphere.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "account_link", schema = "simbiosphere",
        uniqueConstraints = @UniqueConstraint(name = "uq_simbiosphere_external_account",
                columnNames = {"external_system", "external_account_id"}))
public class AccountLinkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(name = "client_account_public_id", nullable = false)
    private UUID clientAccountPublicId;
    @Column(name = "external_system", nullable = false, length = 32)
    private String externalSystem;
    @Column(name = "external_account_id", nullable = false, length = 128)
    private String externalAccountId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountLinkEntity() {
    }

    public AccountLinkEntity(UUID publicId, UUID clientAccountPublicId, String externalSystem,
            String externalAccountId, Instant createdAt) {
        this.publicId = Objects.requireNonNull(publicId);
        this.clientAccountPublicId = Objects.requireNonNull(clientAccountPublicId);
        this.externalSystem = requireText(externalSystem, "externalSystem");
        this.externalAccountId = requireText(externalAccountId, "externalAccountId");
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    private static String requireText(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public UUID getClientAccountPublicId() { return clientAccountPublicId; }
    public String getExternalSystem() { return externalSystem; }
    public String getExternalAccountId() { return externalAccountId; }
    public Instant getCreatedAt() { return createdAt; }
}
