IF SCHEMA_ID(N'notification') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [notification]');
END;

CREATE TABLE [notification].[outbox_message] (
    [id] UNIQUEIDENTIFIER NOT NULL CONSTRAINT [pk_notification_outbox_message] PRIMARY KEY,
    [payload_encrypted] NVARCHAR(MAX) NULL,
    [status] NVARCHAR(16) NOT NULL,
    [attempt_count] INT NOT NULL CONSTRAINT [df_notification_outbox_attempts] DEFAULT 0,
    [available_at] DATETIME2(3) NOT NULL,
    [claimed_until] DATETIME2(3) NULL,
    [created_at] DATETIME2(3) NOT NULL CONSTRAINT [df_notification_outbox_created] DEFAULT SYSUTCDATETIME(),
    [sent_at] DATETIME2(3) NULL,
    [failure_code] NVARCHAR(64) NULL,
    CONSTRAINT [ck_notification_outbox_status]
        CHECK ([status] IN (N'PENDING', N'PROCESSING', N'SENT', N'FAILED'))
);
CREATE INDEX [ix_notification_outbox_claim]
    ON [notification].[outbox_message] ([status], [available_at], [created_at]);
CREATE INDEX [ix_notification_outbox_purge]
    ON [notification].[outbox_message] ([status], [created_at])
    INCLUDE ([payload_encrypted]);
