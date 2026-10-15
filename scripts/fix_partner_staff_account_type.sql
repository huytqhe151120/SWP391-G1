-- Fix: trigger on partner_staff checks account type 'STAFF_PARTNER'; the correct type is 'PARTNER_STAFF'.
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @name NVARCHAR(256), @sql NVARCHAR(MAX), @fixed INT = 0;

    DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
        SELECT QUOTENAME(OBJECT_SCHEMA_NAME(o.object_id)) + N'.' + QUOTENAME(o.name),
               m.definition
        FROM sys.sql_modules m
        JOIN sys.objects o ON o.object_id = m.object_id
        WHERE o.type IN ('TR', 'P', 'FN', 'IF', 'TF')
          AND m.definition LIKE N'%STAFF[_]PARTNER%';

    OPEN cur;
    FETCH NEXT FROM cur INTO @name, @sql;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        SET @sql = REPLACE(@sql, N'STAFF_PARTNER', N'PARTNER_STAFF');
        SET @sql = STUFF(@sql, CHARINDEX(N'CREATE', @sql), 6, N'ALTER');
        EXEC sys.sp_executesql @sql;
        PRINT N'Updated: ' + @name;
        SET @fixed += 1;
        FETCH NEXT FROM cur INTO @name, @sql;
    END
    CLOSE cur;
    DEALLOCATE cur;

    IF @fixed = 0
        PRINT N'No trigger/procedure/function references STAFF_PARTNER. Check the CHECK constraints result set.';

    SELECT name, definition FROM sys.check_constraints WHERE definition LIKE N'%STAFF[_]PARTNER%';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
