package com.simbio.core.request.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RequestMigrationContractTest {
    @Test
    void requestMigrationKeepsForeignOwnersAsIdsAndRequiredIndexes() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/request/")
                .resolve("V202611201600__request__create_requests.sql"));
        assertThat(sql).contains("[owner_account_id] UNIQUEIDENTIFIER")
                .contains("[owner_user_id] UNIQUEIDENTIFIER")
                .contains("ix_request_owner_created")
                .contains("uq_request_reference")
                .doesNotContain("REFERENCES [customer].")
                .doesNotContain("REFERENCES [identity].");
    }
}
