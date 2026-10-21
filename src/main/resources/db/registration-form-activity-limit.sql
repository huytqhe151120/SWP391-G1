IF EXISTS (
    SELECT activity_id
    FROM dbo.activity_registration_form
    GROUP BY activity_id
    HAVING COUNT(*) > 1
)
BEGIN
    THROW 50001, 'Cannot enforce one registration form per activity: duplicate forms already exist.', 1;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.activity_registration_form')
      AND name = N'UX_activity_registration_form_activity_id'
)
BEGIN
    CREATE UNIQUE INDEX UX_activity_registration_form_activity_id
        ON dbo.activity_registration_form(activity_id);
END;
GO
