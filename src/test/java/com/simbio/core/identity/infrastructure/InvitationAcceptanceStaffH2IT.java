package com.simbio.core.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.simbio.core.identity.application.InvitationService;
import com.simbio.core.identity.application.OneTimeTokenService;
import com.simbio.core.identity.application.UserAccountStore;
import com.simbio.core.legal.api.ConsentRecorder;
import com.simbio.core.legal.api.ConsentSource;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.staff.api.StaffDirectory;

@SpringBootTest
@ActiveProfiles("test")
class InvitationAcceptanceStaffH2IT {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private OneTimeTokenService tokenService;

    @Autowired
    private UserAccountStore accounts;

    @Autowired
    private StaffDirectory staffDirectory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ConsentRecorder consentRecorder;

    @Test
    @DisplayName("TC-STF-001: accepting an employee invitation persists its account and staff profile")
    void acceptingEmployeeInvitationCreatesAccountAndStaffProfileOnce() {
        String email = "worker-" + UUID.randomUUID() + "@example.test";
        String token = tokenService.issueInvitation(email, "EMPLOYEE", Duration.ofMinutes(5));

        assertThat(accounts.findByEmail(email)).isEmpty();
        assertThat(invitationService.accept(
                token, "synthetic-password-123", "Synthetic worker", "en", true, true)).isTrue();

        var account = accounts.findByEmail(email).orElseThrow();
        assertThat(account.getStatus()).isEqualTo("ACTIVE");
        assertThat(account.isEnabled()).isTrue();
        assertThat(account.getRoles()).extracting("code").containsExactly("EMPLOYEE");

        var profile = staffDirectory.findByUserId(account.getPublicId()).orElseThrow();
        assertThat(profile.status()).isEqualTo("ACTIVE");
        assertThat(profile.specialties()).isEmpty();

        assertThat(invitationService.accept(
                token, "synthetic-password-123", "Synthetic worker", "en", true, true)).isFalse();
        assertThat(accounts.findByEmail(email).orElseThrow().getPublicId()).isEqualTo(account.getPublicId());
        assertThat(staffDirectory.findByUserId(account.getPublicId())).contains(profile);
        verify(consentRecorder).recordRequiredAcceptances(
                any(AuthenticatedUser.class), eq("en"), eq(ConsentSource.ADMIN_INVITATION));
    }

    @Test
    @DisplayName("TC-STF-001: an expired employee invitation cannot create an account")
    void expiredInvitationDoesNotCreateAnAccountOrRecordConsent() throws NoSuchAlgorithmException {
        String email = "worker-" + UUID.randomUUID() + "@example.test";
        String token = tokenService.issueInvitation(email, "EMPLOYEE", Duration.ofMinutes(5));
        int updated = jdbcTemplate.update(
                "UPDATE identity.one_time_token SET expires_at = ? WHERE token_hash = ?",
                Timestamp.from(Instant.now().minusSeconds(1)),
                sha256(token));

        assertThat(updated).isEqualTo(1);
        assertThat(invitationService.accept(
                token, "synthetic-password-123", "Synthetic worker", "en", true, true)).isFalse();
        assertThat(accounts.findByEmail(email)).isEmpty();
        verifyNoInteractions(consentRecorder);
    }

    @Test
    @DisplayName("TC-STF-001: failed consent recording rolls back account and token before retry")
    void consentFailureRollsBackAcceptanceAndAllowsRetry() {
        String email = "worker-" + UUID.randomUUID() + "@example.test";
        String token = tokenService.issueInvitation(email, "EMPLOYEE", Duration.ofMinutes(5));
        doThrow(new IllegalStateException("synthetic consent failure"))
                .when(consentRecorder)
                .recordRequiredAcceptances(any(AuthenticatedUser.class),
                        eq("en"), eq(ConsentSource.ADMIN_INVITATION));

        assertThatThrownBy(() -> invitationService.accept(
                token, "synthetic-password-123", "Synthetic worker", "en", true, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("synthetic consent failure");

        ArgumentCaptor<AuthenticatedUser> actorCaptor = ArgumentCaptor.forClass(AuthenticatedUser.class);
        verify(consentRecorder).recordRequiredAcceptances(
                actorCaptor.capture(), eq("en"), eq(ConsentSource.ADMIN_INVITATION));
        UUID rolledBackUserId = actorCaptor.getValue().userId();
        assertThat(accounts.findByEmail(email)).isEmpty();
        assertThat(accounts.findByPublicId(rolledBackUserId)).isEmpty();
        assertThat(staffDirectory.findByUserId(rolledBackUserId)).isEmpty();

        doNothing().when(consentRecorder).recordRequiredAcceptances(
                any(AuthenticatedUser.class), eq("en"), eq(ConsentSource.ADMIN_INVITATION));
        assertThat(invitationService.accept(
                token, "synthetic-password-123", "Synthetic worker", "en", true, true)).isTrue();

        var account = accounts.findByEmail(email).orElseThrow();
        assertThat(staffDirectory.findByUserId(account.getPublicId()))
                .get()
                .satisfies(profile -> {
                    assertThat(profile.status()).isEqualTo("ACTIVE");
                    assertThat(profile.specialties()).isEmpty();
                });
    }

    private static String sha256(String value) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
