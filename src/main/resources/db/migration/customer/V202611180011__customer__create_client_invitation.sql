CREATE TABLE [customer].[client_invitation] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_customer_client_invitation] PRIMARY KEY,
    [invitation_reference] UNIQUEIDENTIFIER NOT NULL,
    [account_type] NVARCHAR(24) NOT NULL,
    [account_name_ciphertext] NVARCHAR(2048) NOT NULL,
    [created_by_user_id] UNIQUEIDENTIFIER NOT NULL,
    [status] NVARCHAR(16) NOT NULL CONSTRAINT [df_customer_invitation_status] DEFAULT N'PENDING',
    [created_at] DATETIME2(3) NOT NULL CONSTRAINT [df_customer_invitation_created] DEFAULT SYSUTCDATETIME(),
    [accepted_user_id] UNIQUEIDENTIFIER NULL,
    [client_account_public_id] UNIQUEIDENTIFIER NULL,
    [completed_at] DATETIME2(3) NULL,
    CONSTRAINT [uq_customer_invitation_reference] UNIQUE ([invitation_reference]),
    CONSTRAINT [ck_customer_invitation_account_type]
        CHECK ([account_type] IN (N'COMPANY', N'SOLE_TRADER', N'INDIVIDUAL')),
    CONSTRAINT [ck_customer_invitation_status]
        CHECK (
            ([status] = N'PENDING' AND [accepted_user_id] IS NULL
                AND [client_account_public_id] IS NULL AND [completed_at] IS NULL)
            OR ([status] = N'COMPLETED' AND [accepted_user_id] IS NOT NULL
                AND [client_account_public_id] IS NOT NULL AND [completed_at] IS NOT NULL)
        )
);
