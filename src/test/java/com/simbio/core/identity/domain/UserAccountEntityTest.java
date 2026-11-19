package com.simbio.core.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class UserAccountEntityTest {

    @Test
    void appliesProgressiveLockoutAndResetsAfterSuccessfulAuthentication() {
        UserAccountEntity account = new UserAccountEntity(java.util.UUID.randomUUID(),
                "synthetic@example.test", "{argon2id}hash", "en", Instant.EPOCH,
                new RoleEntity("ADMIN", "Administrator"));

        for (int attempt = 0; attempt < 4; attempt++) {
            account.recordFailedLogin(Instant.EPOCH);
        }
        assertThat(account.getLockedUntil()).isNull();

        account.recordFailedLogin(Instant.EPOCH);
        assertThat(account.getLockedUntil()).isEqualTo(Instant.EPOCH.plusSeconds(60));
        account.recordFailedLogin(Instant.EPOCH.plusSeconds(1));
        assertThat(account.getLockedUntil()).isEqualTo(Instant.EPOCH.plusSeconds(60));

        account.resetLoginFailures();
        assertThat(account.getLockedUntil()).isNull();
        assertThat(account.getFailedLoginCount()).isZero();
    }

    @Test
    void growsLockoutDelayAndAllowsAnewAttemptAfterTheLockExpires() {
        UserAccountEntity account = new UserAccountEntity(java.util.UUID.randomUUID(),
                "synthetic@example.test", "{argon2id}hash", "en", Instant.EPOCH,
                new RoleEntity("ADMIN", "Administrator"));

        for (int attempt = 0; attempt < 5; attempt++) {
            account.recordFailedLogin(Instant.EPOCH);
        }
        account.recordFailedLogin(Instant.EPOCH.plusSeconds(60));

        assertThat(account.getFailedLoginCount()).isEqualTo(1);
        assertThat(account.getLockedUntil()).isNull();
        for (int attempt = 0; attempt < 4; attempt++) {
            account.recordFailedLogin(Instant.EPOCH.plusSeconds(60));
        }
        assertThat(account.getLockedUntil()).isEqualTo(Instant.EPOCH.plusSeconds(180));
        account.unlock();
        assertThat(account.getFailedLoginCount()).isZero();
        assertThat(account.getLockedUntil()).isNull();
    }

    @Test
    void updatesProfileLocaleThemeStatusPasswordAndRoleWithValidation() {
        UserAccountEntity account = new UserAccountEntity(java.util.UUID.randomUUID(),
                "synthetic@example.test", "old-hash", "es", Instant.EPOCH,
                new RoleEntity("CLIENT", "Client"));

        account.updateProfile(" Synthetic user ", "en", "dark");
        account.updateLocale("es");
        account.updateTheme("light");
        account.changePassword("new-hash");
        account.setActive(false);
        account.replaceRole(new RoleEntity("EMPLOYEE", "Employee"));

        assertThat(account.getDisplayName()).isEqualTo("Synthetic user");
        assertThat(account.getPreferredLocale()).isEqualTo("es");
        assertThat(account.getThemePreference()).isEqualTo("light");
        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        assertThat(account.getStatus()).isEqualTo("INACTIVE");
        assertThat(account.isEnabled()).isFalse();
        assertThat(account.getRoles()).extracting(RoleEntity::getCode).containsExactly("EMPLOYEE");
        assertThatThrownBy(() -> account.replaceRole(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A role is required");
        assertThatThrownBy(() -> account.updateProfile(" ", "en", "light"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.updateProfile("Synthetic", "fr", "light"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.updateProfile("Synthetic", "en", "system"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.updateLocale("fr"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.updateTheme("system"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roleEntityUsesItsCodeForEqualityAndHashing() {
        RoleEntity admin = new RoleEntity("ADMIN", "Administrator");
        RoleEntity sameRole = new RoleEntity("ADMIN", "Different description");

        assertThat(admin).isEqualTo(sameRole);
        assertThat(admin.hashCode()).isEqualTo(sameRole.hashCode());
        assertThat(admin.getPermissions()).isEmpty();
    }
}
