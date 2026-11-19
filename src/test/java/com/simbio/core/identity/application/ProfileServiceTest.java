package com.simbio.core.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.simbio.core.identity.domain.RoleEntity;
import com.simbio.core.identity.domain.UserAccountEntity;

class ProfileServiceTest {

    @Test
    void readsAndUpdatesOnlyTheRequestedAccountProfile() {
        UserAccountStore accounts = mock(UserAccountStore.class);
        UUID userId = UUID.randomUUID();
        UserAccountEntity account = new UserAccountEntity(userId, "synthetic@example.test", "hash", "es",
                Instant.parse("2026-11-18T12:00:00Z"), new RoleEntity("CLIENT", "Client"),
                "Synthetic user");
        when(accounts.findByPublicId(userId)).thenReturn(Optional.of(account));
        ProfileService service = new ProfileService(accounts);

        var profile = service.get(userId);
        service.update(userId, "Updated user", "en", "dark");

        assertThat(profile.email()).isEqualTo("synthetic@example.test");
        assertThat(profile.displayName()).isEqualTo("Synthetic user");
        assertThat(profile.locale()).isEqualTo("es");
        assertThat(account.getDisplayName()).isEqualTo("Updated user");
        assertThat(account.getPreferredLocale()).isEqualTo("en");
        assertThat(account.getThemePreference()).isEqualTo("dark");
        verify(accounts).save(account);
    }

    @Test
    void rejectsUpdatesWhenTheAccountDoesNotExist() {
        UserAccountStore accounts = mock(UserAccountStore.class);
        ProfileService service = new ProfileService(accounts);

        assertThatThrownBy(() -> service.get(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account is unavailable");
    }
}
