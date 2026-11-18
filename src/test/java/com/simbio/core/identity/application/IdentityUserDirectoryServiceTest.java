package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import com.simbio.core.shared.paging.PageQuery;
import com.simbio.core.identity.api.UserDirectoryOrder;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;

class IdentityUserDirectoryServiceTest {

    private final UserAccountStore accounts = org.mockito.Mockito.mock(UserAccountStore.class);
    private final IdentityUserDirectoryService service = new IdentityUserDirectoryService(accounts);

    @Test
    void searchesOnlyForAdminWithUserManagementPermissionAndUsesBoundedFilters() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant before = Instant.parse("2026-02-01T00:00:00Z");
        var pageable = PageRequest.of(0, 25, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                .and(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.ASC, "id")));
        AuthenticatedUser admin = new AuthenticatedUser(java.util.UUID.randomUUID(), Role.ADMIN,
                Set.of(Permission.USERS_MANAGE));
        when(accounts.search("example", "CLIENT", "ACTIVE", "es", from, before, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var result = service.search(admin, " example ", "client", "active", "ES", from, before,
                UserDirectoryOrder.DEFAULT, new PageQuery(0, 25));

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isZero();
        verify(accounts).search("example", "CLIENT", "ACTIVE", "es", from, before, pageable);
    }

    @Test
    void deniesNonAdminAndRejectsUnsupportedFiltersBeforeQuerying() {
        AuthenticatedUser employee = new AuthenticatedUser(java.util.UUID.randomUUID(), Role.EMPLOYEE, Set.of());

        assertThatThrownBy(() -> service.search(employee, null, null, null, null, null, null,
                UserDirectoryOrder.DEFAULT, new PageQuery(0, 10))).isInstanceOf(AccessDeniedException.class);

        AuthenticatedUser admin = new AuthenticatedUser(java.util.UUID.randomUUID(), Role.ADMIN,
                Set.of(Permission.USERS_MANAGE));
        assertThatThrownBy(() -> service.search(admin, null, "OWNER", null, null, null, null,
                UserDirectoryOrder.DEFAULT, new PageQuery(0, 10))).isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verifyNoInteractions(accounts);
    }

    @Test
    void acceptsOnlyTypedSortFieldsAndBuildsStableNameOrdering() {
        AuthenticatedUser admin = new AuthenticatedUser(java.util.UUID.randomUUID(), Role.ADMIN,
                Set.of(Permission.USERS_MANAGE));
        var order = new UserDirectoryOrder(
                UserDirectoryOrder.Field.DISPLAY_NAME, UserDirectoryOrder.Direction.ASC);
        var pageable = PageRequest.of(0, 25, org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "displayName")
                .and(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.ASC, "id")));
        when(accounts.search(null, "CLIENT", null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.search(admin, null, "CLIENT", null, null, null, null, order, new PageQuery(0, 25));

        verify(accounts).search(null, "CLIENT", null, null, null, null, pageable);
    }
}
