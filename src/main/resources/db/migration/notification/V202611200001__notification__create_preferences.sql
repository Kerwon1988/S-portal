IF SCHEMA_ID(N'notification') IS NULL
BEGIN
    EXEC(N'CREATE SCHEMA [notification]');
END;

CREATE TABLE [notification].[notification_preference] (
    [id] UNIQUEIDENTIFIER NOT NULL CONSTRAINT [pk_notification_preference] PRIMARY KEY,
    [user_id] UNIQUEIDENTIFIER NOT NULL,
    [category] NVARCHAR(32) NOT NULL,
    [enabled] BIT NOT NULL CONSTRAINT [df_notification_preference_enabled] DEFAULT 1,
    [updated_at] DATETIME2(3) NOT NULL,
    CONSTRAINT [uk_notification_preference_user_category] UNIQUE ([user_id], [category]),
    CONSTRAINT [ck_notification_preference_category]
        CHECK ([category] IN (N'REQUEST_UPDATES', N'MESSAGE_UPDATES'))
);
CREATE INDEX [ix_notification_preference_user]
    ON [notification].[notification_preference] ([user_id], [category]);
