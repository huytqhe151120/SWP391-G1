IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.activity_registration_form')
      AND name = N'CK_activity_registration_form_status'
)
BEGIN
    ALTER TABLE dbo.activity_registration_form
        DROP CONSTRAINT CK_activity_registration_form_status;
END;
GO

UPDATE dbo.activity_registration_form
SET status = 'OPENING'
WHERE status = 'OPEN';
GO

ALTER TABLE dbo.activity_registration_form
    ADD CONSTRAINT CK_activity_registration_form_status
        CHECK (status IN ('DRAFT', 'OPENING', 'CLOSED'));
GO
