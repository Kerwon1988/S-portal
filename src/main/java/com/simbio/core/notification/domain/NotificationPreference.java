package com.simbio.core.notification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.simbio.core.notification.api.NotificationCategory;

@Entity
@Table(name = "notification_preference", schema = "notification",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_preference_user_category",
                columnNames = {"user_id", "category"}))
public class NotificationPreference {

    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationCategory category;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationPreference() {
    }

    public NotificationPreference(UUID userId, NotificationCategory category, boolean enabled, Instant updatedAt) {
        this(UUID.randomUUID(), userId, category, enabled, updatedAt);
    }

    NotificationPreference(
            UUID id, UUID userId, NotificationCategory category, boolean enabled, Instant updatedAt) {
        if (userId == null || category == null || category.isMandatory() || updatedAt == null) {
            throw new IllegalArgumentException("Optional preference requires a user, category and update time");
        }
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

    public void update(boolean enabled, Instant updatedAt) {
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
