package com.simbio.core.customer.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.domain.ClientAccountEntity;
import com.simbio.core.customer.domain.ClientAccountType;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.SensitiveDataProtector;

class CustomerPhoneServiceTest {

    private final ClientAccountStore store = org.mockito.Mockito.mock(ClientAccountStore.class);
    private final SensitiveDataProtector protector = org.mockito.Mockito.mock(SensitiveDataProtector.class);
    private final AuditRecorder auditRecorder = org.mockito.Mockito.mock(AuditRecorder.class);
    private final CustomerPhoneService service = new CustomerPhoneService(store, protector, auditRecorder);

    @Test
    void storesOnlyProtectedE164PhoneForTheOwningClient() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        ClientAccountEntity account = new ClientAccountEntity(accountId, ClientAccountType.INDIVIDUAL,
                "Synthetic account", Instant.parse("2026-11-18T08:00:00Z"));
        when(store.findOwnedAccountBy(accountId, userId)).thenReturn(Optional.of(account));
        when(protector.protect("+34123456789")).thenReturn("v1:synthetic-phone-ciphertext");

        service.setForOwnedAccount(new AuthenticatedUser(userId, Role.CLIENT, Set.of()), accountId, "+34123456789");

        verify(store).save(account);
        org.assertj.core.api.Assertions.assertThat(account.getPhoneCiphertext())
                .isEqualTo("v1:synthetic-phone-ciphertext")
                .doesNotContain("+34123456789");
        verify(auditRecorder).record(any());
    }

    @Test
    void rejectsNonE164AndNonOwnerRequestsWithoutStoringPhone() {
        UUID ownerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        assertThatThrownBy(() -> service.setForOwnedAccount(
                new AuthenticatedUser(ownerId, Role.CLIENT, Set.of()), accountId, "123456789"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.setForOwnedAccount(
                new AuthenticatedUser(ownerId, Role.EMPLOYEE, Set.of()), accountId, "+34123456789"))
                .isInstanceOf(ClientAccountNotFoundException.class);

        verify(protector, never()).protect(any());
        verify(store, never()).save(any(ClientAccountEntity.class));
    }
}
