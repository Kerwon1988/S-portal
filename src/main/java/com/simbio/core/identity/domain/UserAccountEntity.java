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
    @Column(name = "display_name", length = 120)
    private String displayName;
    @Column(name = "theme_preference", length = 8)
    private String themePreference;
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
        this(publicId, emailNormalized, passwordHash, locale, now, role, null);
    }

    public UserAccountEntity(UUID publicId, String emailNormalized, String passwordHash,
            String locale, Instant now, RoleEntity role, String displayName) {
        this(publicId, emailNormalized, passwordHash, locale, now, role, displayName, true);
    }

    public UserAccountEntity(UUID publicId, String emailNormalized, String passwordHash,
            String locale, Instant now, RoleEntity role, String displayName, boolean emailVerified) {
        this.publicId = publicId;
        this.emailNormalized = emailNormalized;
        this.passwordHash = passwordHash;
        this.status = "ACTIVE";
        this.emailVerifiedAt = emailVerified ? now : null;
        this.preferredLocale = locale;
        this.displayName = displayName;
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

    public void verifyEmail(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    public void changePassword(String encodedPassword) {
        passwordHash = java.util.Objects.requireNonNull(encodedPassword);
    }

    public void setActive(boolean active) {
        status = active ? "ACTIVE" : "INACTIVE";
    }

    public void unlock() {
        resetLoginFailures();
    }

    public void replaceRole(RoleEntity role) {
        if (role == null) {
            throw new IllegalArgumentException("A role is required");
        }
        roles.clear();
        roles.add(role);
    }

    public void updateProfile(String displayName, String locale, String theme) {
        if (displayName == null || displayName.isBlank() || displayName.length() > 120) {
            throw new IllegalArgumentException("Display name must contain 1 to 120 characters");
        }
        if (!"es".equals(locale) && !"en".equals(locale)) {
            throw new IllegalArgumentException("Unsupported locale");
        }
        if (theme != null && !"light".equals(theme) && !"dark".equals(theme)) {
            throw new IllegalArgumentException("Unsupported theme");
        }
        this.displayName = displayName.trim();
        preferredLocale = locale;
        themePreference = theme;
    }

    public void updateLocale(String locale) {
        if (!"es".equals(locale) && !"en".equals(locale)) {
            throw new IllegalArgumentException("Unsupported locale");
        }
        preferredLocale = locale;
    }

    public void updateTheme(String theme) {
        if (!"light".equals(theme) && !"dark".equals(theme)) {
            throw new IllegalArgumentException("Unsupported theme");
        }
        themePreference = theme;
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

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getThemePreference() {
        return themePreference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<RoleEntity> getRoles() {
        return Set.copyOf(roles);
    }
}
