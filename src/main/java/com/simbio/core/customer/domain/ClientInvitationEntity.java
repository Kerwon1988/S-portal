package com.simbio.core.customer.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_invitation", schema = "customer")
public class ClientInvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "invitation_reference", nullable = false, unique = true)
    private UUID invitationReference;
    @Column(name = "account_type", nullable = false, length = 24)
    private String accountType;
    @Column(name = "account_name_ciphertext", nullable = false, length = 2048)
    private String accountNameCiphertext;
    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "accepted_user_id")
    private UUID acceptedUserId;
    @Column(name = "client_account_public_id")
    private UUID clientAccountPublicId;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected ClientInvitationEntity() {
    }

    public ClientInvitationEntity(
            UUID invitationReference,
            String accountType,
            String accountNameCiphertext,
            UUID createdByUserId,
            Instant createdAt) {
        if (invitationReference == null || accountType == null || accountNameCiphertext == null
                || accountNameCiphertext.isBlank() || accountNameCiphertext.length() > 2048
                || createdByUserId == null || createdAt == null) {
            throw new IllegalArgumentException("Invalid client invitation");
        }
        ClientAccountType.valueOf(accountType);
        this.invitationReference = invitationReference;
        this.accountType = accountType;
        this.accountNameCiphertext = accountNameCiphertext;
        this.createdByUserId = createdByUserId;
        this.status = "PENDING";
        this.createdAt = createdAt;
    }

    public void complete(UUID acceptedUserId, UUID clientAccountPublicId, Instant completedAt) {
        if (!"PENDING".equals(status) || acceptedUserId == null
                || clientAccountPublicId == null || completedAt == null) {
            throw new IllegalStateException("Client invitation cannot be completed");
        }
        this.acceptedUserId = acceptedUserId;
        this.clientAccountPublicId = clientAccountPublicId;
        this.completedAt = completedAt;
        this.status = "COMPLETED";
    }

    public Long getId() {
        return id;
    }

    public UUID getInvitationReference() {
        return invitationReference;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getAccountNameCiphertext() {
        return accountNameCiphertext;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public String getStatus() {
        return status;
    }

    public UUID getAcceptedUserId() {
        return acceptedUserId;
    }

    public UUID getClientAccountPublicId() {
        return clientAccountPublicId;
    }
}
