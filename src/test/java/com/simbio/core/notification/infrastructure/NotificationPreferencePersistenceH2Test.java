package com.simbio.core.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.notification.api.NotificationCategory;
import com.simbio.core.notification.domain.NotificationPreference;

@DataJpaTest
@ActiveProfiles("test")
class NotificationPreferencePersistenceH2Test {

    @Autowired
    private NotificationPreferenceJpaRepository repository;

    @Test
    void storesOneOwnerSpecificPreferencePerOptionalCategory() {
        UUID owner = UUID.randomUUID();
        repository.saveAndFlush(new NotificationPreference(
                owner, NotificationCategory.REQUEST_UPDATES, false,
                Instant.parse("2026-11-20T10:00:00Z")));

        assertThat(repository.findByUserIdAndCategory(owner, NotificationCategory.REQUEST_UPDATES))
                .get().extracting(NotificationPreference::isEnabled).isEqualTo(false);
        assertThat(repository.findByUserIdAndCategory(UUID.randomUUID(), NotificationCategory.REQUEST_UPDATES))
                .isEmpty();
    }
}
