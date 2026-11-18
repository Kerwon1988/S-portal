IF SCHEMA_ID(N'customer') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [customer]');
END;

CREATE TABLE [customer].[client_account] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_customer_client_account] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [account_type] NVARCHAR(24) NOT NULL,
    [display_name] NVARCHAR(160) NOT NULL,
    [status] NVARCHAR(16) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_customer_account_public_id] UNIQUE ([public_id]),
    CONSTRAINT [ck_customer_account_type] CHECK (
        [account_type] IN (N'COMPANY', N'SOLE_TRADER', N'INDIVIDUAL')),
    CONSTRAINT [ck_customer_account_status] CHECK ([status] IN (N'ACTIVE', N'INACTIVE'))
);

CREATE TABLE [customer].[account_member] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_customer_account_member] PRIMARY KEY,
    [account_id] BIGINT NOT NULL,
    [user_public_id] UNIQUEIDENTIFIER NOT NULL,
    [role_code] NVARCHAR(16) NOT NULL,
    [status_code] NVARCHAR(16) NOT NULL,
    [owner_slot] NVARCHAR(5) NULL,
    [joined_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [fk_customer_member_account] FOREIGN KEY ([account_id])
        REFERENCES [customer].[client_account] ([id]),
    CONSTRAINT [uq_customer_member_account_user] UNIQUE ([account_id], [user_public_id]),
    CONSTRAINT [ck_customer_member_role] CHECK ([role_code] IN (N'OWNER', N'MEMBER')),
    CONSTRAINT [ck_customer_member_status] CHECK ([status_code] IN (N'ACTIVE', N'DISABLED')),
    CONSTRAINT [ck_customer_member_owner_slot] CHECK (
        ([role_code] = N'OWNER' AND [status_code] = N'ACTIVE'
            AND [owner_slot] IS NOT NULL AND [owner_slot] = N'OWNER')
        OR ([role_code] = N'OWNER' AND [status_code] = N'DISABLED' AND [owner_slot] IS NULL)
        OR ([role_code] = N'MEMBER' AND [owner_slot] IS NULL))
);

CREATE UNIQUE INDEX [uq_customer_member_active_owner]
    ON [customer].[account_member] ([account_id], [owner_slot])
    WHERE [owner_slot] IS NOT NULL;
CREATE INDEX [ix_customer_member_user_status]
    ON [customer].[account_member] ([user_public_id], [status_code]);
