package com.simbio.core.staff.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.staff.application.StaffProfileStore;
import com.simbio.core.staff.domain.StaffProfileEntity;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaStaffProfileStore.class)
class StaffPersistenceH2IT {

    @Autowired
    private StaffProfileStore store;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("TC-STF-001: H2 stores an employee profile and its specialties")
    void persistsProfileEmploymentDateAndSpecialties() {
        UUID userId = UUID.randomUUID();
        store.save(new StaffProfileEntity(userId, LocalDate.of(2026, 11, 18),
                Instant.parse("2026-11-18T12:00:00Z")));
        store.replaceSpecialties(userId, List.of("Analysis", "Delivery"));
        entityManager.flush();
        entityManager.clear();

        assertThat(store.find(userId)).get()
                .satisfies(profile -> {
                    assertThat(profile.getEmploymentStartDate()).isEqualTo(LocalDate.of(2026, 11, 18));
                    assertThat(profile.getStatus()).isEqualTo("ACTIVE");
                });
        assertThat(store.specialties(userId)).containsExactly("Analysis", "Delivery");
    }

    @Test
    @DisplayName("TC-STF-001: specialty names are unique per employee without case variants")
    void rejectsDuplicateSpecialtyKeys() {
        UUID userId = UUID.randomUUID();
        store.save(new StaffProfileEntity(userId, LocalDate.of(2026, 11, 18),
                Instant.parse("2026-11-18T12:00:00Z")));
        store.replaceSpecialties(userId, List.of("Analysis"));
        entityManager.flush();

        assertThatThrownBy(() -> {
            store.replaceSpecialties(userId, List.of("Analysis", "analysis"));
            entityManager.flush();
        }).isInstanceOfAny(ConstraintViolationException.class,
                org.springframework.dao.DataIntegrityViolationException.class);
    }
}
