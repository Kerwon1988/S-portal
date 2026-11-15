package com.simbio.core.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.identity.application.IdentityInitializer;

@SpringBootTest
@ActiveProfiles("test")
class IdentitySeedH2IT {

    @Autowired
    private IdentityInitializer identityInitializer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedsRolesAndPermissionsIdempotentlyWithoutCreatingAccounts() {
        identityInitializer.seed();
        identityInitializer.seed();

        assertThat(jdbcTemplate.queryForObject("select count(*) from identity.role", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from identity.permission", Integer.class))
                .isEqualTo(com.simbio.core.identity.api.Permission.values().length);
        assertThat(jdbcTemplate.queryForObject("select count(*) from identity.user_account", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from identity.role_permission where role_code = 'ADMIN'", Integer.class))
                .isEqualTo(com.simbio.core.identity.api.Permission.values().length);
    }
}
