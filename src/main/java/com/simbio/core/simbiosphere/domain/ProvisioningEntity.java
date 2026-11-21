package com.simbio.core.simbiosphere.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.simbio.core.simbiosphere.api.ProvisioningStatus;

@Entity
@Table(name = "provisioning", schema = "simbiosphere")
public class ProvisioningEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(name = "client_account_public_id", nullable = false, unique = true)
    private UUID clientAccountPublicId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 24)
    private ProvisioningStatus status;
    @Column(name = "external_system", nullable = false, length = 32)
    private String externalSystem;
    @Column(name = "external_account_id", length = 128)
    private String externalAccountId;
    @Column(name = "external_customer_account_id")
    private Long externalCustomerAccountId;
    @Column(name = "idempotency_key", nullable = false, length = 128, unique = true)
    private String idempotencyKey;
    @Column(name = "owner_user_public_id")
    private UUID ownerUserPublicId;
    @Column(name = "business_name", length = 200)
    private String businessName;
    @Column(name = "business_slug", length = 50)
    private String businessSlug;
    @Column(name = "business_type", length = 32)
    private String businessType;
    @Column(name = "location_name", length = 200)
    private String locationName;
    @Column(name = "location_slug", length = 50)
    private String locationSlug;
    @Column(name = "modules_json", length = 2048)
    private String modulesJson;
    @Column(name = "terms_version", length = 64)
    private String termsVersion;
    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;
    @Column(name = "error_code", length = 64)
    private String errorCode;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "lease_owner", length = 80)
    private String leaseOwner;
    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProvisioningEntity() {
    }

    public ProvisioningEntity(UUID publicId, UUID clientAccountPublicId, String externalSystem,
            String idempotencyKey, Instant createdAt) {
        this.publicId = Objects.requireNonNull(publicId);
        this.clientAccountPublicId = Objects.requireNonNull(clientAccountPublicId);
        this.externalSystem = requireText(externalSystem, "externalSystem");
        this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = createdAt;
        this.status = ProvisioningStatus.PENDING;
    }

    public void attachIntent(UUID ownerUserId, String businessName, String businessSlug,
            String businessType, String locationName, String locationSlug, String modulesJson,
            String termsVersion, Instant termsAcceptedAt) {
        if (this.businessName != null) throw new IllegalStateException("Provisioning intent already attached");
        this.ownerUserPublicId = Objects.requireNonNull(ownerUserId);
        this.businessName = requireText(businessName, "businessName");
        this.businessSlug = requireText(businessSlug, "businessSlug");
        this.businessType = requireText(businessType, "businessType");
        this.locationName = requireText(locationName, "locationName");
        this.locationSlug = requireText(locationSlug, "locationSlug");
        this.modulesJson = modulesJson;
        this.termsVersion = requireText(termsVersion, "termsVersion");
        this.termsAcceptedAt = Objects.requireNonNull(termsAcceptedAt);
    }

    public boolean markReady(Instant now) {
        requireStatus(ProvisioningStatus.PENDING);
        if (businessName == null || termsAcceptedAt == null) {
            throw new IllegalStateException("Complete provisioning intent and separate terms are required");
        }
        status = ProvisioningStatus.READY;
        updatedAt = Objects.requireNonNull(now);
        return true;
    }

    public void markProcessing(String owner, Instant now, Instant leaseUntil) {
        requireStatus(ProvisioningStatus.READY);
        leaseOwner = requireText(owner, "leaseOwner");
        leaseExpiresAt = Objects.requireNonNull(leaseUntil);
        status = ProvisioningStatus.PROCESSING;
        attemptCount++;
        updatedAt = Objects.requireNonNull(now);
    }

    public void complete(String accountId, long customerAccountId, Instant now) {
        requireStatus(ProvisioningStatus.PROCESSING);
        externalAccountId = requireText(accountId, "externalAccountId");
        if (customerAccountId < 1) throw new IllegalArgumentException("customerAccountId must be positive");
        externalCustomerAccountId = customerAccountId;
        status = ProvisioningStatus.PROVISIONED;
        errorCode = null;
        nextAttemptAt = null;
        clearLease();
        updatedAt = Objects.requireNonNull(now);
    }

    public void actionRequired(String code, Instant now) {
        requireStatus(ProvisioningStatus.PROCESSING);
        status = ProvisioningStatus.ACTION_REQUIRED;
        errorCode = requireText(code, "errorCode");
        clearLease();
        updatedAt = Objects.requireNonNull(now);
    }

    public void correctAndRequeue(String newSlug, String newIdempotencyKey, Instant now) {
        requireStatus(ProvisioningStatus.ACTION_REQUIRED);
        businessSlug = requireText(newSlug, "businessSlug");
        if (!businessSlug.matches("^[a-z0-9-]{3,50}$")) throw new IllegalArgumentException("businessSlug is invalid");
        idempotencyKey = requireText(newIdempotencyKey, "idempotencyKey");
        status = ProvisioningStatus.READY;
        errorCode = null;
        nextAttemptAt = Objects.requireNonNull(now);
        updatedAt = now;
    }

    public void retry(String code, Instant nextAttempt, Instant now) {
        requireStatus(ProvisioningStatus.PROCESSING, ProvisioningStatus.ACTION_REQUIRED);
        status = ProvisioningStatus.READY;
        errorCode = requireText(code, "errorCode");
        nextAttemptAt = Objects.requireNonNull(nextAttempt);
        clearLease();
        updatedAt = Objects.requireNonNull(now);
    }

    public void fail(String code, Instant now) {
        requireStatus(ProvisioningStatus.PROCESSING, ProvisioningStatus.ACTION_REQUIRED);
        status = ProvisioningStatus.FAILED;
        errorCode = requireText(code, "errorCode");
        nextAttemptAt = null;
        clearLease();
        updatedAt = Objects.requireNonNull(now);
    }

    private void requireStatus(ProvisioningStatus... allowed) {
        for (ProvisioningStatus candidate : allowed) {
            if (candidate == status) return;
        }
        throw new IllegalStateException("Invalid provisioning transition from " + status);
    }

    private void clearLease() {
        leaseOwner = null;
        leaseExpiresAt = null;
    }

    private static String requireText(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return normalized;
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public UUID getClientAccountPublicId() { return clientAccountPublicId; }
    public ProvisioningStatus getStatus() { return status; }
    public String getExternalSystem() { return externalSystem; }
    public String getExternalAccountId() { return externalAccountId; }
    public Long getExternalCustomerAccountId() { return externalCustomerAccountId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public UUID getOwnerUserPublicId() { return ownerUserPublicId; }
    public String getBusinessName() { return businessName; }
    public String getBusinessSlug() { return businessSlug; }
    public String getBusinessType() { return businessType; }
    public String getLocationName() { return locationName; }
    public String getLocationSlug() { return locationSlug; }
    public String getModulesJson() { return modulesJson; }
    public String getTermsVersion() { return termsVersion; }
    public Instant getTermsAcceptedAt() { return termsAcceptedAt; }
    public String getErrorCode() { return errorCode; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLeaseOwner() { return leaseOwner; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
