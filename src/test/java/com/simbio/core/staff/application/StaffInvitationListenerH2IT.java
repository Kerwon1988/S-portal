package com.simbio.core.staff.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.simbio.core.identity.api.InvitationAccepted;
import com.simbio.core.identity.api.IdentityAccountChanged;
import com.simbio.core.staff.domain.StaffProfileEntity;

@SpringBootTest
@ActiveProfiles("test")
class StaffInvitationListenerH2IT {

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private StaffProfileStore profiles;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    @DisplayName("TC-STF-001: committed employee invitation creates a persisted staff profile")
    void createsProfileAfterInvitationTransactionCommits() {
        UUID userId = UUID.randomUUID();

        transactions.executeWithoutResult(status ->
                events.publishEvent(new InvitationAccepted(userId, "EMPLOYEE")));

        assertThat(profiles.find(userId))
                .get()
                .extracting(StaffProfileEntity::getStatus)
                .isEqualTo("ACTIVE");
        assertThat(profiles.specialties(userId)).isEmpty();
    }

    @Test
    @DisplayName("TC-STF-001: accepted client invitation does not create a staff profile")
    void doesNotCreateProfileForClientInvitation() {
        UUID userId = UUID.randomUUID();

        transactions.executeWithoutResult(status ->
                events.publishEvent(new InvitationAccepted(userId, "CLIENT")));

        assertThat(profiles.find(userId)).isEmpty();
    }

    @Test
    @DisplayName("TC-STF-001: rolled-back invitation transaction does not create a staff profile")
    void doesNotCreateProfileWhenInvitationTransactionRollsBack() {
        UUID userId = UUID.randomUUID();

        transactions.executeWithoutResult(status -> {
            events.publishEvent(new InvitationAccepted(userId, "EMPLOYEE"));
            status.setRollbackOnly();
        });

        assertThat(profiles.find(userId)).isEmpty();
    }

    @Test
    @DisplayName("TC-STF-001: repeated committed employee invitation preserves the existing profile")
    void repeatedInvitationPreservesExistingProfile() {
        UUID userId = UUID.randomUUID();
        InvitationAccepted invitation = new InvitationAccepted(userId, "EMPLOYEE");

        transactions.executeWithoutResult(status -> events.publishEvent(invitation));
        var firstProfile = profiles.find(userId).orElseThrow();

        transactions.executeWithoutResult(status -> events.publishEvent(invitation));

        assertThat(profiles.find(userId))
                .get()
                .satisfies(profile -> {
                    assertThat(profile.getCreatedAt()).isEqualTo(firstProfile.getCreatedAt());
                    assertThat(profile.getStatus()).isEqualTo("ACTIVE");
                });
    }

    @Test
    @DisplayName("TC-STF-001: active ADMIN account change creates the initial staff profile")
    void createsInitialProfileForActiveAdmin() {
        UUID userId = UUID.randomUUID();

        transactions.executeWithoutResult(status ->
                events.publishEvent(new IdentityAccountChanged(userId, "ADMIN", "ACTIVE")));

        assertThat(profiles.find(userId))
                .get()
                .extracting(StaffProfileEntity::getStatus)
                .isEqualTo("ACTIVE");
        assertThat(profiles.specialties(userId)).isEmpty();
    }

    @Test
    @DisplayName("TC-STF-001: inactive employee account change deactivates the staff profile")
    void deactivatesProfileForInactiveEmployee() {
        UUID userId = UUID.randomUUID();

        transactions.executeWithoutResult(status ->
                events.publishEvent(new InvitationAccepted(userId, "EMPLOYEE")));
        transactions.executeWithoutResult(status -> {
            StaffProfileEntity profile = profiles.find(userId).orElseThrow();
            profile.update("Synthetic engineer", "ACTIVE");
            profiles.save(profile);
        });
        transactions.executeWithoutResult(status ->
                events.publishEvent(new IdentityAccountChanged(userId, "EMPLOYEE", "INACTIVE")));

        assertThat(profiles.find(userId))
                .get()
                .satisfies(profile -> {
                    assertThat(profile.getStatus()).isEqualTo("INACTIVE");
                    assertThat(profile.getJobTitle()).isEqualTo("Synthetic engineer");
                });
    }
}
