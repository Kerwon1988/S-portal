package com.simbio.core.identity.application;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.identity.api.StaffAccountAccessManager;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class StaffAccountAccessService implements StaffAccountAccessManager {

    private final UserAccountStore accounts;
    private final AuditRecorder auditRecorder;
    private final SessionInvalidator sessions;

    public StaffAccountAccessService(
            UserAccountStore accounts,
            AuditRecorder auditRecorder,
            SessionInvalidator sessions) {
        this.accounts = accounts;
        this.auditRecorder = auditRecorder;
        this.sessions = sessions;
    }

    @Override
    @Transactional
    public void setActive(AuthenticatedUser actor, UUID employeeUserId, boolean active) {
        if (actor == null || actor.role() != Role.ADMIN || !actor.hasPermission(Permission.STAFF_MANAGE)) {
            throw new AccessDeniedException("Worker account management permission required");
        }
        UserAccountEntity account = accounts.findByPublicId(employeeUserId)
                .orElseThrow(() -> new IllegalArgumentException("Worker account is unavailable"));
        boolean employee = account.getRoles().stream().anyMatch(role ->
                "EMPLOYEE".equals(role.getCode()) || "ADMIN".equals(role.getCode()));
        if (!employee) {
            throw new IllegalArgumentException("Only staff accounts can be managed as workers");
        }
        boolean targetIsAdmin = account.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode()));
        if (!active && targetIsAdmin && "ACTIVE".equals(account.getStatus())) {
            if (actor.userId().equals(employeeUserId)) {
                throw new IllegalArgumentException("Administrators cannot deactivate their own account");
            }
            if (accounts.lockActiveAdmins().size() <= 1) {
                throw new IllegalArgumentException("The last active administrator cannot be deactivated");
            }
        }
        if (active == "ACTIVE".equals(account.getStatus())) {
            return;
        }
        account.setActive(active);
        accounts.save(account);
        auditRecorder.record(new AuditRecord(actor.userId(),
                active ? "STAFF_ACCOUNT_ACTIVATED" : "STAFF_ACCOUNT_DEACTIVATED",
                "USER", employeeUserId, null));
        if (!active) {
            invalidateSessionsAfterCommit(employeeUserId);
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
