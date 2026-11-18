package com.simbio.core.identity.application;

import java.util.Locale;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.InvitationIssuer;
import com.simbio.core.identity.api.IdentityAccountChanged;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailSender;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class AdminUserActions {

    private final UserAccountStore accounts;
    private final PasswordRecoveryService passwordRecovery;
    private final InvitationIssuer invitations;
    private final AuditRecorder auditRecorder;
    private final ApplicationEventPublisher events;
    private final SessionInvalidator sessions;
    private final EmailSender emailSender;

    public AdminUserActions(
            UserAccountStore accounts,
            PasswordRecoveryService passwordRecovery,
            InvitationIssuer invitations,
            AuditRecorder auditRecorder,
            ApplicationEventPublisher events,
            SessionInvalidator sessions,
            EmailSender emailSender) {
        this.accounts = accounts;
        this.passwordRecovery = passwordRecovery;
        this.invitations = invitations;
        this.auditRecorder = auditRecorder;
        this.events = events;
        this.sessions = sessions;
        this.emailSender = emailSender;
    }

    @Transactional
    public void setActive(AuthenticatedUser actor, UUID userPublicId, boolean active) {
        requireAdmin(actor, Permission.USERS_MANAGE);
        UserAccountEntity target = requireAccount(userPublicId);
        if (active == "ACTIVE".equals(target.getStatus())) {
            return;
        }
        if (!active && isAdmin(target) && "ACTIVE".equals(target.getStatus())) {
            preventLastAdminRemoval();
        }
        if (!active && actor.userId().equals(userPublicId)) {
            throw new IllegalArgumentException("Administrators cannot deactivate their own account");
        }
        target.setActive(active);
        accounts.save(target);
        auditRecorder.record(new AuditRecord(actor.userId(),
                active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", userPublicId, null));
        notifyAccountSecurityChange(target);
        if (!active) {
            invalidateSessionsAfterCommit(userPublicId);
        }
        events.publishEvent(new IdentityAccountChanged(userPublicId, roleCode(target), target.getStatus()));
    }

    @Transactional
    public void unlock(AuthenticatedUser actor, UUID userPublicId) {
        requireAdmin(actor, Permission.USERS_MANAGE);
        UserAccountEntity target = requireAccount(userPublicId);
        target.unlock();
        accounts.save(target);
        auditRecorder.record(new AuditRecord(actor.userId(), "USER_UNLOCKED", "USER", userPublicId, null));
        notifyAccountSecurityChange(target);
    }

    @Transactional
    public void changeRole(AuthenticatedUser actor, UUID userPublicId, String roleCode) {
        Role newRole;
        try {
            newRole = Role.valueOf(roleCode);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Unsupported account role", exception);
        }
        requireAdmin(actor, Permission.USERS_MANAGE);
        UserAccountEntity target = requireAccount(userPublicId);
        String currentRole = roleCode(target);
        if (currentRole.equals(newRole.name())) {
            return;
        }
        if ("CLIENT".equals(currentRole) || newRole == Role.CLIENT) {
            throw new IllegalArgumentException("Client role changes require a customer-account workflow");
        }
        requireAdmin(actor, Permission.STAFF_MANAGE);
        boolean targetIsAdmin = isAdmin(target);
        if (targetIsAdmin && newRole != Role.ADMIN) {
            if ("ACTIVE".equals(target.getStatus())) {
                preventLastAdminRemoval();
            }
            if (actor.userId().equals(userPublicId)) {
                throw new IllegalArgumentException("Administrators cannot demote themselves");
            }
        }
        target.replaceRole(accounts.findRole(newRole.name()));
        accounts.save(target);
        auditRecorder.record(new AuditRecord(actor.userId(), "USER_ROLE_CHANGED", "USER", userPublicId, null));
        notifyAccountSecurityChange(target);
        events.publishEvent(new IdentityAccountChanged(userPublicId, newRole.name(), target.getStatus()));
    }

    @Transactional
    public void sendPasswordReset(AuthenticatedUser actor, UUID userPublicId) {
        requireAdmin(actor, Permission.USERS_MANAGE);
        UserAccountEntity target = requireAccount(userPublicId);
        passwordRecovery.requestReset(target.getEmailNormalized());
        auditRecorder.record(new AuditRecord(actor.userId(), "PASSWORD_RESET_REQUESTED", "USER", userPublicId, null));
    }

    @Transactional
    public boolean sendInvitation(AuthenticatedUser actor, String email, String roleCode, Locale locale) {
        requireAdmin(actor, "EMPLOYEE".equals(roleCode) ? Permission.STAFF_MANAGE : Permission.USERS_MANAGE);
        return invitations.invite(actor, email, roleCode, locale);
    }

    private UserAccountEntity requireAccount(UUID userPublicId) {
        return accounts.findByPublicId(userPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Account is unavailable"));
    }

    private void preventLastAdminRemoval() {
        if (accounts.lockActiveAdmins().size() <= 1) {
            throw new IllegalArgumentException("The last active administrator cannot be removed");
        }
    }

    private static boolean isAdmin(UserAccountEntity account) {
        return account.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode()));
    }

    private static String roleCode(UserAccountEntity account) {
        return account.getRoles().stream().map(role -> role.getCode()).sorted()
                .findFirst().orElseThrow(() -> new IllegalStateException("Account has no role"));
    }

    private void notifyAccountSecurityChange(UserAccountEntity account) {
        emailSender.enqueue(EmailCommand.toUser(account.getPublicId(), EmailTemplate.ACCOUNT_SECURITY_CHANGED,
                Locale.forLanguageTag("en".equals(account.getPreferredLocale()) ? "en" : "es"), java.util.Map.of()));
    }

    private static void requireAdmin(AuthenticatedUser actor, Permission permission) {
        if (actor == null || actor.role() != Role.ADMIN || !actor.hasPermission(permission)) {
            throw new AccessDeniedException("User management permission required");
        }
    }

    private void invalidateSessionsAfterCommit(UUID userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Session invalidation requires an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sessions.invalidate(userId);
            }
        });
    }
}
