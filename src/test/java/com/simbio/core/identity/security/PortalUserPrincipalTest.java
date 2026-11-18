package com.simbio.core.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;
import com.simbio.core.shared.security.Role;

class PortalUserPrincipalTest {

    @Test
    void mapsKnownRolesToAuthenticatedPrincipal() {
        PortalUserPrincipal principal = new PortalUserPrincipal(account(new RoleEntity("ADMIN", "Administrator")));

        assertThat(principal.authenticatedUser().role()).isEqualTo(Role.ADMIN);
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN");
    }

    @Test
    void rejectsUnknownRolesInsteadOfGrantingAnImplicitClientRole() {
        assertThatThrownBy(() ->
                new PortalUserPrincipal(account(new RoleEntity("SUPERUSER", "Unknown role"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown identity role");
    }

    private static UserAccountEntity account(RoleEntity role) {
        return new UserAccountEntity(UUID.randomUUID(), "synthetic@example.test",
                "{argon2id}synthetic-hash", "en", Instant.parse("2026-11-17T00:00:00Z"), role);
    }
}
