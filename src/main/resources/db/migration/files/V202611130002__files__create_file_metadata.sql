IF SCHEMA_ID(N'files') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [files]');
END;

CREATE TABLE [files].[file_metadata] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_file_metadata] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [storage_key] NVARCHAR(160) NOT NULL,
    [original_name] NVARCHAR(255) NOT NULL,
    [detected_mime] NVARCHAR(127) NOT NULL,
    [size_bytes] BIGINT NOT NULL,
    [scan_state] NVARCHAR(16) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL CONSTRAINT [df_file_metadata_created_at] DEFAULT SYSUTCDATETIME(),
    CONSTRAINT [uq_file_metadata_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_file_metadata_storage_key] UNIQUE ([storage_key]),
    CONSTRAINT [ck_file_metadata_size] CHECK ([size_bytes] > 0),
    CONSTRAINT [ck_file_metadata_scan_state] CHECK ([scan_state] IN (N'QUARANTINED', N'CLEAN', N'REJECTED'))
);

CREATE TABLE [files].[file_variant] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_file_variant] PRIMARY KEY,
    [file_id] BIGINT NOT NULL,
    [variant_code] NVARCHAR(32) NOT NULL,
    [storage_key] NVARCHAR(160) NOT NULL,
    [width] INT NOT NULL,
    [height] INT NOT NULL,
    [mime] NVARCHAR(127) NOT NULL,
    CONSTRAINT [fk_file_variant_file] FOREIGN KEY ([file_id]) REFERENCES [files].[file_metadata] ([id]),
    CONSTRAINT [uq_file_variant_code] UNIQUE ([file_id], [variant_code]),
    CONSTRAINT [uq_file_variant_storage_key] UNIQUE ([storage_key])
);
