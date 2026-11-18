package com.simbio.core.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.customer.api.ClientRegistered;
import com.simbio.core.customer.api.CustomerAccountCreator;
import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerPhoneManager;
import com.simbio.core.customer.api.CustomerTaxIdentifierManager;
import com.simbio.core.identity.api.CustomerRegistrationIssuer;
import com.simbio.core.legal.api.ConsentDocumentVersions;
import com.simbio.core.legal.api.ConsentRecorder;
import com.simbio.core.legal.api.ConsentSource;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.AntiBotVerification;

class CustomerRegistrationServiceTest {

    private final AntiBotVerification antiBot = org.mockito.Mockito.mock(AntiBotVerification.class);
    private final CustomerRegistrationIssuer identity = org.mockito.Mockito.mock(CustomerRegistrationIssuer.class);
    private final CustomerAccountCreator accounts = org.mockito.Mockito.mock(CustomerAccountCreator.class);
    private final CustomerPhoneManager phones = org.mockito.Mockito.mock(CustomerPhoneManager.class);
    private final CustomerTaxIdentifierManager taxIds = org.mockito.Mockito.mock(CustomerTaxIdentifierManager.class);
    private final ConsentRecorder consents = org.mockito.Mockito.mock(ConsentRecorder.class);
    private final ApplicationEventPublisher events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    private final CustomerRegistrationService service = new CustomerRegistrationService(
            antiBot, identity, accounts, phones, taxIds,
            List.of(new SpainTaxIdentifierValidator()), consents, events);

    @Test
    void rejectsBotBeforeCreatingIdentityOrCustomerData() {
        when(antiBot.verify("invalid", "", false)).thenReturn(false);

        assertThat(service.register(command(false, "", ""))).isEqualTo(CustomerRegistrationOutcome.REJECTED);

        verify(identity, never()).registerCustomer(any(), any(), any(), any());
        verify(accounts, never()).createOwnerAccount(any(), any(), any());
        verify(consents, never()).recordRequiredAcceptances(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void createsOwnerRecordsLegalVersionsAndPublishesIdsAfterCommit() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(antiBot.verify("valid", "", false)).thenReturn(true);
        when(consents.currentDocumentVersions("es")).thenReturn(new ConsentDocumentVersions(3, 5, 0));
        when(identity.registerCustomer("synthetic@example.test", "long-enough-password",
                "Synthetic person", "es")).thenReturn(Optional.of(userId));
        when(accounts.createOwnerAccount(userId, "INDIVIDUAL", "Synthetic person"))
                .thenReturn(new CustomerAccountSummary(accountId, "INDIVIDUAL", "Synthetic person",
                        "ACTIVE", Instant.parse("2026-11-18T08:00:00Z")));
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThat(service.register(command(false, "", ""))).isEqualTo(CustomerRegistrationOutcome.SUBMITTED);
            verify(consents).recordRequiredAcceptances(
                    new AuthenticatedUser(userId, Role.CLIENT, java.util.Set.of()),
                    "es", ConsentSource.REGISTRATION, 3, 5);
            verify(consents, never()).recordMarketingAcceptance(any(), any(), anyInt());
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCommit());
            verify(events).publishEvent(new ClientRegistered(accountId, userId));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void existingEmailUsesGenericOutcomeWithoutCreatingAccountOrConsent() {
        when(antiBot.verify("valid", "", false)).thenReturn(true);
        when(consents.currentDocumentVersions("es")).thenReturn(new ConsentDocumentVersions(3, 5, 0));
        when(identity.registerCustomer("synthetic@example.test", "long-enough-password",
                "Synthetic person", "es")).thenReturn(Optional.empty());

        assertThat(service.register(command(false, "", ""))).isEqualTo(CustomerRegistrationOutcome.SUBMITTED);

        verify(accounts, never()).createOwnerAccount(any(), any(), any());
        verify(consents, never()).recordRequiredAcceptances(
                any(), any(), any(), anyInt(), anyInt());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void rejectsInvalidTaxIdentifierBeforeCreatingIdentity() {
        when(antiBot.verify("valid", "", false)).thenReturn(true);

        assertThatThrownBy(() -> service.register(command(false, "", "INVALID")))
                .isInstanceOfSatisfying(InvalidCustomerRegistrationException.class,
                        exception -> assertThat(exception.field()).isEqualTo("taxIdentifier"));

        verify(identity, never()).registerCustomer(any(), any(), any(), any());
    }

    private static CustomerRegistrationCommand command(
            boolean marketingAccepted, String phone, String taxIdentifier) {
        return new CustomerRegistrationCommand("synthetic@example.test", "long-enough-password",
                "Synthetic person", "", "INDIVIDUAL", "es", phone, "ES", taxIdentifier,
                true, true, marketingAccepted, 3, 5, 0, "valid", "", false);
    }
}
