IF SCHEMA_ID(N'messaging') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [messaging]');
END;

CREATE TABLE [messaging].[conversation] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_messaging_conversation] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [context_type] NVARCHAR(16) NOT NULL,
    [context_public_id] UNIQUEIDENTIFIER NOT NULL,
    [owner_user_id] UNIQUEIDENTIFIER NULL,
    [status_code] NVARCHAR(16) NOT NULL,
    [assigned_to] UNIQUEIDENTIFIER NULL,
    [created_at] DATETIME2(3) NOT NULL,
    [version] BIGINT NOT NULL CONSTRAINT [df_messaging_conversation_version] DEFAULT (0),
    CONSTRAINT [uq_messaging_conversation_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_messaging_conversation_context] UNIQUE ([context_type], [context_public_id]),
    CONSTRAINT [ck_messaging_conversation_context_type] CHECK ([context_type] IN (N'CONTACT', N'REQUEST')),
    CONSTRAINT [ck_messaging_conversation_status] CHECK ([status_code] IN (N'OPEN', N'CLOSED')),
    CONSTRAINT [ck_messaging_conversation_owner] CHECK (
        [context_type] = N'CONTACT' OR [owner_user_id] IS NOT NULL)
);

CREATE INDEX [ix_messaging_conversation_owner_status_created]
    ON [messaging].[conversation] ([owner_user_id], [status_code], [created_at], [id]);
CREATE INDEX [ix_messaging_conversation_assigned_status_created]
    ON [messaging].[conversation] ([assigned_to], [status_code], [created_at], [id]);

CREATE TABLE [messaging].[message] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_messaging_message] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [conversation_id] BIGINT NOT NULL,
    [author_id] UNIQUEIDENTIFIER NULL,
    [author_type] NVARCHAR(16) NOT NULL,
    [visibility] NVARCHAR(16) NOT NULL,
    [body] NVARCHAR(10000) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    [read_at] DATETIME2(3) NULL,
    CONSTRAINT [uq_messaging_message_public_id] UNIQUE ([public_id]),
    CONSTRAINT [fk_messaging_message_conversation] FOREIGN KEY ([conversation_id])
        REFERENCES [messaging].[conversation] ([id]),
    CONSTRAINT [ck_messaging_message_author] CHECK (
        ([author_type] IN (N'ANONYMOUS', N'SYSTEM') AND [author_id] IS NULL)
        OR ([author_type] IN (N'CLIENT', N'EMPLOYEE', N'ADMIN') AND [author_id] IS NOT NULL)),
    CONSTRAINT [ck_messaging_message_visibility] CHECK ([visibility] IN (N'PUBLIC', N'INTERNAL')),
    CONSTRAINT [ck_messaging_message_body] CHECK (LEN(LTRIM(RTRIM([body]))) > 0)
);

CREATE INDEX [ix_messaging_message_conversation_created]
    ON [messaging].[message] ([conversation_id], [created_at], [id]);
CREATE INDEX [ix_messaging_message_visibility_created]
    ON [messaging].[message] ([conversation_id], [visibility], [created_at], [id]);
