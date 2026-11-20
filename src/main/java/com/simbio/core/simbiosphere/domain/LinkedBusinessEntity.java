package com.simbio.core.simbiosphere.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "linked_business", schema = "simbiosphere",
        uniqueConstraints = @UniqueConstraint(name = "uq_simbiosphere_external_business",
                columnNames = "external_business_id"))
public class LinkedBusinessEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_link_id", nullable = false)
    private AccountLinkEntity accountLink;
    @Column(name = "external_business_id", nullable = false, length = 128)
    private String externalBusinessId;
    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;
    @Column(name = "business_type", nullable = false, length = 40)
    private String businessType;
    @Column(name = "tenant_slug", nullable = false, length = 80)
    private String tenantSlug;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LinkedBusinessEntity() {
    }

    public LinkedBusinessEntity(UUID publicId, AccountLinkEntity accountLink,
            String externalBusinessId, String businessName, String businessType, String tenantSlug,
            Instant createdAt) {
        this.publicId = Objects.requireNonNull(publicId);
        this.accountLink = Objects.requireNonNull(accountLink);
        this.externalBusinessId = requireText(externalBusinessId, "externalBusinessId");
        this.businessName = requireText(businessName, "businessName");
        this.businessType = requireText(businessType, "businessType");
        this.tenantSlug = requireText(tenantSlug, "tenantSlug");
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    private static String requireText(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public AccountLinkEntity getAccountLink() { return accountLink; }
    public String getExternalBusinessId() { return externalBusinessId; }
    public String getBusinessName() { return businessName; }
    public String getBusinessType() { return businessType; }
    public String getTenantSlug() { return tenantSlug; }
    public Instant getCreatedAt() { return createdAt; }
}
