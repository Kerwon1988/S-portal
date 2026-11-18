package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.identity.domain.MfaFactor;
import com.simbio.core.shared.spi.SensitiveDataProtector;
import org.junit.jupiter.api.Test;

class MfaServiceTest {

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
}
