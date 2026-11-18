ALTER TABLE [files].[file_variant]
    ADD [size_bytes] BIGINT NOT NULL
        CONSTRAINT [df_file_variant_size_bytes] DEFAULT 0;

ALTER TABLE [files].[file_variant]
    ADD CONSTRAINT [ck_file_variant_size_bytes] CHECK ([size_bytes] >= 0);
