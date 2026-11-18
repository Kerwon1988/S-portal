package com.simbio.core.customer.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.identity.api.IdentityUserDirectory;
import com.simbio.core.identity.api.IdentityUserSummary;
import com.simbio.core.identity.api.UserDirectoryOrder;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.staff.api.StaffDirectory;
import com.simbio.core.staff.api.StaffSummary;

@Service
public class UserManagementService {

    private final IdentityUserDirectory identityUsers;
    private final CustomerDirectory customers;
    private final StaffDirectory staff;

    public UserManagementService(
            IdentityUserDirectory identityUsers,
            CustomerDirectory customers,
            StaffDirectory staff) {
        this.identityUsers = identityUsers;
        this.customers = customers;
        this.staff = staff;
    }

    @Transactional(readOnly = true)
    public PageResult<ManagedUserSummary> search(
            AuthenticatedUser actor,
            String role,
            String search,
            String status,
            String locale,
            Instant createdFrom,
            Instant createdBefore,
            UserDirectoryOrder order,
            PageQuery page) {
        if (!"CLIENT".equals(role) && !"STAFF".equals(role)) {
            throw new IllegalArgumentException("Only client and employee directories are available");
        }
        PageResult<IdentityUserSummary> identities = identityUsers.search(actor, search, role, status, locale,
                createdFrom, createdBefore, order, page);
        return new PageResult<>(identities.content().stream()
                .map(this::summary)
                .toList(), identities.page(), identities.size(), identities.totalElements());
    }

    @Transactional(readOnly = true)
    public Optional<ManagedUserSummary> find(AuthenticatedUser actor, UUID userPublicId) {
        return identityUsers.find(actor, userPublicId).map(this::summary);
    }

    private ManagedUserSummary summary(IdentityUserSummary identity) {
        Optional<com.simbio.core.customer.api.CustomerAccountSummary> customerAccount =
                "CLIENT".equals(identity.role())
                        ? customers.findOwnedBy(identity.publicId())
                        : Optional.empty();
        Optional<StaffSummary> staffProfile = ("EMPLOYEE".equals(identity.role()) || "ADMIN".equals(identity.role()))
                ? staff.findByUserId(identity.publicId())
                : Optional.empty();
        return new ManagedUserSummary(identity, customerAccount, staffProfile);
    }
}
