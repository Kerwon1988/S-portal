ALTER TABLE [messaging].[message] DROP CONSTRAINT [ck_messaging_message_author];

ALTER TABLE [messaging].[message]
    ADD CONSTRAINT [ck_messaging_message_author] CHECK (
        ([author_type] IN (N'ANONYMOUS', N'SYSTEM', N'AI_AGENT') AND [author_id] IS NULL)
        OR ([author_type] IN (N'CLIENT', N'EMPLOYEE', N'ADMIN') AND [author_id] IS NOT NULL));
