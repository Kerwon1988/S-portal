package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.identity.domain.MfaFactor;
import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.shared.spi.SensitiveDataProtector;
import org.junit.jupiter.api.Test;

class MfaServiceTest {

    @Test
    void startsEnrollmentWithProtectedSecretAndReusesPendingSecret() {
        Instant now = Instant.ofEpochSecond(59);
        UUID userId = UUID.randomUUID();
        MfaFactorStore factors = mock(MfaFactorStore.class);
        UserAccountStore accounts = mock(UserAccountStore.class);
        UserAccountEntity account = new UserAccountEntity(userId, "synthetic@example.test", "hash", "en",
                now, new RoleEntity("ADMIN", "Administrator"));
        when(accounts.findByPublicId(userId)).thenReturn(Optional.of(account));
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        when(protector.protect(org.mockito.ArgumentMatchers.anyString())).thenReturn("protected-secret");
        MfaService service = service(factors, accounts, protector, now);

        var enrollment = service.beginEnrollment(userId);

        assertThat(enrollment.secret()).isNotBlank();
        assertThat(enrollment.provisioningUri()).contains("synthetic%40example.test", "issuer=Simbio");
        org.mockito.ArgumentCaptor<MfaFactor> saved = org.mockito.ArgumentCaptor.forClass(MfaFactor.class);
        verify(factors).save(saved.capture());
        assertThat(saved.getValue().getSecretEncrypted()).isEqualTo("protected-secret");
        assertThat(saved.getValue().isEnabled()).isFalse();

        MfaFactor pending = new MfaFactor(userId, "protected-secret", now);
        when(factors.find(userId)).thenReturn(Optional.of(pending));
        when(protector.reveal("protected-secret")).thenReturn("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");

        assertThat(service.beginEnrollment(userId).secret())
                .isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        verify(factors, org.mockito.Mockito.times(1)).save(org.mockito.ArgumentMatchers.any(MfaFactor.class));
    }

