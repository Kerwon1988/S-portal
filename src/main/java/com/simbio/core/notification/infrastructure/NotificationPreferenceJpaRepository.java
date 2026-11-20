package com.simbio.core.notification.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.simbio.core.notification.api.NotificationCategory;
import com.simbio.core.notification.domain.NotificationPreference;

interface NotificationPreferenceJpaRepository extends JpaRepository<NotificationPreference, UUID> {

    Optional<NotificationPreference> findByUserIdAndCategory(UUID userId, NotificationCategory category);
}
