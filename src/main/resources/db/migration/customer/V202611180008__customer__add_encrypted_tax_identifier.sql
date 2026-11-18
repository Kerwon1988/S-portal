ALTER TABLE [customer].[client_account]
    ADD [country_code] NCHAR(2) NULL,
        [tax_id_ciphertext] NVARCHAR(2048) NULL;

ALTER TABLE [customer].[client_account]
    ADD CONSTRAINT [ck_customer_account_tax_identifier_pair] CHECK (
        ([country_code] IS NULL AND [tax_id_ciphertext] IS NULL)
        OR ([country_code] IS NOT NULL AND [tax_id_ciphertext] IS NOT NULL));

ALTER TABLE [customer].[client_account]
    ADD CONSTRAINT [ck_customer_account_country_code] CHECK (
        [country_code] IS NULL OR [country_code] LIKE N'[A-Z][A-Z]');
