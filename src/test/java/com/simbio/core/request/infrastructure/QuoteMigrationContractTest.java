package com.simbio.core.request.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class QuoteMigrationContractTest {
    @Test
    void quoteMigrationUsesDecimalAndRequestOwnerOnly() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/request")
                .resolve("V202611201800__request__quotes_and_employee_operations.sql"));

        assertThat(sql).contains("DECIMAL(19,4)").contains("[request].[quote_version]")
                .contains("[request_id]").contains("REFERENCES [request].[request]")
                .contains("ix_quote_version_expiry").doesNotContain("REFERENCES [customer].")
                .doesNotContain("REFERENCES [messaging].");
    }
}
