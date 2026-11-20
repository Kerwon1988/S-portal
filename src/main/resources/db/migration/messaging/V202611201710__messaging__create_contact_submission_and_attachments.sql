CREATE TABLE [messaging].[contact_submission] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_messaging_contact_submission] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [conversation_id] BIGINT NOT NULL,
    [person_name_encrypted] NVARCHAR(2048) NOT NULL,
    [email_encrypted] NVARCHAR(2048) NOT NULL,
    [phone_encrypted] NVARCHAR(2048) NULL,
    [reason] NVARCHAR(160) NOT NULL,
    [locale] NCHAR(2) NOT NULL,
    [consent_version] INT NOT NULL,
    [consent_text_sha256] CHAR(64) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_messaging_contact_submission_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_messaging_contact_submission_conversation] UNIQUE ([conversation_id]),
    CONSTRAINT [fk_messaging_contact_submission_conversation] FOREIGN KEY ([conversation_id])
        REFERENCES [messaging].[conversation] ([id]),
    CONSTRAINT [ck_messaging_contact_submission_locale] CHECK ([locale] IN (N'es', N'en')),
    CONSTRAINT [ck_messaging_contact_submission_consent] CHECK ([consent_version] > 0)
);
CREATE INDEX [ix_messaging_contact_submission_created]
    ON [messaging].[contact_submission] ([created_at], [id]);

CREATE TABLE [messaging].[message_attachment] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_messaging_message_attachment] PRIMARY KEY,
    [message_id] BIGINT NOT NULL,
    [file_public_id] UNIQUEIDENTIFIER NOT NULL,
    [detected_mime] NVARCHAR(100) NOT NULL,
    [size_bytes] BIGINT NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [fk_messaging_message_attachment_message] FOREIGN KEY ([message_id])
        REFERENCES [messaging].[message] ([id]),
    CONSTRAINT [uq_messaging_message_attachment_file] UNIQUE ([file_public_id]),
    CONSTRAINT [ck_messaging_message_attachment_size] CHECK ([size_bytes] > 0)
);
CREATE INDEX [ix_messaging_message_attachment_message_created]
    ON [messaging].[message_attachment] ([message_id], [created_at], [id]);
