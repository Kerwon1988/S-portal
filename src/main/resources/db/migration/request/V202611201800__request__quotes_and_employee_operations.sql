ALTER TABLE [request].[request]
    ADD [priority_code] NVARCHAR(16) NOT NULL CONSTRAINT [df_request_priority] DEFAULT (N'NORMAL');
ALTER TABLE [request].[request]
    ADD [estimate_hours] DECIMAL(9,2) NULL;
ALTER TABLE [request].[request]
    ADD [first_response_at] DATETIME2(3) NULL;
ALTER TABLE [request].[request]
    ADD CONSTRAINT [ck_request_priority] CHECK ([priority_code] IN (N'LOW', N'NORMAL', N'HIGH', N'URGENT'));
ALTER TABLE [request].[request]
    ADD CONSTRAINT [ck_request_estimate] CHECK ([estimate_hours] IS NULL OR [estimate_hours] >= 0);
CREATE UNIQUE INDEX [ux_request_assignment_responsible]
    ON [request].[request_assignment] ([request_id]) WHERE [responsible] = 1;
CREATE INDEX [ix_request_employee_created] ON [request].[request] ([status_code], [created_at], [id]);
CREATE INDEX [ix_request_employee_filter] ON [request].[request] ([request_type], [category_code], [created_at], [id]);

CREATE TABLE [request].[quote] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_quote] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [request_id] BIGINT NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uq_quote_public_id] UNIQUE ([public_id]),
    CONSTRAINT [uq_quote_request] UNIQUE ([request_id]),
    CONSTRAINT [fk_quote_request] FOREIGN KEY ([request_id]) REFERENCES [request].[request] ([id])
);

CREATE TABLE [request].[quote_version] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_quote_version] PRIMARY KEY,
    [quote_id] BIGINT NOT NULL,
    [version_number] INT NOT NULL,
    [net_amount] DECIMAL(19,4) NOT NULL,
    [currency] CHAR(3) NOT NULL,
    [tax_rate] DECIMAL(7,4) NULL,
    [tax_amount] DECIMAL(19,4) NULL,
    [gross_amount] DECIMAL(19,4) NOT NULL,
    [valid_until] DATE NULL,
    [status_code] NVARCHAR(16) NOT NULL,
    [created_by] UNIQUEIDENTIFIER NOT NULL,
    [created_at] DATETIME2(3) NOT NULL,
    [sent_at] DATETIME2(3) NULL,
    [decided_by] UNIQUEIDENTIFIER NULL,
    [decided_at] DATETIME2(3) NULL,
    [row_version] BIGINT NOT NULL CONSTRAINT [df_quote_version_row] DEFAULT (0),
    CONSTRAINT [fk_quote_version_quote] FOREIGN KEY ([quote_id]) REFERENCES [request].[quote] ([id]),
    CONSTRAINT [uq_quote_version] UNIQUE ([quote_id], [version_number]),
    CONSTRAINT [ck_quote_version_positive] CHECK ([version_number] > 0),
    CONSTRAINT [ck_quote_amounts] CHECK ([net_amount] >= 0 AND [gross_amount] >= 0),
    CONSTRAINT [ck_quote_tax] CHECK ([tax_rate] IS NULL OR ([tax_rate] >= 0 AND [tax_rate] <= 100)),
    CONSTRAINT [ck_quote_currency] CHECK ([currency] LIKE N'[A-Z][A-Z][A-Z]'),
    CONSTRAINT [ck_quote_status] CHECK ([status_code] IN (N'DRAFT', N'SENT', N'ACCEPTED', N'REJECTED', N'EXPIRED')),
    CONSTRAINT [ck_quote_decision] CHECK (
        ([status_code] NOT IN (N'ACCEPTED', N'REJECTED') AND [decided_by] IS NULL)
        OR ([status_code] IN (N'ACCEPTED', N'REJECTED') AND [decided_by] IS NOT NULL AND [decided_at] IS NOT NULL))
);
CREATE INDEX [ix_quote_version_latest] ON [request].[quote_version] ([quote_id], [version_number] DESC);
CREATE INDEX [ix_quote_version_expiry] ON [request].[quote_version] ([status_code], [valid_until], [id])
    WHERE [status_code] = N'SENT';
