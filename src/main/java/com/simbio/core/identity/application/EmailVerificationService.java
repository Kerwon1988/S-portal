package com.simbio.core.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.EmailVerified;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.shared.value.EmailAddress;

@Service
public class EmailVerificationService {

    private final UserAccountStore accounts;
    private final OneTimeTokenService tokens;
    private final EmailSender emailSender;
    private final IdentityProperties properties;
    private final ApplicationEventPublisher events;
    private final AuditRecorder auditRecorder;
    private final String baseUrl;
    private final Clock clock;

    @Autowired
    public EmailVerificationService(
            UserAccountStore accounts,
            OneTimeTokenService tokens,
            EmailSender emailSender,
            IdentityProperties properties,
            ApplicationEventPublisher events,
            AuditRecorder auditRecorder,
            @Value("${portal.base-url}") String baseUrl) {
        this(accounts, tokens, emailSender, properties, events, auditRecorder, baseUrl, Clock.systemUTC());
    }

    EmailVerificationService(
            UserAccountStore accounts,
            OneTimeTokenService tokens,
            EmailSender emailSender,
            IdentityProperties properties,
            ApplicationEventPublisher events,
            AuditRecorder auditRecorder,
            String baseUrl,
            Clock clock) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.emailSender = emailSender;
        this.properties = properties;
        this.events = events;
        this.auditRecorder = auditRecorder;
        this.baseUrl = baseUrl;
        this.clock = clock;
    }

    @Transactional
    public void sendVerification(UUID userId) {
        UserAccountEntity account = accounts.findByPublicId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account is unavailable"));
        if (account.isEmailVerified()) {
            return;
        }
        tokens.revokeForUser(userId, TokenPurpose.EMAIL_VERIFICATION);
        String token = tokens.issueForUser(userId, TokenPurpose.EMAIL_VERIFICATION,
                properties.getEmailVerificationTtl());
        String url = baseUrl + "/verificar-email?token=" + token;
        emailSender.enqueue(EmailCommand.toUser(
                account.getPublicId(),
                EmailTemplate.EMAIL_VERIFICATION,
                locale(account.getPreferredLocale()),
                Map.of("url", url)));
    }

    @Transactional
    public boolean verify(String rawToken) {
        var consumed = tokens.consume(rawToken, TokenPurpose.EMAIL_VERIFICATION);
        if (consumed.isEmpty() || consumed.get().userPublicId() == null) {
            return false;
        }
        UUID userId = consumed.get().userPublicId();
        UserAccountEntity account = accounts.findByPublicId(userId).orElse(null);
        if (account == null) {
            return false;
        }
        account.verifyEmail(Instant.now(clock));
        accounts.save(account);
        auditRecorder.record(new AuditRecord(userId, "EMAIL_VERIFIED", "USER", userId, null));
        publishAfterCommit(new EmailVerified(userId));
        return true;
    }

    @Transactional
    public void resend(String email) {
        if (email == null || email.length() > 320) {
            return;
        }
        accounts.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(account -> !account.isEmailVerified())
                .ifPresent(account -> sendVerification(account.getPublicId()));
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Identity events require an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.publishEvent(event);
            }
        });
    }

    private static Locale locale(String language) {
        return Locale.forLanguageTag("en".equals(language) ? "en" : "es");
    }
}
