ALTER TABLE [simbiosphere].[provisioning] ADD [owner_user_public_id] UNIQUEIDENTIFIER NULL;
ALTER TABLE [simbiosphere].[provisioning] ADD [business_name] NVARCHAR(200) NULL;
ALTER TABLE [simbiosphere].[provisioning] ADD [business_slug] NVARCHAR(50) NULL;
ALTER TABLE [simbiosphere].[provisioning] ADD [business_type] NVARCHAR(32) NULL;
ALTER TABLE [simbiosphere].[provisioning] ADD [location_name] NVARCHAR(200) NULL;
ALTER TABLE [simbiosphere].[provisioning] ADD [location_slug] NVARCHAR(50) NULL;
ALTER TABLE [simbiosphere].[provisioning] ADD [modules_json] NVARCHAR(2048) NULL;
ALTER TABLE [simbiosphere].[provisioning] ADD [terms_version] NVARCHAR(64) NULL;
ALTER TABLE [simbiosphere].[provisioning] ADD [terms_accepted_at] DATETIME2(3) NULL;

CREATE INDEX [ix_simbiosphere_provisioning_owner_user]
    ON [simbiosphere].[provisioning] ([owner_user_public_id], [status_code]);
