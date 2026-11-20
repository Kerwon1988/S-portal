package com.simbio.core.messaging.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MessagingMigrationContractTest {

    private static final Path MIGRATION = Path.of(
            "src/main/resources/db/migration/messaging/"
                    + "V202611191552__messaging__create_conversations_and_messages.sql");

    @Test
    void messagingMigrationKeepsOwnerReferencesAndRequiredIndexes() throws IOException {
        String migration = Files.readString(MIGRATION);

        assertThat(migration)
                .contains("CREATE TABLE [messaging].[conversation]")
                .contains("CREATE TABLE [messaging].[message]")
                .contains("[context_public_id] UNIQUEIDENTIFIER NOT NULL")
                .contains("[owner_user_id] UNIQUEIDENTIFIER NULL")
                .contains("UNIQUE ([context_type], [context_public_id])")
                .contains("ix_messaging_message_conversation_created")
                .contains("ix_messaging_message_visibility_created")
                .contains("ix_messaging_conversation_owner_status_created")
                .doesNotContain("REFERENCES [request].")
                .doesNotContain("REFERENCES [customer].")
                .doesNotContain("REFERENCES [identity].");
    }

    @Test
    void contactMigrationKeepsSubmissionAndAttachmentOwnershipInsideMessaging() throws IOException {
        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/messaging/"
                        + "V202611201710__messaging__create_contact_submission_and_attachments.sql"));

        assertThat(migration)
                .contains("CREATE TABLE [messaging].[contact_submission]")
                .contains("CREATE TABLE [messaging].[message_attachment]")
                .contains("[person_name_encrypted]")
                .contains("[email_encrypted]")
                .contains("[consent_version]")
                .contains("REFERENCES [messaging].[conversation]")
                .contains("REFERENCES [messaging].[message]")
                .doesNotContain("REFERENCES [files].")
                .doesNotContain("REFERENCES [customer].")
                .doesNotContain("REFERENCES [legal].");
    }

    @Test
    void messagingMigrationRejectsInvalidContextAuthorAndVisibilityAtTheDatabaseBoundary() throws IOException {
        String migration = Files.readString(MIGRATION);

        assertThat(migration)
                .contains("CHECK ([context_type] IN (N'CONTACT', N'REQUEST'))")
                .contains("CHECK ([status_code] IN (N'OPEN', N'CLOSED'))")
                .contains("[context_type] = N'CONTACT' OR [owner_user_id] IS NOT NULL")
                .contains("CHECK ([visibility] IN (N'PUBLIC', N'INTERNAL'))")
                .contains("[body] NVARCHAR(10000) NOT NULL")
                .contains("LEN(LTRIM(RTRIM([body]))) > 0");
    }
}
