package com.simbio.core.identity.application;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.api.CustomerRegistrationIssuer;
import com.simbio.core.shared.value.EmailAddress;

@Service("identityCustomerRegistrationService")
public class CustomerRegistrationService implements CustomerRegistrationIssuer {

    private final UserAccountStore accounts;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public CustomerRegistrationService(
            UserAccountStore accounts,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.accounts = accounts;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    @Transactional
    public Optional<UUID> registerCustomer(String email, String password, String displayName, String locale) {
        EmailAddress address = EmailAddress.of(email);
        String normalizedLocale = normalizeLocale(locale);
        if (displayName == null || displayName.isBlank() || displayName.trim().length() > 120) {
            throw new IllegalArgumentException("Display name must contain 1 to 120 characters");
        }
        passwordPolicy.requireAccepted(password);
        if (accounts.findByEmail(address.value().toLowerCase(Locale.ROOT)).isPresent()) {
            return Optional.empty();
        }

        var account = accounts.createUnverifiedUser(address.value(), passwordEncoder.encode(password),
                normalizedLocale, "CLIENT", displayName.trim());
        emailVerificationService.sendVerification(account.getPublicId());
        return Optional.of(account.getPublicId());
    }

    private static String normalizeLocale(String locale) {
        String normalized = locale == null ? "" : locale.toLowerCase(Locale.ROOT);
        if (!normalized.equals("es") && !normalized.equals("en")) {
            throw new IllegalArgumentException("Unsupported registration locale");
        }
        return normalized;
    }
}
