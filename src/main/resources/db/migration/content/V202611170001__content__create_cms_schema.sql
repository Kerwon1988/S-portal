IF SCHEMA_ID(N'content') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [content]');
END;

CREATE TABLE [content].[page] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_content_page] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [page_key] NVARCHAR(120) NOT NULL,
    [page_type] NVARCHAR(32) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_content_page_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_content_page_key] UNIQUE ([page_key]),
    CONSTRAINT [ck_content_page_type] CHECK ([page_type] IN (N'PAGE', N'LEGAL', N'REQUEST_CONDITIONS'))
);

CREATE TABLE [content].[page_translation] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_content_page_translation] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [page_id] BIGINT NOT NULL,
    [locale] NCHAR(2) NOT NULL,
    [slug] NVARCHAR(120) NOT NULL,
    [title] NVARCHAR(200) NOT NULL,
    [seo_title] NVARCHAR(200) NOT NULL,
    [meta_description] NVARCHAR(320) NOT NULL,
    [published_slug] NVARCHAR(120) NULL,
    [published_snapshot_json] NVARCHAR(MAX) NULL,
    [published_at] DATETIME2(3) NULL,
    [state_code] NVARCHAR(16) NOT NULL,
    [approval_state] NVARCHAR(16) NOT NULL,
    [version_no] INT NOT NULL CONSTRAINT [df_content_translation_version] DEFAULT 0,
    [updated_by] UNIQUEIDENTIFIER NOT NULL,
    [updated_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_content_translation_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_content_translation_locale_slug] UNIQUE ([locale], [slug]),
    CONSTRAINT [uq_content_translation_page_locale] UNIQUE ([page_id], [locale]),
    CONSTRAINT [fk_content_translation_page] FOREIGN KEY ([page_id]) REFERENCES [content].[page] ([id]),
    CONSTRAINT [ck_content_translation_locale] CHECK ([locale] IN (N'es', N'en')),
    CONSTRAINT [ck_content_translation_state] CHECK ([state_code] IN (N'DRAFT', N'PUBLISHED')),
    CONSTRAINT [ck_content_translation_approval] CHECK ([approval_state] IN (N'PENDING', N'APPROVED')),
    CONSTRAINT [ck_content_translation_slug] CHECK (
        [slug] NOT LIKE N'%..%' AND [slug] NOT LIKE N'/%' AND [slug] NOT LIKE N'%/')
);

CREATE INDEX [ix_content_translation_state_locale]
    ON [content].[page_translation] ([state_code], [locale], [slug]);

CREATE UNIQUE INDEX [uq_content_translation_published_locale_slug]
    ON [content].[page_translation] ([locale], [published_slug])
    WHERE [published_slug] IS NOT NULL;

CREATE TABLE [content].[content_block] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_content_block] PRIMARY KEY,
    [translation_id] BIGINT NOT NULL,
    [block_type] NVARCHAR(16) NOT NULL,
    [position_no] INT NOT NULL,
    [heading] NVARCHAR(200) NOT NULL,
    [body_markdown] NVARCHAR(8000) NOT NULL,
    [target_url] NVARCHAR(512) NULL,
    [media_public_id] UNIQUEIDENTIFIER NULL,
    CONSTRAINT [fk_content_block_translation] FOREIGN KEY ([translation_id])
        REFERENCES [content].[page_translation] ([id]),
    CONSTRAINT [uq_content_block_position] UNIQUE ([translation_id], [position_no]),
    CONSTRAINT [ck_content_block_type] CHECK ([block_type] IN (N'HERO', N'CARD', N'TEXT', N'IMAGE', N'CTA', N'FAQ'))
);

CREATE TABLE [content].[page_version] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_content_page_version] PRIMARY KEY,
    [translation_public_id] UNIQUEIDENTIFIER NOT NULL,
    [version_no] INT NOT NULL,
    [snapshot_json] NVARCHAR(MAX) NOT NULL,
    [actor_id] UNIQUEIDENTIFIER NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_content_page_version_translation_version] UNIQUE ([translation_public_id], [version_no])
);

CREATE TABLE [content].[slug_redirect] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_content_slug_redirect] PRIMARY KEY,
    [locale] NCHAR(2) NOT NULL,
    [old_slug] NVARCHAR(120) NOT NULL,
    [new_slug] NVARCHAR(120) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_content_slug_redirect_locale_old] UNIQUE ([locale], [old_slug]),
    CONSTRAINT [ck_content_slug_redirect_locale] CHECK ([locale] IN (N'es', N'en'))
);

CREATE TABLE [content].[media_asset] (
    [file_public_id] UNIQUEIDENTIFIER NOT NULL CONSTRAINT [pk_content_media_asset] PRIMARY KEY,
    [alt_es] NVARCHAR(500) NOT NULL,
    [alt_en] NVARCHAR(500) NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [ck_content_media_alt_es] CHECK (LEN(LTRIM(RTRIM([alt_es]))) > 0),
    CONSTRAINT [ck_content_media_alt_en] CHECK (LEN(LTRIM(RTRIM([alt_en]))) > 0)
);

CREATE TABLE [content].[team_member] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_content_team_member] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [position_no] INT NOT NULL,
    [active] BIT NOT NULL,
    [materials_approved] BIT NOT NULL CONSTRAINT [df_content_team_materials_approved] DEFAULT 0,
    CONSTRAINT [uq_content_team_member_public_id] UNIQUE ([public_id])
);

CREATE TABLE [content].[team_member_translation] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_content_team_member_translation] PRIMARY KEY,
    [member_id] BIGINT NOT NULL,
    [locale] NCHAR(2) NOT NULL,
    [display_name] NVARCHAR(120) NOT NULL,
    [role_title] NVARCHAR(120) NOT NULL,
    [bio_markdown] NVARCHAR(4000) NOT NULL,
    [photo_public_id] UNIQUEIDENTIFIER NULL,
    [photo_alt] NVARCHAR(500) NOT NULL,
    [profile_url] NVARCHAR(512) NULL,
    CONSTRAINT [fk_content_team_translation_member] FOREIGN KEY ([member_id])
        REFERENCES [content].[team_member] ([id]),
    CONSTRAINT [uq_content_team_translation_member_locale] UNIQUE ([member_id], [locale]),
    CONSTRAINT [ck_content_team_locale] CHECK ([locale] IN (N'es', N'en'))
);

CREATE TABLE [content].[legal_document_version] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_content_legal_document_version] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [document_type] NVARCHAR(32) NOT NULL,
    [locale] NCHAR(2) NOT NULL,
    [version_no] INT NOT NULL,
    [title] NVARCHAR(200) NOT NULL,
    [content_markdown] NVARCHAR(MAX) NOT NULL,
    [approval_state] NVARCHAR(16) NOT NULL,
    [effective_from] DATETIME2(3) NULL,
    [actor_id] UNIQUEIDENTIFIER NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_content_legal_document_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_content_legal_version] UNIQUE ([document_type], [locale], [version_no]),
    CONSTRAINT [ck_content_legal_locale] CHECK ([locale] IN (N'es', N'en')),
    CONSTRAINT [ck_content_legal_approval] CHECK ([approval_state] IN (N'PENDING', N'APPROVED'))
);
