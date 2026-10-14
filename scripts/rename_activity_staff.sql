SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.activity_staff', N'U') IS NOT NULL
       AND OBJECT_ID(N'dbo.activity_staff_assignment', N'U') IS NOT NULL
        THROW 50000, 'Both dbo.activity_staff and dbo.activity_staff_assignment exist.', 1;

    IF OBJECT_ID(N'dbo.activity_partner_staff', N'U') IS NOT NULL
       AND OBJECT_ID(N'dbo.activity_partner_staff_assignment', N'U') IS NOT NULL
        THROW 50000, 'Both dbo.activity_partner_staff and dbo.activity_partner_staff_assignment exist.', 1;

    IF OBJECT_ID(N'dbo.activity_staff', N'U') IS NULL
       AND OBJECT_ID(N'dbo.activity_staff_assignment', N'U') IS NULL
        THROW 50000, 'Neither dbo.activity_staff nor dbo.activity_staff_assignment was found.', 1;

    IF OBJECT_ID(N'dbo.activity_partner_staff', N'U') IS NULL
       AND OBJECT_ID(N'dbo.activity_partner_staff_assignment', N'U') IS NULL
        THROW 50000, 'Neither dbo.activity_partner_staff nor dbo.activity_partner_staff_assignment was found.', 1;

    IF OBJECT_ID(N'dbo.activity_staff', N'U') IS NOT NULL
        EXEC sys.sp_rename
            @objname = N'dbo.activity_staff',
            @newname = N'activity_staff_assignment',
            @objtype = N'OBJECT';

    IF OBJECT_ID(N'dbo.activity_partner_staff', N'U') IS NOT NULL
        EXEC sys.sp_rename
            @objname = N'dbo.activity_partner_staff',
            @newname = N'activity_partner_staff_assignment',
            @objtype = N'OBJECT';

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
