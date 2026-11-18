ALTER TABLE [identity].[one_time_token]
    ADD [email_key] CHAR(64) NULL;
CREATE INDEX [ix_identity_token_invitation_email]
    ON [identity].[one_time_token] ([email_key], [purpose], [consumed_at], [revoked_at]);
