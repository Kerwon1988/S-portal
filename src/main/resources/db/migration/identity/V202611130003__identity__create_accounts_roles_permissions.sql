IF SCHEMA_ID(N'identity') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [identity]');
END;

CREATE TABLE [identity].[user_account] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_identity_user_account] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [email_normalized] NVARCHAR(320) NOT NULL,
    [password_hash] NVARCHAR(255) NOT NULL,
    [status] NVARCHAR(16) NOT NULL,
    [email_verified_at] DATETIME2(3) NULL,
    [preferred_locale] NVARCHAR(5) NOT NULL CONSTRAINT [df_identity_locale] DEFAULT N'es',
    [failed_login_count] INT NOT NULL CONSTRAINT [df_identity_failed_login_count] DEFAULT 0,
    [consecutive_lock_count] INT NOT NULL CONSTRAINT [df_identity_lock_count] DEFAULT 0,
    [locked_until] DATETIME2(3) NULL,
    [created_at] DATETIME2(3) NOT NULL CONSTRAINT [df_identity_created_at] DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [uq_identity_user_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_identity_user_email] UNIQUE ([email_normalized]),
    CONSTRAINT [ck_identity_user_status] CHECK ([status] IN (N'ACTIVE', N'DISABLED'))
);

CREATE TABLE [identity].[role] (
    [code] NVARCHAR(32) NOT NULL CONSTRAINT [pk_identity_role] PRIMARY KEY,
    [description] NVARCHAR(128) NOT NULL
);
CREATE TABLE [identity].[permission] (
    [code] NVARCHAR(64) NOT NULL CONSTRAINT [pk_identity_permission] PRIMARY KEY,
    [description] NVARCHAR(128) NOT NULL
);
CREATE TABLE [identity].[role_permission] (
    [role_code] NVARCHAR(32) NOT NULL,
    [permission_code] NVARCHAR(64) NOT NULL,
    CONSTRAINT [pk_identity_role_permission] PRIMARY KEY ([role_code], [permission_code]),
    CONSTRAINT [fk_identity_role_permission_role] FOREIGN KEY ([role_code]) REFERENCES [identity].[role] ([code]),
    CONSTRAINT [fk_identity_role_permission_permission] FOREIGN KEY ([permission_code]) REFERENCES [identity].[permission] ([code])
);
CREATE TABLE [identity].[user_role] (
    [user_id] BIGINT NOT NULL,
    [role_code] NVARCHAR(32) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL CONSTRAINT [df_identity_user_role_created_at] DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [pk_identity_user_role] PRIMARY KEY ([user_id], [role_code]),
    CONSTRAINT [fk_identity_user_role_user] FOREIGN KEY ([user_id]) REFERENCES [identity].[user_account] ([id]),
    CONSTRAINT [fk_identity_user_role_role] FOREIGN KEY ([role_code]) REFERENCES [identity].[role] ([code])
);
CREATE INDEX [ix_identity_user_role_role] ON [identity].[user_role] ([role_code], [user_id]);
CREATE TABLE [identity].[bootstrap_state] (
    [id] TINYINT NOT NULL CONSTRAINT [pk_identity_bootstrap_state] PRIMARY KEY,
    [consumed_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [ck_identity_bootstrap_state_singleton] CHECK ([id] = 1)
);

INSERT INTO [identity].[role] ([code], [description])
VALUES (N'ADMIN', N'Administrator'), (N'EMPLOYEE', N'Employee'), (N'CLIENT', N'Client');

INSERT INTO [identity].[permission] ([code], [description])
VALUES
    (N'USERS_MANAGE', N'Users'),
    (N'REQUESTS_VIEW_ALL', N'Requests'),
    (N'REQUESTS_ASSIGN', N'Assignments'),
    (N'QUOTES_MANAGE', N'Quotes'),
    (N'INBOX_MANAGE', N'Inbox'),
    (N'INBOX_PURGE', N'Inbox purge'),
    (N'CONTENT_EDIT', N'Content'),
    (N'AUDIT_VIEW', N'Audit'),
    (N'STAFF_MANAGE', N'Staff'),
    (N'CLIENTS_EXPORT', N'Client export'),
    (N'PROVISIONING_MANAGE', N'Provisioning');

INSERT INTO [identity].[role_permission] ([role_code], [permission_code])
SELECT N'ADMIN', [code] FROM [identity].[permission];
