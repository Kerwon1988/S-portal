package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.simbio.core.identity.domain.UserAccountEntity;

class CustomerRegistrationServiceTest {

    private final UserAccountStore accounts = org.mockito.Mockito.mock(UserAccountStore.class);
    private final PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    private final EmailVerificationService verification =
            org.mockito.Mockito.mock(EmailVerificationService.class);
    private final CustomerRegistrationService service = new CustomerRegistrationService(
            accounts, new PasswordPolicy(), passwordEncoder, verification);

    @Test
    void createsAnUnverifiedCustomerAndQueuesEmailVerification() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity account = org.mockito.Mockito.mock(UserAccountEntity.class);
        when(accounts.findByEmail("synthetic@example.test")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("long-enough-password")).thenReturn("encoded-hash");
        when(accounts.createUnverifiedUser("synthetic@example.test", "encoded-hash", "en", "CLIENT",
                "Synthetic person")).thenReturn(account);
        when(account.getPublicId()).thenReturn(userId);

        assertThat(service.registerCustomer(
                "Synthetic@Example.Test", "long-enough-password", " Synthetic person ", "EN"))
                .contains(userId);

        verify(accounts).createUnverifiedUser("synthetic@example.test", "encoded-hash",
                "en", "CLIENT", "Synthetic person");
        verify(verification).sendVerification(userId);
    }

    @Test
    void returnsTheSameGenericOutcomeForAnExistingAddress() {
        when(accounts.findByEmail("synthetic@example.test"))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(UserAccountEntity.class)));

        assertThat(service.registerCustomer(
                "synthetic@example.test", "long-enough-password", "Synthetic person", "es"))
                .isEmpty();

        verify(accounts, never()).createUnverifiedUser(any(), any(), any(), any(), any());
        verify(verification, never()).sendVerification(any());
    }

    @Test
    void rejectsUnsupportedLocaleAndInvalidPasswordBeforeCreatingAnAccount() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.registerCustomer("synthetic@example.test", "short", "Synthetic person", "fr"))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.registerCustomer("synthetic@example.test", "short", "Synthetic person", "es"))
                .isInstanceOf(InvalidPasswordException.class);

        verify(accounts, never()).createUnverifiedUser(any(), any(), any(), any(), any());
    }
}
