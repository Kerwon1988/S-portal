package com.simbio.core.identity.application;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.InvitationAccepted;
import com.simbio.core.identity.api.InvitationIssuer;
import com.simbio.core.identity.configuration.IdentityProperties;
import com.simbio.core.identity.domain.TokenPurpose;
import com.simbio.core.legal.api.ConsentRecorder;
import com.simbio.core.legal.api.ConsentSource;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.value.EmailAddress;

@Service
public class InvitationService implements InvitationIssuer {

    private final UserAccountStore accounts;
    private final OneTimeTokenService tokens;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final IdentityProperties properties;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher events;
    private final ConsentRecorder consentRecorder;
    private final String baseUrl;

    public InvitationService(
            UserAccountStore accounts,
            OneTimeTokenService tokens,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            IdentityProperties properties,
            AuditRecorder auditRecorder,
            ApplicationEventPublisher events,
            ConsentRecorder consentRecorder,
            @Value("${portal.base-url}") String baseUrl) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.properties = properties;
        this.auditRecorder = auditRecorder;
        this.events = events;
        this.consentRecorder = consentRecorder;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public boolean invite(AuthenticatedUser actor, String email, String roleCode, Locale locale) {
        return invite(actor, email, roleCode, locale, null);
    }

    @Override
    @Transactional
    public boolean invite(
            AuthenticatedUser actor,
            String email,
            String roleCode,
            Locale locale,
            UUID invitationReference) {
        Permission permission = "EMPLOYEE".equals(roleCode) ? Permission.STAFF_MANAGE : Permission.USERS_MANAGE;
        if (actor == null || actor.role() != Role.ADMIN || !actor.hasPermission(permission)) {
            throw new org.springframework.security.access.AccessDeniedException("Administrator permission required");
        }
        EmailAddress address = EmailAddress.of(email);
        if (!"CLIENT".equals(roleCode) && !"EMPLOYEE".equals(roleCode)) {
            throw new IllegalArgumentException("Only client and employee invitations are allowed");
        }
        if (accounts.findByEmail(address.value().toLowerCase(Locale.ROOT)).isPresent()) {
            return false;
        }
        tokens.revokeInvitations(address.value());
        String token = tokens.issueInvitation(address.value(), roleCode, invitationReference,
                properties.getInvitationTtl());
        emailSender.enqueue(EmailCommand.toAddress(
                address,
                EmailTemplate.INVITATION,
                Locale.forLanguageTag("en".equals(locale.getLanguage()) ? "en" : "es"),
                Map.of("url", baseUrl + "/invitacion/aceptar?token=" + token)));
        auditRecorder.record(new AuditRecord(actor.userId(), "INVITATION_CREATED", "INVITATION", null, null));
        return true;
    }

    @Transactional
    public boolean accept(
            String rawToken,
            String password,
            String displayName,
            String locale,
            boolean termsAccepted,
            boolean privacyAccepted) {
        if (!termsAccepted || !privacyAccepted) {
            return false;
        }
        if (!passwordPolicy.accepts(password)) {
            return false;
        }
        if (!"es".equals(locale) && !"en".equals(locale)
                || displayName == null || displayName.isBlank() || displayName.length() > 120) {
            throw new IllegalArgumentException("Invalid invitation profile");
        }
        var consumed = tokens.consume(rawToken, TokenPurpose.INVITATION);
        if (consumed.isEmpty() || consumed.get().invitationEmail() == null) {
            return false;
        }
        String email = consumed.get().invitationEmail();
        String role = consumed.get().roleCode();
        if (accounts.findByEmail(email).isPresent()
                || (!"CLIENT".equals(role) && !"EMPLOYEE".equals(role))) {
            return false;
        }
        var account = accounts.createUser(email, passwordEncoder.encode(password), locale, role, displayName.trim());
        UUID userId = account.getPublicId();
        consentRecorder.recordRequiredAcceptances(
                new com.simbio.core.shared.security.AuthenticatedUser(
                        userId, com.simbio.core.shared.security.Role.valueOf(role), java.util.Set.of()),
                locale,
                ConsentSource.ADMIN_INVITATION);
        auditRecorder.record(new AuditRecord(userId, "INVITATION_ACCEPTED", "USER", userId, null));
        publishAfterCommit(new InvitationAccepted(userId, role, consumed.get().invitationReference()));
        return true;
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
}
