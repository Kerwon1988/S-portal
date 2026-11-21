package com.simbio.core.request.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

import com.simbio.core.request.api.RequestStatus;
import com.simbio.core.request.api.RequestType;

@Entity
@Table(name = "request", schema = "request")
public class RequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(name = "reference_code", nullable = false, unique = true, length = 20)
    private String referenceCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 24)
    private RequestType type;
    @Column(name = "category_code", nullable = false, length = 40)
    private String category;
    @Column(name = "title", nullable = false, length = 160)
    private String title;
    @Column(name = "description", nullable = false, length = 10000)
    private String description;
    @Column(name = "objective", length = 2000)
    private String objective;
    @Column(name = "impact", length = 4000)
    private String impact;
    @Column(name = "related_module", length = 80)
    private String relatedModule;
    @Column(name = "linked_business_public_id")
    private UUID linkedBusinessPublicId;
    @Column(name = "estimated_budget", precision = 19, scale = 4)
    private BigDecimal estimatedBudget;
    @Column(name = "target_date")
    private LocalDate targetDate;
    @Column(name = "owner_account_id", nullable = false)
    private UUID ownerAccountId;
    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 32)
    private RequestStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "paused_from_status", length = 32)
    private RequestStatus pausedFromStatus;
    @Column(name = "decision_reason", length = 2000)
    private String decisionReason;
    @Column(name = "duplicate_reference", length = 20)
    private String duplicateReference;
    @Column(name = "priority_code", nullable = false, length = 16)
    private String priority = "NORMAL";
    @Column(name = "estimate_hours", precision = 9, scale = 2)
    private BigDecimal estimateHours;
    @Column(name = "first_response_at")
    private Instant firstResponseAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected RequestEntity() {
    }

    public RequestEntity(UUID publicId, String referenceCode, RequestType type, String category,
            String title, String description, String objective, String impact, String relatedModule,
            BigDecimal estimatedBudget, LocalDate targetDate, UUID ownerAccountId, UUID ownerUserId,
            Instant createdAt) {
        this(publicId, referenceCode, type, category, title, description, objective, impact,
                relatedModule, null, estimatedBudget, targetDate, ownerAccountId, ownerUserId, createdAt);
    }

    public RequestEntity(UUID publicId, String referenceCode, RequestType type, String category,
            String title, String description, String objective, String impact, String relatedModule,
            UUID linkedBusinessPublicId, BigDecimal estimatedBudget, LocalDate targetDate, UUID ownerAccountId, UUID ownerUserId,
            Instant createdAt) {
        this.publicId = Objects.requireNonNull(publicId);
        this.referenceCode = requireText(referenceCode, "reference");
        this.type = Objects.requireNonNull(type);
        this.category = requireText(category, "category");
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.objective = trimToNull(objective);
        this.impact = trimToNull(impact);
        this.relatedModule = trimToNull(relatedModule);
        this.linkedBusinessPublicId = linkedBusinessPublicId;
        this.estimatedBudget = estimatedBudget == null || estimatedBudget.signum() >= 0
                ? estimatedBudget : invalid("Estimated budget cannot be negative");
        this.targetDate = targetDate;
        this.ownerAccountId = Objects.requireNonNull(ownerAccountId);
        this.ownerUserId = Objects.requireNonNull(ownerUserId);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = RequestStatus.RECIBIDA;
    }

    public void transition(RequestStatus target, String reason, String originalReference) {
        Objects.requireNonNull(target);
        if (type == RequestType.CUSTOM_DEVELOPMENT) {
            CustomDevelopmentStateMachine.requireTransition(status, target, reason, pausedFromStatus);
        } else {
            SimbiosphereModuleStateMachine.requireTransition(status, target, reason, originalReference);
        }
        if (target == RequestStatus.EN_PAUSA) {
            pausedFromStatus = status;
        } else if (status == RequestStatus.EN_PAUSA) {
            pausedFromStatus = null;
        }
        if (target == RequestStatus.DESCARTADA) {
            decisionReason = requireText(reason, "reason");
        } else if (target == RequestStatus.DUPLICADA) {
            duplicateReference = requireText(originalReference, "original reference");
        }
        status = target;
    }

    public void updateOperational(String priorityCode, BigDecimal estimate, Instant now, boolean recordFirstResponse) {
        String normalized = priorityCode == null ? priority : priorityCode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("LOW|NORMAL|HIGH|URGENT")) {
            throw new IllegalArgumentException("Priority is invalid");
        }
        if (estimate != null && (estimate.signum() < 0 || estimate.compareTo(new BigDecimal("9999999.99")) > 0)) {
            throw new IllegalArgumentException("Estimate hours cannot be negative or oversized");
        }
        priority = normalized;
        estimateHours = estimate;
        if (recordFirstResponse && firstResponseAt == null) firstResponseAt = Objects.requireNonNull(now);
    }

    private static BigDecimal invalid(String message) { throw new IllegalArgumentException(message); }
    private static String requireText(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException(field + " is required");
        return trimmed;
    }
    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public String getReferenceCode() { return referenceCode; }
    public RequestType getType() { return type; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getObjective() { return objective; }
    public String getImpact() { return impact; }
    public String getRelatedModule() { return relatedModule; }
    public UUID getLinkedBusinessPublicId() { return linkedBusinessPublicId; }
    public BigDecimal getEstimatedBudget() { return estimatedBudget; }
    public LocalDate getTargetDate() { return targetDate; }
    public UUID getOwnerAccountId() { return ownerAccountId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public RequestStatus getStatus() { return status; }
    public RequestStatus getPausedFromStatus() { return pausedFromStatus; }
    public String getDecisionReason() { return decisionReason; }
    public String getDuplicateReference() { return duplicateReference; }
    public String getPriority() { return priority; }
    public BigDecimal getEstimateHours() { return estimateHours; }
    public Instant getFirstResponseAt() { return firstResponseAt; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
