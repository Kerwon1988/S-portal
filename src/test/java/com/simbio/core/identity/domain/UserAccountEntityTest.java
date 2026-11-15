package com.simbio.core.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
}
