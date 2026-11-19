package com.simbio.core.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.customer.api.CustomerAccountSummary;
import com.simbio.core.customer.api.CustomerDirectory;
import com.simbio.core.identity.api.IdentityUserDirectory;
import com.simbio.core.identity.api.IdentityUserSummary;
import com.simbio.core.identity.api.UserDirectoryOrder;
import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.shared.paging.PageResult;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Role;
import com.simbio.core.staff.api.StaffDirectory;
import com.simbio.core.staff.api.StaffSummary;

class UserManagementServiceTest {

    private final IdentityUserDirectory identities = mock(IdentityUserDirectory.class);
    private final CustomerDirectory customers = mock(CustomerDirectory.class);
    private final StaffDirectory staff = mock(StaffDirectory.class);
    private final UserManagementService service = new UserManagementService(identities, customers, staff);

    @Test
    void combinesOnlyOwnerSummariesRelevantToEachIdentityRole() {
        UUID clientId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-18T12:00:00Z");
        var client = summary(clientId, "CLIENT", createdAt);
        var employee = summary(employeeId, "EMPLOYEE", createdAt);
        var admin = summary(adminId, "ADMIN", createdAt);
        PageQuery page = PageQuery.of(0, 10);
        when(identities.search(null, null, "STAFF", null, null, null, null,
                UserDirectoryOrder.DEFAULT, page))
                .thenReturn(new PageResult<>(List.of(employee, admin), 0, 10, 2));
        when(staff.findByUserId(employeeId)).thenReturn(Optional.of(staffSummary(employeeId, createdAt)));
        when(staff.findByUserId(adminId)).thenReturn(Optional.empty());
        when(identities.find(null, clientId)).thenReturn(Optional.of(client));
        CustomerAccountSummary account = new CustomerAccountSummary(
                UUID.randomUUID(), "INDIVIDUAL", "Synthetic account", "ACTIVE", createdAt);
        when(customers.findOwnedBy(clientId)).thenReturn(Optional.of(account));

        var result = service.search(null, "STAFF", null, null, null, null, null,
                UserDirectoryOrder.DEFAULT, page);
        var clientResult = service.find(null, clientId).orElseThrow();

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).staffProfile()).contains(staffSummary(employeeId, createdAt));
        assertThat(result.content().get(1).staffProfile()).isEmpty();
        assertThat(clientResult.customerAccount()).contains(account);
        assertThat(clientResult.staffProfile()).isEmpty();
        verify(customers, never()).findOwnedBy(employeeId);
    }

    @Test
    void rejectsUnsupportedDirectoryAndPreservesAbsentIdentity() {
        UUID actorId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        AuthenticatedUser actor = new AuthenticatedUser(actorId, Role.ADMIN, java.util.Set.of());

        assertThatThrownBy(() -> service.search(actor, "ADMIN", null, null, null, null, null,
                UserDirectoryOrder.DEFAULT, PageQuery.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only client and employee directories are available");
        when(identities.find(actor, missingId)).thenReturn(Optional.empty());
        assertThat(service.find(actor, missingId)).isEmpty();
    }

    private static IdentityUserSummary summary(UUID userId, String role, Instant createdAt) {
        return new IdentityUserSummary(userId, "synthetic@example.test", "Synthetic user", role,
                "ACTIVE", "es", true, false, createdAt);
    }

    private static StaffSummary staffSummary(UUID userId, Instant createdAt) {
        return new StaffSummary(userId, "Specialist", LocalDate.of(2026, 1, 1),
                "ACTIVE", List.of("Analysis"), createdAt);
    }
}
