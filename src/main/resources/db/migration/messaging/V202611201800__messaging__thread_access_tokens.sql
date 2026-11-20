CREATE TABLE [messaging].[thread_access_token] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_messaging_thread_access_token] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [conversation_id] BIGINT NOT NULL,
    [token_hash] NCHAR(64) NOT NULL,
    [expires_at] DATETIME2(3) NOT NULL,
    [consumed_at] DATETIME2(3) NULL,
    [revoked_at] DATETIME2(3) NULL,
    CONSTRAINT [uq_messaging_thread_access_token_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_messaging_thread_access_token_hash] UNIQUE ([token_hash]),
    CONSTRAINT [fk_messaging_thread_access_token_conversation] FOREIGN KEY ([conversation_id])
        REFERENCES [messaging].[conversation] ([id])
);

CREATE INDEX [ix_messaging_thread_access_token_conversation]
    ON [messaging].[thread_access_token] ([conversation_id], [expires_at]);
