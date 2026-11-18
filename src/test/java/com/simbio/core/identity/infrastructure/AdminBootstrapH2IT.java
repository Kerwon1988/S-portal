package com.simbio.core.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.simbio.core.identity.application.AdminBootstrapService;
import com.simbio.core.identity.application.BootstrapStateStore;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin-bootstrap;DB_CLOSE_DELAY=-1;NON_KEYWORDS=IDENTITY,ROLE")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminBootstrapH2IT {

    @Autowired
    private AdminBootstrapService bootstrapService;

    @Autowired
    private BootstrapStateStore bootstrapState;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsExactlyOneAdminAndPersistsConsumptionWithoutLoggingCredentials() {
        assertThat(bootstrapService.bootstrap("admin@example.test", "synthetic-bootstrap-password", "en")).isTrue();
        assertThat(bootstrapService.bootstrap("second-admin@example.test", "synthetic-other-password", "es")).isFalse();

        assertThat(bootstrapState.wasConsumed()).isTrue();
        assertThat(jdbcTemplate.queryForObject("select count(*) from identity.user_account", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from identity.user_role where role_code = 'ADMIN'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit.audit_entry where action_code = 'IDENTITY.ADMIN_BOOTSTRAP'",
                Integer.class)).isEqualTo(1);
    }
}
