IF SCHEMA_ID(N'simbiosphere') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [simbiosphere]');
END;

CREATE TABLE [simbiosphere].[provisioning] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_simbiosphere_provisioning] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [client_account_public_id] UNIQUEIDENTIFIER NOT NULL,
    [status_code] NVARCHAR(24) NOT NULL,
    [external_system] NVARCHAR(32) NOT NULL,
    [external_account_id] NVARCHAR(128) NULL,
    [idempotency_key] NVARCHAR(128) NOT NULL,
    [error_code] NVARCHAR(64) NULL,
    [attempt_count] INT NOT NULL CONSTRAINT [df_simbiosphere_attempt_count] DEFAULT 0,
    [next_attempt_at] DATETIME2(3) NULL,
    [lease_owner] NVARCHAR(80) NULL,
    [lease_expires_at] DATETIME2(3) NULL,
    [created_at] DATETIME2(3) NOT NULL,
    [updated_at] DATETIME2(3) NOT NULL,
    [version] BIGINT NOT NULL CONSTRAINT [df_simbiosphere_provisioning_version] DEFAULT 0,
    CONSTRAINT [uq_simbiosphere_provisioning_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_simbiosphere_provisioning_account] UNIQUE ([client_account_public_id]),
    CONSTRAINT [uq_simbiosphere_provisioning_idempotency] UNIQUE ([idempotency_key]),
    CONSTRAINT [ck_simbiosphere_provisioning_status] CHECK ([status_code] IN
        (N'PENDING', N'READY', N'PROCESSING', N'PROVISIONED', N'ACTION_REQUIRED', N'FAILED')),
    CONSTRAINT [ck_simbiosphere_provisioning_attempts] CHECK ([attempt_count] >= 0)
);

CREATE INDEX [ix_simbiosphere_provisioning_ready]
    ON [simbiosphere].[provisioning] ([status_code], [next_attempt_at], [lease_expires_at]);

CREATE TABLE [simbiosphere].[account_link] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_simbiosphere_account_link] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [client_account_public_id] UNIQUEIDENTIFIER NOT NULL,
    [external_system] NVARCHAR(32) NOT NULL,
    [external_account_id] NVARCHAR(128) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_simbiosphere_account_link_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_simbiosphere_external_account] UNIQUE ([external_system], [external_account_id])
);

CREATE INDEX [ix_simbiosphere_account_link_owner]
    ON [simbiosphere].[account_link] ([client_account_public_id], [external_system]);

CREATE TABLE [simbiosphere].[linked_business] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_simbiosphere_linked_business] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [account_link_id] BIGINT NOT NULL,
    [external_business_id] NVARCHAR(128) NOT NULL,
    [business_name] NVARCHAR(200) NOT NULL,
    [business_type] NVARCHAR(40) NOT NULL,
    [tenant_slug] NVARCHAR(80) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [fk_simbiosphere_business_account_link] FOREIGN KEY ([account_link_id])
        REFERENCES [simbiosphere].[account_link] ([id]),
    CONSTRAINT [uq_simbiosphere_linked_business_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_simbiosphere_external_business] UNIQUE ([external_business_id])
);

CREATE INDEX [ix_simbiosphere_linked_business_account]
    ON [simbiosphere].[linked_business] ([account_link_id]);
