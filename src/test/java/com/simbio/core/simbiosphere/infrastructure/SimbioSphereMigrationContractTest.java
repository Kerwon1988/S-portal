package com.simbio.core.simbiosphere.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SimbioSphereMigrationContractTest {
    private static final Path MIGRATION = Path.of("src", "main", "resources", "db", "migration",
            "simbiosphere", "V202611201930__simbiosphere__create_provisioning_and_links.sql");

    @Test
    void migrationKeepsAllTablesOwnedAndPreventsDuplicateExternalLinks() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("CREATE TABLE [simbiosphere].[provisioning]")
                .contains("CREATE TABLE [simbiosphere].[account_link]")
                .contains("CREATE TABLE [simbiosphere].[linked_business]")
                .contains("UNIQUE ([external_system], [external_account_id])")
                .contains("UNIQUE ([external_business_id])")
                .contains("[client_account_public_id] UNIQUEIDENTIFIER")
                .doesNotContain("REFERENCES [customer]")
                .doesNotContain("REFERENCES [identity]");
    }
}
