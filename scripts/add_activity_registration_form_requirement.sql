SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.extracurricular_activity', N'U') IS NULL
        THROW 50000, 'Table dbo.extracurricular_activity was not found.', 1;

    IF COL_LENGTH(N'dbo.extracurricular_activity', N'start_time') IS NULL
       OR COL_LENGTH(N'dbo.extracurricular_activity', N'end_time') IS NULL
        THROW 50000, 'Required start_time or end_time column was not found.', 1;

    IF EXISTS (
        SELECT 1
        FROM dbo.extracurricular_activity
        WHERE start_time IS NULL OR end_time IS NULL
    )
    BEGIN
        SELECT id, start_time, end_time
        FROM dbo.extracurricular_activity
        WHERE start_time IS NULL OR end_time IS NULL;

        THROW 50000, 'Cannot make start_time and end_time NOT NULL while existing rows contain NULL. Correct those rows and rerun this script.', 1;
    END;

    DECLARE @startTimeType NVARCHAR(128);
    DECLARE @endTimeType NVARCHAR(128);
    DECLARE @sql NVARCHAR(MAX);

    SELECT @startTimeType =
        CASE
            WHEN TYPE_NAME(c.system_type_id) IN (N'datetime2', N'time', N'datetimeoffset')
                THEN TYPE_NAME(c.system_type_id) + N'(' + CONVERT(NVARCHAR(10), c.scale) + N')'
            ELSE TYPE_NAME(c.system_type_id)
        END
    FROM sys.columns c
    WHERE c.object_id = OBJECT_ID(N'dbo.extracurricular_activity')
      AND c.name = N'start_time';

    SELECT @endTimeType =
        CASE
            WHEN TYPE_NAME(c.system_type_id) IN (N'datetime2', N'time', N'datetimeoffset')
                THEN TYPE_NAME(c.system_type_id) + N'(' + CONVERT(NVARCHAR(10), c.scale) + N')'
            ELSE TYPE_NAME(c.system_type_id)
        END
    FROM sys.columns c
    WHERE c.object_id = OBJECT_ID(N'dbo.extracurricular_activity')
      AND c.name = N'end_time';

    IF EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.extracurricular_activity')
          AND name IN (N'start_time', N'end_time')
          AND system_type_id NOT IN (40, 41, 42, 43, 58, 61)
    )
        THROW 50000, 'start_time or end_time has an unsupported SQL data type.', 1;

    IF EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.extracurricular_activity')
          AND name IN (N'start_time', N'end_time')
          AND is_nullable = 1
    )
    BEGIN
        SET @sql =
            N'ALTER TABLE dbo.extracurricular_activity ALTER COLUMN start_time '
            + @startTimeType + N' NOT NULL; '
            + N'ALTER TABLE dbo.extracurricular_activity ALTER COLUMN end_time '
            + @endTimeType + N' NOT NULL;';
        EXEC sys.sp_executesql @sql;
    END;

    IF COL_LENGTH(N'dbo.extracurricular_activity', N'registration_form_required') IS NULL
    BEGIN
        ALTER TABLE dbo.extracurricular_activity
        ADD registration_form_required BIT NOT NULL
            CONSTRAINT DF_extracurricular_activity_registration_form_required DEFAULT (0)
            WITH VALUES;
    END
    ELSE
    BEGIN
        IF EXISTS (
            SELECT 1
            FROM sys.columns
            WHERE object_id = OBJECT_ID(N'dbo.extracurricular_activity')
              AND name = N'registration_form_required'
              AND TYPE_NAME(system_type_id) <> N'bit'
        )
            THROW 50000, 'registration_form_required exists but is not a BIT column.', 1;

        IF EXISTS (
            SELECT 1
            FROM sys.columns
            WHERE object_id = OBJECT_ID(N'dbo.extracurricular_activity')
              AND name = N'registration_form_required'
              AND is_nullable = 1
        )
        BEGIN
            -- Dynamic SQL: the column may still be nullable here, so the IDE's static check is a false positive.
            EXEC sys.sp_executesql N'UPDATE dbo.extracurricular_activity SET registration_form_required = 0 WHERE registration_form_required IS NULL;';
            ALTER TABLE dbo.extracurricular_activity
            ALTER COLUMN registration_form_required BIT NOT NULL;
        END;

        IF NOT EXISTS (
            SELECT 1
            FROM sys.default_constraints dc
            JOIN sys.columns c
              ON c.object_id = dc.parent_object_id
             AND c.column_id = dc.parent_column_id
            WHERE dc.parent_object_id = OBJECT_ID(N'dbo.extracurricular_activity')
              AND c.name = N'registration_form_required'
        )
            ALTER TABLE dbo.extracurricular_activity
            ADD CONSTRAINT DF_extracurricular_activity_registration_form_required
                DEFAULT (0) FOR registration_form_required;
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
