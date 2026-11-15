package com.simbio.core.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_account", schema = "identity")
public class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(name = "email_normalized", nullable = false, unique = true, length = 320)
    private String emailNormalized;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;
    @Column(name = "preferred_locale", nullable = false, length = 5)
    private String preferredLocale;
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;
    @Column(name = "consecutive_lock_count", nullable = false)
    private int consecutiveLockCount;
    @Column(name = "locked_until")
    private Instant lockedUntil;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role", schema = "identity",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_code"))
    private Set<RoleEntity> roles = new HashSet<>();

    protected UserAccountEntity() {
    }

    public UserAccountEntity(UUID publicId, String emailNormalized, String passwordHash,
            String locale, Instant now, RoleEntity role) {
        this.publicId = publicId;
        this.emailNormalized = emailNormalized;
        this.passwordHash = passwordHash;
        this.status = "ACTIVE";
        this.emailVerifiedAt = now;
        this.preferredLocale = locale;
        this.createdAt = now;
        this.roles.add(role);
    }

    public boolean isEnabled() {
        return "ACTIVE".equals(status) && emailVerifiedAt != null;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void recordFailedLogin(Instant now) {
        if (isLockedAt(now)) {
            return;
        }
        if (lockedUntil != null) {
            failedLoginCount = 0;
            lockedUntil = null;
        }
        failedLoginCount++;
        if (failedLoginCount >= 5) {
            long seconds = Math.min(3600, 60L << Math.min(consecutiveLockCount, 5));
            lockedUntil = now.plus(Duration.ofSeconds(seconds));
            consecutiveLockCount++;
            failedLoginCount = 0;
        }
    }

    public void resetLoginFailures() {
        failedLoginCount = 0;
        consecutiveLockCount = 0;
        lockedUntil = null;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getEmailNormalized() {
        return emailNormalized;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public Set<RoleEntity> getRoles() {
        return Set.copyOf(roles);
    }
}
