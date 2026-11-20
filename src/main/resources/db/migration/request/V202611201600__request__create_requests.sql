IF SCHEMA_ID(N'request') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [request]');
END;

CREATE TABLE [request].[request_category] (
    [code] NVARCHAR(40) NOT NULL CONSTRAINT [pk_request_category] PRIMARY KEY,
    [active] BIT NOT NULL CONSTRAINT [df_request_category_active] DEFAULT (1)
);
INSERT INTO [request].[request_category] ([code]) VALUES
    (N'WEB_APPLICATION'), (N'INTEGRATION'), (N'AUTOMATION'), (N'CONSULTING'), (N'SIMBIOSPHERE_MODULE');

CREATE TABLE [request].[reference_counter] (
    [business_year] INT NOT NULL CONSTRAINT [pk_request_reference_counter] PRIMARY KEY,
    [last_number] BIGINT NOT NULL CONSTRAINT [df_request_reference_counter_last] DEFAULT (0),
    [version] BIGINT NOT NULL CONSTRAINT [df_request_reference_counter_version] DEFAULT (0)
);

CREATE TABLE [request].[request] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_request] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [reference_code] NVARCHAR(20) NOT NULL,
    [request_type] NVARCHAR(24) NOT NULL,
    [category_code] NVARCHAR(40) NOT NULL,
    [title] NVARCHAR(160) NOT NULL,
    [description] NVARCHAR(10000) NOT NULL,
    [objective] NVARCHAR(2000) NULL,
    [impact] NVARCHAR(4000) NULL,
    [related_module] NVARCHAR(80) NULL,
    [estimated_budget] DECIMAL(19,4) NULL,
    [target_date] DATE NULL,
    [owner_account_id] UNIQUEIDENTIFIER NOT NULL,
    [owner_user_id] UNIQUEIDENTIFIER NOT NULL,
    [status_code] NVARCHAR(32) NOT NULL,
    [paused_from_status] NVARCHAR(32) NULL,
    [decision_reason] NVARCHAR(2000) NULL,
    [duplicate_reference] NVARCHAR(20) NULL,
    [created_at] DATETIME2(3) NOT NULL,
    [version] BIGINT NOT NULL CONSTRAINT [df_request_version] DEFAULT (0),
    CONSTRAINT [uq_request_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_request_reference] UNIQUE ([reference_code]),
    CONSTRAINT [fk_request_category] FOREIGN KEY ([category_code]) REFERENCES [request].[request_category] ([code]),
    CONSTRAINT [ck_request_type] CHECK ([request_type] IN (N'CUSTOM_DEVELOPMENT', N'SIMBIOSPHERE_MODULE')),
    CONSTRAINT [ck_request_status] CHECK ([status_code] IN (N'RECIBIDA', N'EN_ANALISIS', N'EN_ESTUDIO', N'PENDIENTE_INFO_CLIENTE', N'PRESUPUESTO_ENVIADO', N'ACEPTADA', N'RECHAZADA', N'PLANIFICADA', N'DESCARTADA', N'DUPLICADA', N'EN_PAUSA', N'EN_DESARROLLO', N'EN_REVISION_CLIENTE', N'ENTREGADA', N'PUBLICADA', N'CANCELADA', N'CERRADA')),
    CONSTRAINT [ck_request_budget] CHECK ([estimated_budget] IS NULL OR [estimated_budget] >= 0),
    CONSTRAINT [ck_request_title] CHECK (LEN(LTRIM(RTRIM([title]))) > 0),
    CONSTRAINT [ck_request_description] CHECK (LEN(LTRIM(RTRIM([description]))) > 0)
);
CREATE INDEX [ix_request_owner_created] ON [request].[request] ([owner_account_id], [created_at], [id]);
CREATE INDEX [ix_request_owner_status_reference] ON [request].[request] ([owner_account_id], [status_code], [reference_code]);

CREATE TABLE [request].[request_status_history] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_request_status_history] PRIMARY KEY,
    [request_id] BIGINT NOT NULL,
    [from_status] NVARCHAR(32) NULL,
    [to_status] NVARCHAR(32) NOT NULL,
    [actor_id] UNIQUEIDENTIFIER NOT NULL,
    [reason] NVARCHAR(1000) NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [fk_request_status_history] FOREIGN KEY ([request_id]) REFERENCES [request].[request] ([id])
);
CREATE INDEX [ix_request_history_created] ON [request].[request_status_history] ([request_id], [created_at], [id]);

CREATE TABLE [request].[request_attachment] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_request_attachment] PRIMARY KEY,
    [request_id] BIGINT NOT NULL,
    [file_public_id] UNIQUEIDENTIFIER NOT NULL,
    [added_by] UNIQUEIDENTIFIER NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [fk_request_attachment] FOREIGN KEY ([request_id]) REFERENCES [request].[request] ([id]),
    CONSTRAINT [uq_request_attachment_file] UNIQUE ([request_id], [file_public_id])
);
CREATE INDEX [ix_request_attachment_request] ON [request].[request_attachment] ([request_id], [created_at], [id]);

CREATE TABLE [request].[request_assignment] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_request_assignment] PRIMARY KEY,
    [request_id] BIGINT NOT NULL,
    [staff_user_id] UNIQUEIDENTIFIER NOT NULL,
    [responsible] BIT NOT NULL CONSTRAINT [df_request_assignment_responsible] DEFAULT (0),
    [assigned_by] UNIQUEIDENTIFIER NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [fk_request_assignment] FOREIGN KEY ([request_id]) REFERENCES [request].[request] ([id]),
    CONSTRAINT [uq_request_assignment_staff] UNIQUE ([request_id], [staff_user_id])
);
CREATE INDEX [ix_request_assignment_staff] ON [request].[request_assignment] ([staff_user_id], [request_id]);
