IF SCHEMA_ID(N'legal') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [legal]');
END;

CREATE TABLE [legal].[consent_record] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_legal_consent_record] PRIMARY KEY,
    [public_id] UNIQUEIDENTIFIER NOT NULL,
    [subject_user_id] UNIQUEIDENTIFIER NOT NULL,
    [purpose_code] NVARCHAR(40) NOT NULL,
    [document_type] NVARCHAR(32) NOT NULL,
    [document_locale] NCHAR(2) NOT NULL,
    [document_version] INT NOT NULL,
    [accepted_at] DATETIME2(3) NOT NULL,
    [source_code] NVARCHAR(32) NOT NULL,
    CONSTRAINT [uq_legal_consent_public_id] UNIQUE ([public_id]),
    CONSTRAINT [ck_legal_consent_purpose] CHECK (
        [purpose_code] IN (N'TERMS_ACCEPTANCE', N'PRIVACY_ACKNOWLEDGEMENT', N'MARKETING_COMMUNICATIONS')),
    CONSTRAINT [ck_legal_consent_document] CHECK (
        ([purpose_code] = N'TERMS_ACCEPTANCE' AND [document_type] = N'TERMS')
        OR ([purpose_code] = N'PRIVACY_ACKNOWLEDGEMENT' AND [document_type] = N'PRIVACY')
        OR ([purpose_code] = N'MARKETING_COMMUNICATIONS' AND [document_type] = N'MARKETING')),
    CONSTRAINT [ck_legal_consent_locale] CHECK ([document_locale] IN (N'es', N'en')),
    CONSTRAINT [ck_legal_consent_version] CHECK ([document_version] > 0),
    CONSTRAINT [ck_legal_consent_source] CHECK (
        [source_code] IN (N'REGISTRATION', N'ADMIN_INVITATION', N'ACCOUNT_SETTINGS'))
);

CREATE INDEX [ix_legal_consent_subject_purpose_date]
    ON [legal].[consent_record] ([subject_user_id], [purpose_code], [accepted_at]);