    @Test
    void refusesToStartAnAlreadyEnabledFactor() {
        Instant now = Instant.ofEpochSecond(59);
        UUID userId = UUID.randomUUID();
        MfaFactor enabled = new MfaFactor(userId, "protected-secret", now);
        enabled.enable("", 0, now);
        MfaFactorStore factors = mock(MfaFactorStore.class);
        UserAccountStore accounts = mock(UserAccountStore.class);
        when(accounts.findByPublicId(userId)).thenReturn(Optional.of(
                new UserAccountEntity(userId, "synthetic@example.test", "hash", "es", now,
                        new RoleEntity("ADMIN", "Administrator"))));
        when(factors.find(userId)).thenReturn(Optional.of(enabled));

        assertThatThrownBy(() -> service(factors, accounts, mock(SensitiveDataProtector.class), now)
                .beginEnrollment(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MFA is already enabled");
    }

    @Test
    void confirmsEnrollmentAndRegeneratesRecoveryCodesOnlyWithValidTotp() {
        Instant now = Instant.ofEpochSecond(59);
        UUID userId = UUID.randomUUID();
        MfaFactor factor = new MfaFactor(userId, "protected-secret", now);
        MfaFactorStore factors = mock(MfaFactorStore.class);
        when(factors.find(userId)).thenReturn(Optional.of(factor));
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        when(protector.reveal("protected-secret"))
                .thenReturn("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        AuditRecorder audit = mock(AuditRecorder.class);
        MfaService service = new MfaService(factors, mock(UserAccountStore.class), protector,
                new IdentityProperties(), audit, new java.security.SecureRandom(),
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.confirmEnrollment(userId, "invalid")).isEmpty();
        List<String> recoveryCodes = service.confirmEnrollment(userId, "287082");
        assertThat(recoveryCodes).hasSize(10).doesNotContainNull();
        assertThat(factor.isEnabled()).isTrue();
        verify(audit).record(org.mockito.ArgumentMatchers.any());

        assertThat(service.regenerateRecoveryCodes(userId, "invalid")).isEmpty();
        List<String> regenerated = service.regenerateRecoveryCodes(userId, "359152");
        assertThat(regenerated).hasSize(10).doesNotContainAnyElementsOf(recoveryCodes);
        verify(audit).record(org.mockito.ArgumentMatchers.argThat(record ->
                record.actionCode().equals("MFA_RECOVERY_CODES_REGENERATED")));
    }

    @Test
    void returnsFalseForMissingDisabledOrMalformedFactors() {
        Instant now = Instant.ofEpochSecond(59);
        UUID userId = UUID.randomUUID();
        MfaFactorStore factors = mock(MfaFactorStore.class);
        MfaService service = service(factors, mock(UserAccountStore.class),
                mock(SensitiveDataProtector.class), now);

        when(factors.find(userId)).thenReturn(Optional.empty());
        assertThat(service.verifyLogin(userId, "123456")).isFalse();
        assertThat(service.regenerateRecoveryCodes(userId, "123456")).isEmpty();
        assertThat(service.isEnrolled(userId)).isFalse();

        MfaFactor pending = new MfaFactor(userId, "protected-secret", now);
        when(factors.find(userId)).thenReturn(Optional.of(pending));
        assertThat(service.verifyLogin(userId, "123456")).isFalse();
        assertThat(service.regenerateRecoveryCodes(userId, "123456")).isEmpty();
        assertThat(service.confirmEnrollment(userId, "invalid")).isEmpty();
        assertThat(service.isEnrolled(userId)).isFalse();

        pending.enable("", 0, now);
        when(factors.find(userId)).thenReturn(Optional.of(pending));
        assertThat(service.verifyLogin(userId, "x".repeat(65))).isFalse();
        assertThat(service.regenerateRecoveryCodes(userId, "invalid")).isEmpty();
        assertThat(service.isEnrolled(userId)).isTrue();
    }

    @Test
    void acceptsCurrentTotpStepOnlyOnce() {
        Instant now = Instant.ofEpochSecond(59);
        UUID userId = UUID.randomUUID();
        MfaFactorStore factors = mock(MfaFactorStore.class);
        MfaFactor factor = new MfaFactor(userId, "protected-secret", now);
        factor.enable("", 0, now);
        when(factors.find(userId)).thenReturn(Optional.of(factor));
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        when(protector.reveal("protected-secret"))
                .thenReturn("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        MfaService service = new MfaService(
                factors,
                mock(UserAccountStore.class),
                protector,
                new IdentityProperties(),
                mock(AuditRecorder.class),
                new java.security.SecureRandom(),
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.verifyLogin(userId, "287082")).isTrue();
        assertThat(service.verifyLogin(userId, "287082")).isFalse();
        assertThat(factor.getLastAcceptedStep()).isEqualTo(1);
        verify(factors).save(factor);
    }

    @Test
    void recoveryCodeIsConsumedAfterSingleUse() {
        Instant now = Instant.parse("2026-11-18T10:00:00Z");
        UUID userId = UUID.randomUUID();
        MfaFactorStore factors = mock(MfaFactorStore.class);
        MfaFactor factor = new MfaFactor(userId, "protected-secret", now);
        factor.enable(Totp.recoveryHash("synthetic-recovery-code"), 0, now);
        when(factors.find(userId)).thenReturn(Optional.of(factor));
        SensitiveDataProtector protector = mock(SensitiveDataProtector.class);
        when(protector.reveal("protected-secret"))
                .thenReturn("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        MfaService service = new MfaService(
                factors,
                mock(UserAccountStore.class),
                protector,
                new IdentityProperties(),
                mock(AuditRecorder.class),
                new java.security.SecureRandom(),
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(service.verifyLogin(userId, "synthetic-recovery-code")).isTrue();
        assertThat(service.verifyLogin(userId, "synthetic-recovery-code")).isFalse();
        verify(factors).save(factor);
    }

    private static MfaService service(
            MfaFactorStore factors, UserAccountStore accounts, SensitiveDataProtector protector, Instant now) {
        return new MfaService(factors, accounts, protector, new IdentityProperties(),
                mock(AuditRecorder.class), new java.security.SecureRandom(),
                Clock.fixed(now, ZoneOffset.UTC));
    }
}
