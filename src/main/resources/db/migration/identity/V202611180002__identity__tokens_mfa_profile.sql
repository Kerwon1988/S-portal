ALTER TABLE [identity].[user_account]
    ADD [display_name] NVARCHAR(120) NULL,
        [theme_preference] NVARCHAR(8) NULL;
ALTER TABLE [identity].[user_account]
    ADD CONSTRAINT [ck_identity_user_theme]
        CHECK ([theme_preference] IS NULL OR [theme_preference] IN (N'light', N'dark'));

CREATE TABLE [identity].[one_time_token] (
    [id] UNIQUEIDENTIFIER NOT NULL CONSTRAINT [pk_identity_one_time_token] PRIMARY KEY,
    [token_hash] CHAR(64) NOT NULL,
    [purpose] NVARCHAR(32) NOT NULL,
    [user_public_id] UNIQUEIDENTIFIER NULL,
    [email_encrypted] NVARCHAR(2048) NULL,
    [role_code] NVARCHAR(32) NULL,
    [expires_at] DATETIME2(3) NOT NULL,
    [consumed_at] DATETIME2(3) NULL,
    [revoked_at] DATETIME2(3) NULL,
    [created_at] DATETIME2(3) NOT NULL CONSTRAINT [df_identity_token_created] DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [uq_identity_token_hash] UNIQUE ([token_hash]),
    CONSTRAINT [fk_identity_token_user]
        FOREIGN KEY ([user_public_id]) REFERENCES [identity].[user_account] ([public_id]),
    CONSTRAINT [ck_identity_token_purpose]
        CHECK ([purpose] IN (N'EMAIL_VERIFICATION', N'PASSWORD_RESET', N'INVITATION')),
    CONSTRAINT [ck_identity_invitation_role]
        CHECK ([purpose] <> N'INVITATION' OR
            ([role_code] IS NOT NULL AND [role_code] IN (N'CLIENT', N'EMPLOYEE')))
);
CREATE INDEX [ix_identity_token_active]
    ON [identity].[one_time_token] ([purpose], [expires_at], [consumed_at], [revoked_at]);
CREATE INDEX [ix_identity_token_user]
    ON [identity].[one_time_token] ([user_public_id], [purpose], [consumed_at], [revoked_at]);

CREATE TABLE [identity].[mfa_factor] (
    [user_public_id] UNIQUEIDENTIFIER NOT NULL CONSTRAINT [pk_identity_mfa_factor] PRIMARY KEY,
    [secret_encrypted] NVARCHAR(2048) NOT NULL,
    [enabled] BIT NOT NULL CONSTRAINT [df_identity_mfa_enabled] DEFAULT 0,
    [last_accepted_step] BIGINT NOT NULL CONSTRAINT [df_identity_mfa_step] DEFAULT 0,
    [recovery_hashes] NVARCHAR(2048) NULL,
    [updated_at] DATETIME2(3) NOT NULL CONSTRAINT [df_identity_mfa_updated] DEFAULT SYSUTCDATETIME(),
    [version] BIGINT NOT NULL CONSTRAINT [df_identity_mfa_version] DEFAULT 0,
    CONSTRAINT [fk_identity_mfa_user]
        FOREIGN KEY ([user_public_id]) REFERENCES [identity].[user_account] ([public_id])
);
