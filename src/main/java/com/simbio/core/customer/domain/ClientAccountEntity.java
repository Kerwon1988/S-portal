package com.simbio.core.customer.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_account", schema = "customer")
public class ClientAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 24)
    private ClientAccountType accountType;
    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "country_code", length = 2)
    private String countryCode;
    @Column(name = "tax_id_ciphertext", length = 2048)
    private String taxIdCiphertext;
    @Column(name = "phone_ciphertext", length = 2048)
    private String phoneCiphertext;

    protected ClientAccountEntity() {
    }

    public ClientAccountEntity(UUID publicId, ClientAccountType accountType, String displayName, Instant createdAt) {
        if (publicId == null || accountType == null || displayName == null
                || displayName.isBlank() || displayName.length() > 160 || createdAt == null) {
            throw new IllegalArgumentException("Invalid client account");
        }
        this.publicId = publicId;
        this.accountType = accountType;
        this.displayName = displayName.trim();
        this.status = "ACTIVE";
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public ClientAccountType getAccountType() {
        return accountType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getTaxIdCiphertext() {
        return taxIdCiphertext;
    }

    public String getPhoneCiphertext() {
        return phoneCiphertext;
    }

    public void setTaxIdentifier(String countryCode, String taxIdCiphertext) {
        if (countryCode == null || !countryCode.matches("[A-Z]{2}")
                || taxIdCiphertext == null || taxIdCiphertext.isBlank() || taxIdCiphertext.length() > 2048) {
            throw new IllegalArgumentException("Invalid protected tax identifier");
        }
        this.countryCode = countryCode;
        this.taxIdCiphertext = taxIdCiphertext;
    }

    public void setPhoneCiphertext(String phoneCiphertext) {
        if (phoneCiphertext == null || phoneCiphertext.isBlank() || phoneCiphertext.length() > 2048) {
            throw new IllegalArgumentException("Invalid protected phone number");
        }
        this.phoneCiphertext = phoneCiphertext;
    }
}
