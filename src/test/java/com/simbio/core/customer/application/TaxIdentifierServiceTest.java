package com.simbio.core.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.config.crypto.AesGcmSensitiveDataProtector;
import com.simbio.core.config.crypto.EncryptionProperties;
import com.simbio.core.customer.domain.ClientAccountEntity;
import com.simbio.core.customer.domain.ClientAccountType;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.SensitiveDataProtector;

class TaxIdentifierServiceTest {

    private final ClientAccountStore store = org.mockito.Mockito.mock(ClientAccountStore.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final UUID ownerId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final ClientAccountEntity account = new ClientAccountEntity(accountId, ClientAccountType.COMPANY,
            "Synthetic company", Instant.parse("2026-11-18T08:00:00Z"));
    private final SensitiveDataProtector protector = testProtector();
    private final TaxIdentifierService service = new TaxIdentifierService(
            store, List.of(new SpainTaxIdentifierValidator()), protector, auditRecorder);
    private final AuthenticatedUser owner = new AuthenticatedUser(ownerId, Role.CLIENT, Set.of());

    @Test
    void encryptsSpanishTaxIdAndReturnsOnlyMaskedValueForItsOwner() {
        when(store.findOwnedAccountBy(accountId, ownerId)).thenReturn(Optional.of(account));
        when(store.save(account)).thenReturn(account);

        String masked = service.setForOwnedAccount(owner, accountId, "es", "12345678-Z");

        assertThat(masked).isEqualTo("*****678Z");
        assertThat(account.getCountryCode()).isEqualTo("ES");
        assertThat(account.getTaxIdCiphertext()).isNotEqualTo("12345678Z");
        assertThat(protector.reveal(account.getTaxIdCiphertext())).isEqualTo("12345678Z");
        assertThat(service.maskedForOwnedAccount(owner, accountId)).contains(masked);
        verify(auditRecorder).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidTaxIdOrNonOwnerWithoutSaving() {
        when(store.findOwnedAccountBy(accountId, ownerId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.setForOwnedAccount(owner, accountId, "ES", "12345678A"))
                .isInstanceOf(IllegalArgumentException.class);

        AuthenticatedUser otherClient = new AuthenticatedUser(UUID.randomUUID(), Role.CLIENT, Set.of());
        when(store.findOwnedAccountBy(accountId, otherClient.userId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.maskedForOwnedAccount(otherClient, accountId))
                .isInstanceOf(ClientAccountNotFoundException.class);
        verify(store, never()).save(account);
        verify(auditRecorder, never()).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deniesNonClientRolesAndUnsupportedCountries() {
        AuthenticatedUser employee = new AuthenticatedUser(UUID.randomUUID(), Role.EMPLOYEE, Set.of());
        assertThatThrownBy(() -> service.setForOwnedAccount(employee, accountId, "ES", "12345678Z"))
                .isInstanceOf(ClientAccountNotFoundException.class);
        assertThatThrownBy(() -> service.setForOwnedAccount(owner, accountId, "FR", "12345678Z"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(store, never()).save(account);
    }

    private static SensitiveDataProtector testProtector() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setCurrentKeyId("synthetic-test-key");
        properties.setKeys("synthetic-test-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        return new AesGcmSensitiveDataProtector(properties);
    }
}
