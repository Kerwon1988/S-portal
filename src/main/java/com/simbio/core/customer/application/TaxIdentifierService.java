package com.simbio.core.customer.application;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.api.CustomerTaxIdentifierManager;
import com.simbio.core.customer.domain.ClientAccountEntity;
import com.simbio.core.customer.domain.TaxIdentifier;
import com.simbio.core.customer.domain.TaxIdentifierValidator;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.SensitiveDataProtector;

@Service
public class TaxIdentifierService implements CustomerTaxIdentifierManager {

    private final ClientAccountStore store;
    private final List<TaxIdentifierValidator> validators;
    private final SensitiveDataProtector protector;
    private final AuditRecorder auditRecorder;

    public TaxIdentifierService(ClientAccountStore store, List<TaxIdentifierValidator> validators,
            SensitiveDataProtector protector, AuditRecorder auditRecorder) {
        this.store = Objects.requireNonNull(store);
        this.validators = List.copyOf(validators);
        this.protector = Objects.requireNonNull(protector);
        this.auditRecorder = Objects.requireNonNull(auditRecorder);
    }

    @Override
    @Transactional
    public String setForOwnedAccount(AuthenticatedUser actor, UUID accountPublicId,
            String countryCode, String taxId) {
        requireClientOwner(actor, accountPublicId);
        String normalizedCountry = normalizeCountryCode(countryCode);
        TaxIdentifierValidator validator = validators.stream()
                .filter(candidate -> candidate.countryCode().equals(normalizedCountry))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported tax identifier country"));
        TaxIdentifier identifier = new TaxIdentifier(normalizedCountry, TaxIdentifier.normalize(taxId));
        if (!validator.accepts(identifier.normalizedValue())) {
            throw new IllegalArgumentException("Tax identifier is invalid");
        }

        ClientAccountEntity account = ownedAccount(actor, accountPublicId);
        account.setTaxIdentifier(identifier.countryCode(), protector.protect(identifier.normalizedValue()));
        store.save(account);
        auditRecorder.record(new AuditRecord(actor.userId(), "CLIENT_TAX_IDENTIFIER_SET",
                "CLIENT_ACCOUNT", account.getPublicId(), null));
        return identifier.masked();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> maskedForOwnedAccount(AuthenticatedUser actor, UUID accountPublicId) {
        requireClientOwner(actor, accountPublicId);
        ClientAccountEntity account = ownedAccount(actor, accountPublicId);
        if (account.getTaxIdCiphertext() == null) {
            return Optional.empty();
        }
        return Optional.of(new TaxIdentifier(account.getCountryCode(),
                protector.reveal(account.getTaxIdCiphertext())).masked());
    }

    private ClientAccountEntity ownedAccount(AuthenticatedUser actor, UUID accountPublicId) {
        return store.findOwnedAccountBy(accountPublicId, actor.userId())
                .orElseThrow(ClientAccountNotFoundException::new);
    }

    private static void requireClientOwner(AuthenticatedUser actor, UUID accountPublicId) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(accountPublicId, "accountPublicId must not be null");
        if (actor.role() != Role.CLIENT) {
            throw new ClientAccountNotFoundException();
        }
    }

    private static String normalizeCountryCode(String countryCode) {
        String normalized = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Unsupported tax identifier country");
        }
        return normalized;
    }
}
