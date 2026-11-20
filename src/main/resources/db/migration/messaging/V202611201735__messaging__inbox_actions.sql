ALTER TABLE [messaging].[conversation]
    ADD [inbox_state] NVARCHAR(16) NOT NULL
        CONSTRAINT [df_messaging_conversation_inbox_state] DEFAULT (N'ACTIVE');

ALTER TABLE [messaging].[conversation]
    ADD [labels] NVARCHAR(255) NOT NULL
        CONSTRAINT [df_messaging_conversation_labels] DEFAULT (N'');

ALTER TABLE [messaging].[conversation]
    ADD [state_changed_at] DATETIME2(3) NULL;

ALTER TABLE [messaging].[conversation]
    ADD CONSTRAINT [ck_messaging_conversation_inbox_state]
        CHECK ([inbox_state] IN (N'ACTIVE', N'ARCHIVED', N'TRASH'));

CREATE INDEX [ix_messaging_conversation_inbox_state_created]
    ON [messaging].[conversation] ([inbox_state], [created_at], [id]);
