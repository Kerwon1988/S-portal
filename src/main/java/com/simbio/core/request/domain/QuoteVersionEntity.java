package com.simbio.core.request.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import com.simbio.core.request.api.QuoteStatus;

@Entity
@Table(name = "quote_version", schema = "request", uniqueConstraints =
        @UniqueConstraint(name = "uq_quote_version", columnNames = {"quote_id", "version_number"}))
public class QuoteVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "quote_id", nullable = false)
    private Long quoteId;
    @Column(name = "version_number", nullable = false)
    private int versionNumber;
    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(name = "tax_rate", precision = 7, scale = 4)
    private BigDecimal taxRate;
    @Column(name = "tax_amount", precision = 19, scale = 4)
    private BigDecimal taxAmount;
    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossAmount;
    @Column(name = "valid_until")
    private LocalDate validUntil;
    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 16)
    private QuoteStatus status;
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "decided_by")
    private UUID decidedBy;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    protected QuoteVersionEntity() {
    }

    public QuoteVersionEntity(Long quoteId, int versionNumber, BigDecimal netAmount, String currency,
            BigDecimal taxRate, LocalDate validUntil, UUID createdBy, Instant createdAt) {
        this.quoteId = Objects.requireNonNull(quoteId);
        if (versionNumber < 1) throw new IllegalArgumentException("Quote version must be positive");
        if (netAmount == null || netAmount.scale() > 4 || netAmount.signum() < 0) {
            throw new IllegalArgumentException("Quote amount is invalid");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Quote currency must be ISO 4217");
        }
        if (taxRate != null && (taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Quote tax rate is invalid");
        }
        this.versionNumber = versionNumber;
        this.netAmount = netAmount.setScale(4, RoundingMode.UNNECESSARY);
        this.currency = currency;
        this.taxRate = taxRate;
        this.taxAmount = taxRate == null ? null : netAmount.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        this.grossAmount = taxAmount == null ? netAmount : netAmount.add(taxAmount);
        this.validUntil = validUntil;
        this.status = QuoteStatus.DRAFT;
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public void send(Instant now) {
        if (status != QuoteStatus.DRAFT) throw new IllegalStateException("Only a draft quote can be sent");
        if (validUntil == null || !validUntil.isAfter(today(now)) && !validUntil.equals(today(now))) {
            throw new IllegalArgumentException("Quote validity must be current");
        }
        if (taxRate == null) throw new IllegalArgumentException("Tax conditions must be supplied before sending");
        status = QuoteStatus.SENT;
        sentAt = Objects.requireNonNull(now);
    }

    public void decide(QuoteStatus decision, UUID actor, Instant now) {
        if (decision != QuoteStatus.ACCEPTED && decision != QuoteStatus.REJECTED) {
            throw new IllegalArgumentException("Only acceptance or rejection is allowed");
        }
        if (status != QuoteStatus.SENT) throw new IllegalStateException("Quote is not awaiting a decision");
        if (validUntil == null || validUntil.isBefore(today(now))) {
            throw new IllegalStateException("Quote has expired");
        }
        status = decision;
        decidedBy = Objects.requireNonNull(actor);
        decidedAt = Objects.requireNonNull(now);
    }

    public boolean expire(Instant now) {
        if (status != QuoteStatus.SENT || validUntil == null || !validUntil.isBefore(today(now))) return false;
        status = QuoteStatus.EXPIRED;
        decidedAt = Objects.requireNonNull(now);
        return true;
    }

    private static LocalDate today(Instant instant) { return Objects.requireNonNull(instant).atZone(ZoneOffset.UTC).toLocalDate(); }

    public Long getId() { return id; }
    public Long getQuoteId() { return quoteId; }
    public int getVersionNumber() { return versionNumber; }
    public BigDecimal getNetAmount() { return netAmount; }
    public String getCurrency() { return currency; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public LocalDate getValidUntil() { return validUntil; }
    public QuoteStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public UUID getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public long getRowVersion() { return rowVersion; }
}
