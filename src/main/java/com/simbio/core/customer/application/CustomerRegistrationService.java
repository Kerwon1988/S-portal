package com.simbio.core.customer.application;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.customer.api.ClientRegistered;
import com.simbio.core.customer.api.CustomerAccountCreator;
import com.simbio.core.customer.api.CustomerPhoneManager;
import com.simbio.core.customer.api.CustomerTaxIdentifierManager;
import com.simbio.core.customer.domain.ClientAccountType;
import com.simbio.core.customer.domain.TaxIdentifier;
import com.simbio.core.customer.domain.TaxIdentifierValidator;
import com.simbio.core.identity.api.CustomerRegistrationIssuer;
import com.simbio.core.legal.api.ConsentDocumentVersions;
import com.simbio.core.legal.api.ConsentRecorder;
import com.simbio.core.legal.api.ConsentSource;
import com.simbio.core.legal.api.StaleConsentVersionException;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.AntiBotVerification;

@Service
public class CustomerRegistrationService {

    private final AntiBotVerification antiBot;
    private final CustomerRegistrationIssuer identityRegistration;
    private final CustomerAccountCreator accountCreator;
    private final CustomerPhoneManager phoneManager;
    private final CustomerTaxIdentifierManager taxIdentifierManager;
    private final List<TaxIdentifierValidator> taxValidators;
    private final ConsentRecorder consentRecorder;
    private final ApplicationEventPublisher events;

    public CustomerRegistrationService(
            AntiBotVerification antiBot,
            CustomerRegistrationIssuer identityRegistration,
            CustomerAccountCreator accountCreator,
            CustomerPhoneManager phoneManager,
            CustomerTaxIdentifierManager taxIdentifierManager,
            List<TaxIdentifierValidator> taxValidators,
            ConsentRecorder consentRecorder,
            ApplicationEventPublisher events) {
        this.antiBot = Objects.requireNonNull(antiBot);
        this.identityRegistration = Objects.requireNonNull(identityRegistration);
        this.accountCreator = Objects.requireNonNull(accountCreator);
        this.phoneManager = Objects.requireNonNull(phoneManager);
        this.taxIdentifierManager = Objects.requireNonNull(taxIdentifierManager);
        this.taxValidators = List.copyOf(taxValidators);
        this.consentRecorder = Objects.requireNonNull(consentRecorder);
        this.events = Objects.requireNonNull(events);
    }

    @Transactional
    public CustomerRegistrationOutcome register(CustomerRegistrationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (!antiBot.verify(command.altchaPayload(), command.honeypot(), command.accessibleConfirmation())) {
            return CustomerRegistrationOutcome.REJECTED;
        }
        String locale = normalizeLocale(command.locale());
        ClientAccountType accountType = accountType(command.accountType());
        String personName = requireText(command.personName(), 120, "personName");
        String accountName = accountName(command.accountName(), accountType, personName);
        validateContact(command.phoneNumber(), command.countryCode(), command.taxIdentifier());
        if (!command.termsAccepted() || !command.privacyAccepted()) {
            throw new InvalidCustomerRegistrationException("termsAccepted");
        }
        ConsentDocumentVersions currentVersions = consentRecorder.currentDocumentVersions(locale);
        if (currentVersions.termsVersion() != command.termsVersion()
                || currentVersions.privacyVersion() != command.privacyVersion()
                || command.marketingAccepted()
                    && (!currentVersions.hasMarketingDocument()
                        || currentVersions.marketingVersion() != command.marketingVersion())) {
            throw new StaleConsentVersionException();
        }

        var userId = identityRegistration.registerCustomer(
                command.email(), command.password(), personName, locale);
        if (userId.isEmpty()) {
            return CustomerRegistrationOutcome.SUBMITTED;
        }

        var account = accountCreator.createOwnerAccount(userId.get(), accountType.name(), accountName);
        AuthenticatedUser actor = new AuthenticatedUser(userId.get(), Role.CLIENT, Set.of());
        if (command.phoneNumber() != null && !command.phoneNumber().isBlank()) {
            phoneManager.setForOwnedAccount(actor, account.publicId(), command.phoneNumber());
        }
        if (command.taxIdentifier() != null && !command.taxIdentifier().isBlank()) {
            taxIdentifierManager.setForOwnedAccount(
                    actor, account.publicId(), command.countryCode(), command.taxIdentifier());
        }
        consentRecorder.recordRequiredAcceptances(actor, locale, ConsentSource.REGISTRATION,
                command.termsVersion(), command.privacyVersion());
        if (command.marketingAccepted()) {
            consentRecorder.recordMarketingAcceptance(actor, locale, command.marketingVersion());
        }
        publishAfterCommit(new ClientRegistered(account.publicId(), userId.get()));
        return CustomerRegistrationOutcome.SUBMITTED;
    }

    private void validateContact(String phoneNumber, String countryCode, String taxId) {
        if (phoneNumber != null && !phoneNumber.isBlank()
                && !phoneNumber.trim().matches("\\+[1-9][0-9]{0,14}")) {
            throw new InvalidCustomerRegistrationException("phoneNumber");
        }
        if (taxId == null || taxId.isBlank()) {
            return;
        }
        String country = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
        TaxIdentifierValidator validator = taxValidators.stream()
                .filter(candidate -> candidate.countryCode().equals(country))
                .findFirst()
                .orElseThrow(() -> new InvalidCustomerRegistrationException("taxIdentifier"));
        String normalized;
        try {
            normalized = TaxIdentifier.normalize(taxId);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCustomerRegistrationException("taxIdentifier");
        }
        if (!validator.accepts(normalized)) {
            throw new InvalidCustomerRegistrationException("taxIdentifier");
        }
    }

    private static ClientAccountType accountType(String value) {
        try {
            return ClientAccountType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCustomerRegistrationException("accountType");
        }
    }

    private static String accountName(String value, ClientAccountType type, String personName) {
        if (type == ClientAccountType.INDIVIDUAL && (value == null || value.isBlank())) {
            return personName;
        }
        return requireText(value, 160, "accountName");
    }

    private static String requireText(String value, int maximumLength, String field) {
        if (value == null || value.isBlank() || value.trim().length() > maximumLength) {
            throw new InvalidCustomerRegistrationException(field);
        }
        return value.trim();
    }

    private static String normalizeLocale(String locale) {
        String normalized = locale == null ? "" : locale.toLowerCase(Locale.ROOT);
        if (!normalized.equals("es") && !normalized.equals("en")) {
            throw new InvalidCustomerRegistrationException("locale");
        }
        return normalized;
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Customer events require an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.publishEvent(event);
            }
        });
    }
}
