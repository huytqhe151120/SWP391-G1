IF OBJECT_ID(N'dbo.activity_registration', N'U') IS NULL
BEGIN
    THROW 50002, 'Table dbo.activity_registration does not exist.', 1;
END;
GO

IF EXISTS (
    SELECT 1
    FROM dbo.activity_registration
    WHERE status IS NULL OR status NOT IN ('SUBMITTED', 'APPROVED', 'REJECTED')
)
BEGIN
    THROW 50003, 'Cannot enforce registration application statuses: unsupported status values exist.', 1;
END;
GO

DECLARE @constraintName SYSNAME;
DECLARE @dropConstraintSql NVARCHAR(MAX);

DECLARE status_constraint_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT name
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.activity_registration')
      AND definition LIKE N'%status%'
      AND definition LIKE N'%SUBMITTED%'
      AND definition LIKE N'%APPROVED%';

OPEN status_constraint_cursor;
FETCH NEXT FROM status_constraint_cursor INTO @constraintName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @dropConstraintSql = N'ALTER TABLE dbo.activity_registration DROP CONSTRAINT '
        + QUOTENAME(@constraintName) + N';';
    EXEC sys.sp_executesql @dropConstraintSql;
    FETCH NEXT FROM status_constraint_cursor INTO @constraintName;
END;

CLOSE status_constraint_cursor;
DEALLOCATE status_constraint_cursor;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.activity_registration')
      AND name = N'CK_activity_registration_status'
)
BEGIN
    ALTER TABLE dbo.activity_registration
        ADD CONSTRAINT CK_activity_registration_status
        CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED'));
END;
GO
