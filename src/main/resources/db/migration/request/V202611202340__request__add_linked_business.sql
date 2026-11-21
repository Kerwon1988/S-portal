ALTER TABLE [request].[request]
    ADD [linked_business_public_id] UNIQUEIDENTIFIER NULL;

CREATE INDEX [ix_request_linked_business]
    ON [request].[request] ([linked_business_public_id]);
