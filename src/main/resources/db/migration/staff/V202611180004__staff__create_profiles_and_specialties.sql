IF SCHEMA_ID(N'staff') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [staff]');
END;

CREATE TABLE [staff].[staff_profile] (
    [user_public_id] UNIQUEIDENTIFIER NOT NULL CONSTRAINT [pk_staff_profile] PRIMARY KEY,
    [job_title] NVARCHAR(120) NULL,
    [employment_start_date] DATE NOT NULL,
    [status] NVARCHAR(16) NOT NULL CONSTRAINT [df_staff_profile_status] DEFAULT N'ACTIVE',
    [created_at] DATETIME2(3) NOT NULL CONSTRAINT [df_staff_profile_created] DEFAULT SYSUTCDATETIME(),
    [version] BIGINT NOT NULL CONSTRAINT [df_staff_profile_version] DEFAULT 0,
    CONSTRAINT [ck_staff_profile_status] CHECK ([status] IN (N'ACTIVE', N'INACTIVE'))
);

CREATE TABLE [staff].[staff_specialty] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_staff_specialty] PRIMARY KEY,
    [user_public_id] UNIQUEIDENTIFIER NOT NULL,
    [name] NVARCHAR(80) NOT NULL,
    [name_key] NVARCHAR(80) NOT NULL,
    CONSTRAINT [fk_staff_specialty_profile]
        FOREIGN KEY ([user_public_id]) REFERENCES [staff].[staff_profile] ([user_public_id]),
    CONSTRAINT [uq_staff_specialty_worker_name] UNIQUE ([user_public_id], [name_key])
);
CREATE INDEX [ix_staff_profile_status_created]
    ON [staff].[staff_profile] ([status], [created_at]);
