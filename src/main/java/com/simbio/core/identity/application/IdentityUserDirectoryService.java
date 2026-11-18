package com.simbio.core.identity.application;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.identity.api.IdentityUserDirectory;
import com.simbio.core.identity.api.IdentityUserSummary;
import com.simbio.core.identity.api.UserDirectoryOrder;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

@Service
public class IdentityUserDirectoryService implements IdentityUserDirectory {

    private final UserAccountStore accounts;

    public IdentityUserDirectoryService(UserAccountStore accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityUserSummary> find(AuthenticatedUser actor, UUID userPublicId) {
        requireUserOrStaffManagement(actor);
        return accounts.findByPublicId(userPublicId).map(IdentityUserDirectoryService::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<IdentityUserSummary> search(
            AuthenticatedUser actor,
            String search,
            String role,
            String status,
            String locale,
            Instant createdFrom,
            Instant createdBefore,
            UserDirectoryOrder order,
            PageQuery page) {
        requireUserManagement(actor);
        if (page == null || order == null) {
            throw new IllegalArgumentException("A page query and directory order are required");
        }
        String normalizedSearch = normalizeSearch(search);
        String normalizedRole = normalizeRole(role);
        String normalizedStatus = normalizeStatus(status);
        String normalizedLocale = normalizeLocale(locale);
        if (createdFrom != null && createdBefore != null && !createdFrom.isBefore(createdBefore)) {
            throw new IllegalArgumentException("Invalid user creation date range");
        }
        String sortProperty = switch (order.field()) {
            case CREATED_AT -> "createdAt";
            case DISPLAY_NAME -> "displayName";
            case EMAIL -> "emailNormalized";
        };
        Sort.Direction sortDirection = order.direction() == UserDirectoryOrder.Direction.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(sortDirection, sortProperty).and(Sort.by(Sort.Direction.ASC, "id"));
        var result = accounts.search(normalizedSearch, normalizedRole, normalizedStatus, normalizedLocale,
                createdFrom, createdBefore, PageRequest.of(page.page(), page.size(), sort));
        return new PageResult<>(result.getContent().stream().map(IdentityUserDirectoryService::summary).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    private static void requireUserManagement(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.ADMIN || !actor.hasPermission(Permission.USERS_MANAGE)) {
            throw new AccessDeniedException("User management permission required");
        }
    }

    private static void requireUserOrStaffManagement(AuthenticatedUser actor) {
        if (actor == null || actor.role() != Role.ADMIN
                || !actor.hasPermission(Permission.USERS_MANAGE) && !actor.hasPermission(Permission.STAFF_MANAGE)) {
            throw new AccessDeniedException("User management permission required");
        }
    }

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String normalized = search.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Search text exceeds the allowed length");
        }
        return normalized;
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            String normalized = role.trim().toUpperCase(Locale.ROOT);
            return "STAFF".equals(normalized) ? normalized : Role.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported user role", exception);
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported user status");
        }
        return normalized;
    }

    private static String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return null;
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT);
        if (!"es".equals(normalized) && !"en".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported user locale");
        }
        return normalized;
    }

    private static IdentityUserSummary summary(UserAccountEntity account) {
        String role = account.getRoles().stream().map(roleEntity -> roleEntity.getCode())
                .sorted().findFirst().orElseThrow(() -> new IllegalStateException("Account has no role"));
        return new IdentityUserSummary(account.getPublicId(), account.getEmailNormalized(),
                account.getDisplayName(), role, account.getStatus(), account.getPreferredLocale(),
                account.isEmailVerified(), account.isLockedAt(java.time.Instant.now()), account.getCreatedAt());
    }
}
