IF SCHEMA_ID(N'audit') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [audit]');
END;

CREATE TABLE [audit].[audit_entry] (
    [id] BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT [pk_audit_entry] PRIMARY KEY,
    [actor_id] UNIQUEIDENTIFIER NULL,
    [action_code] NVARCHAR(64) NOT NULL,
    [entity_type] NVARCHAR(64) NOT NULL,
    [entity_public_id] UNIQUEIDENTIFIER NULL,
    [occurred_at] DATETIME2(3) NOT NULL CONSTRAINT [df_audit_entry_occurred_at] DEFAULT SYSUTCDATETIME(),
    [correlation_id] NVARCHAR(64) NOT NULL
);

CREATE INDEX [ix_audit_entry_entity_occurred]
    ON [audit].[audit_entry] ([entity_type], [entity_public_id], [occurred_at]);
CREATE INDEX [ix_audit_entry_actor_occurred]
    ON [audit].[audit_entry] ([actor_id], [occurred_at]);
