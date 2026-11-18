package com.simbio.core.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserAccountEntityRegistrationTest {

    @Test
    void selfServiceAccountRemainsDisabledUntilEmailVerification() {
        UserAccountEntity account = new UserAccountEntity(UUID.randomUUID(), "synthetic@example.test",
                "encoded-hash", "es", Instant.parse("2026-11-18T08:00:00Z"),
                org.mockito.Mockito.mock(RoleEntity.class), "Synthetic person", false);

        assertThat(account.isEmailVerified()).isFalse();
        assertThat(account.isEnabled()).isFalse();

        account.verifyEmail(Instant.parse("2026-11-18T09:00:00Z"));

        assertThat(account.isEmailVerified()).isTrue();
        assertThat(account.isEnabled()).isTrue();
    }
}
