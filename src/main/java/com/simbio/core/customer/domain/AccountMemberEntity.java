package com.simbio.core.customer.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "account_member", schema = "customer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_customer_member_account_user",
                        columnNames = {"account_id", "user_public_id"}),
                @UniqueConstraint(name = "uq_customer_member_owner_slot",
                        columnNames = {"account_id", "owner_slot"})
        })
public class AccountMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private ClientAccountEntity account;
    @Column(name = "user_public_id", nullable = false)
    private UUID userPublicId;
    @Column(name = "role_code", nullable = false, length = 16)
    private String roleCode;
    @Column(name = "status_code", nullable = false, length = 16)
    private String statusCode;
    @Column(name = "owner_slot", length = 5)
    private String ownerSlot;
    @Column(name = "joined_at", nullable = false)
    private java.time.Instant joinedAt;

    protected AccountMemberEntity() {
    }

    public AccountMemberEntity(ClientAccountEntity account, UUID userPublicId) {
        this(account, userPublicId, "OWNER", "ACTIVE", java.time.Instant.now(java.time.Clock.systemUTC()));
    }

    public AccountMemberEntity(ClientAccountEntity account, UUID userPublicId, String roleCode,
            String statusCode, java.time.Instant joinedAt) {
        if (account == null || userPublicId == null || roleCode == null || statusCode == null || joinedAt == null
                || !roleCode.equals("OWNER") && !roleCode.equals("MEMBER")
                || !statusCode.equals("ACTIVE") && !statusCode.equals("DISABLED")) {
            throw new IllegalArgumentException("Invalid client account member");
        }
        this.account = account;
        this.userPublicId = userPublicId;
        this.roleCode = roleCode;
        this.statusCode = statusCode;
        this.ownerSlot = roleCode.equals("OWNER") && statusCode.equals("ACTIVE") ? "OWNER" : null;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public ClientAccountEntity getAccount() {
        return account;
    }

    public UUID getUserPublicId() {
        return userPublicId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public java.time.Instant getJoinedAt() {
        return joinedAt;
    }
}
