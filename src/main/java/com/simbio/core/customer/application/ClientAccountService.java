package com.simbio.core.customer.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.audit.api.AuditRecord;
import com.simbio.core.audit.api.AuditRecorder;
import com.simbio.core.customer.api.CustomerAccountCreator;
import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.customer.domain.AccountMemberEntity;
import com.simbio.core.customer.domain.ClientAccountEntity;
import com.simbio.core.customer.domain.ClientAccountType;
import com.simbio.core.identity.api.IdentityUserLookup;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;

@Service
public class ClientAccountService implements CustomerDirectory, CustomerAccountCreator {

    private final ClientAccountStore store;
    private final IdentityUserLookup identityUsers;
    private final AuditRecorder auditRecorder;
    private final Clock clock;

    @Autowired
    public ClientAccountService(ClientAccountStore store, IdentityUserLookup identityUsers,
            AuditRecorder auditRecorder) {
        this(store, identityUsers, auditRecorder, Clock.systemUTC());
    }

    ClientAccountService(ClientAccountStore store, IdentityUserLookup identityUsers,
            AuditRecorder auditRecorder, Clock clock) {
        this.store = store;
        this.identityUsers = identityUsers;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CustomerAccountSummary createOwnerAccount(UUID ownerUserId, String accountType, String displayName) {
        return createOwnerAccount(ownerUserId, ownerUserId, accountType, displayName);
    }

    @Override
    @Transactional
    public CustomerAccountSummary createOwnerAccount(
            UUID creatorUserId,
            UUID ownerUserId,
            String accountType,
            String displayName) {
        if (ownerUserId == null || creatorUserId == null
                || !identityUsers.existsById(ownerUserId) || !identityUsers.existsById(creatorUserId)) {
            throw new IllegalArgumentException("Existing identity users are required for creator and account OWNER");
        }
        if (accountType == null) {
            throw new IllegalArgumentException("Unsupported account type");
        }
        ClientAccountType type;
        try {
            type = ClientAccountType.valueOf(accountType);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported account type", exception);
        }
        Instant now = Instant.now(clock);
        ClientAccountEntity account = store.save(new ClientAccountEntity(
                UUID.randomUUID(), type, displayName, now));
        store.save(new AccountMemberEntity(account, ownerUserId, "OWNER", "ACTIVE", now));
        auditRecorder.record(new AuditRecord(creatorUserId, "CLIENT_ACCOUNT_CREATED",
                "CLIENT_ACCOUNT", account.getPublicId(), null));
        return summary(account);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CustomerAccountSummary> search(String search, String status, PageQuery page) {
        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus = normalizeStatus(status);
        var result = store.search(normalizedSearch, normalizedStatus,
                PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResult<>(result.getContent().stream().map(ClientAccountService::summary).toList(),
                page.page(), page.size(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerAccountSummary> findOwnedBy(UUID userPublicId) {
        return store.findOwnedBy(userPublicId).map(ClientAccountService::summary);
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

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported account status");
        }
        return normalized;
    }

    private static CustomerAccountSummary summary(ClientAccountEntity account) {
        return new CustomerAccountSummary(account.getPublicId(), account.getAccountType().name(),
                account.getDisplayName(), account.getStatus(), account.getCreatedAt());
    }
}
