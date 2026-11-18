package com.simbio.core.identity.application;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.shared.value.EmailAddress;

@Service
public class PasswordRecoveryService {

    private final UserAccountStore accounts;
    private final OneTimeTokenService tokens;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final IdentityProperties properties;
    private final SessionInvalidator sessionInvalidator;
    private final AuditRecorder auditRecorder;
    private final String baseUrl;

    public PasswordRecoveryService(
            UserAccountStore accounts,
            OneTimeTokenService tokens,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            IdentityProperties properties,
            SessionInvalidator sessionInvalidator,
            AuditRecorder auditRecorder,
            @Value("${portal.base-url}") String baseUrl) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.properties = properties;
        this.sessionInvalidator = sessionInvalidator;
        this.auditRecorder = auditRecorder;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.length() > 320) {
            return;
        }
        accounts.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(account -> account.isEmailVerified())
                .ifPresent(account -> {
                    UUID userId = account.getPublicId();
                    tokens.revokeForUser(userId, TokenPurpose.PASSWORD_RESET);
                    String token = tokens.issueForUser(userId, TokenPurpose.PASSWORD_RESET,
                            properties.getPasswordRecoveryTtl());
                    emailSender.enqueue(EmailCommand.toUser(
                            userId,
                            EmailTemplate.PASSWORD_RECOVERY,
                            Locale.forLanguageTag("en".equals(account.getPreferredLocale()) ? "en" : "es"),
                            Map.of("url", baseUrl + "/restablecer?token=" + token)));
                });
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (!passwordPolicy.accepts(newPassword)) {
            return false;
        }
        var consumed = tokens.consume(rawToken, TokenPurpose.PASSWORD_RESET);
        if (consumed.isEmpty() || consumed.get().userPublicId() == null) {
            return false;
        }
        UUID userId = consumed.get().userPublicId();
        var account = accounts.findByPublicId(userId).orElse(null);
        if (account == null || !account.isEmailVerified()) {
            return false;
        }
        account.changePassword(passwordEncoder.encode(newPassword));
        accounts.save(account);
        tokens.revokeForUser(userId, TokenPurpose.PASSWORD_RESET);
        sessionInvalidator.invalidate(userId);
        emailSender.enqueue(EmailCommand.toUser(
                userId,
                EmailTemplate.PASSWORD_CHANGED,
                Locale.forLanguageTag("en".equals(account.getPreferredLocale()) ? "en" : "es"),
                Map.of()));
        auditRecorder.record(new AuditRecord(userId, "PASSWORD_RESET", "USER", userId, null));
        return true;
    }

    @Transactional
    public boolean changePassword(UUID userId, String currentPassword, String newPassword) {
        if (!passwordPolicy.accepts(newPassword)) {
            return false;
        }
        var account = accounts.findByPublicId(userId).orElse(null);
        if (account == null || currentPassword == null
                || !passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            return false;
        }
        account.changePassword(passwordEncoder.encode(newPassword));
        accounts.save(account);
        tokens.revokeForUser(userId, TokenPurpose.PASSWORD_RESET);
        sessionInvalidator.invalidate(userId);
        emailSender.enqueue(EmailCommand.toUser(
                userId,
                EmailTemplate.PASSWORD_CHANGED,
                Locale.forLanguageTag("en".equals(account.getPreferredLocale()) ? "en" : "es"),
                Map.of()));
        auditRecorder.record(new AuditRecord(userId, "PASSWORD_CHANGED", "USER", userId, null));
        return true;
    }
}
