-- =============================================================================
-- Migration Script: Thiết lập Cấp tổ chức hoạt động (activity_organization_level)
-- Mô tả:
--   1. Tạo/cập nhật bảng dbo.activity_organization_level lưu danh mục cấp tổ chức
--      và hệ số nhân (coefficient DECIMAL(4,2) - làm tròn đến chữ số thập phân thứ 2).
--   2. Thêm dữ liệu mẫu mặc định nếu bảng chưa có dữ liệu.
--   3. Bổ sung cột organization_level_id (NOT NULL, FK) vào dbo.extracurricular_activity.
-- =============================================================================

SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. Tạo bảng dbo.activity_organization_level nếu chưa tồn tại
    IF OBJECT_ID(N'dbo.activity_organization_level', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.activity_organization_level (
            id          INT IDENTITY(1,1) NOT NULL,
            code        VARCHAR(50)       NOT NULL,
            name        NVARCHAR(255)     NOT NULL,
            coefficient DECIMAL(4,2)      NOT NULL CONSTRAINT DF_activity_organization_level_coefficient DEFAULT (1.00),
            created_at  DATETIME2         NOT NULL CONSTRAINT DF_activity_organization_level_created_at DEFAULT (SYSUTCDATETIME()),
            updated_at  DATETIME2         NOT NULL CONSTRAINT DF_activity_organization_level_updated_at DEFAULT (SYSUTCDATETIME()),
            CONSTRAINT PK_activity_organization_level PRIMARY KEY CLUSTERED (id ASC),
            CONSTRAINT UQ_activity_organization_level_code UNIQUE (code),
            CONSTRAINT CK_activity_organization_level_coefficient CHECK (coefficient >= 0.01 AND coefficient <= 10.00)
        );
    END
    ELSE
    BEGIN
        -- Đảm bảo cột coefficient có kiểu DECIMAL(4,2) (làm tròn 2 chữ số sau dấu phẩy)
        IF EXISTS (
            SELECT 1
            FROM sys.check_constraints
            WHERE name = N'CK_activity_organization_level_coefficient'
              AND parent_object_id = OBJECT_ID(N'dbo.activity_organization_level')
        )
        BEGIN
            ALTER TABLE dbo.activity_organization_level
            DROP CONSTRAINT CK_activity_organization_level_coefficient;
        END;

        IF EXISTS (
            SELECT 1
            FROM sys.default_constraints
            WHERE name = N'DF_activity_organization_level_coefficient'
              AND parent_object_id = OBJECT_ID(N'dbo.activity_organization_level')
        )
        BEGIN
            ALTER TABLE dbo.activity_organization_level
            DROP CONSTRAINT DF_activity_organization_level_coefficient;
        END;

        ALTER TABLE dbo.activity_organization_level
        ALTER COLUMN coefficient DECIMAL(4,2) NOT NULL;

        ALTER TABLE dbo.activity_organization_level
        ADD CONSTRAINT DF_activity_organization_level_coefficient DEFAULT (1.00) FOR coefficient;

        ALTER TABLE dbo.activity_organization_level
        WITH CHECK ADD CONSTRAINT CK_activity_organization_level_coefficient
        CHECK (coefficient >= 0.01 AND coefficient <= 10.00);
    END;

    -- 2. Thêm dữ liệu danh mục Cấp tổ chức mặc định nếu bảng đang trống
    IF NOT EXISTS (SELECT 1 FROM dbo.activity_organization_level)
    BEGIN
        INSERT INTO dbo.activity_organization_level (code, name, coefficient)
        VALUES
            ('LV1', N'Cấp Lớp/Câu lạc bộ', 1.00),
            ('LV2', N'Cấp Khoa',           1.30),
            ('LV3', N'Cấp Trường',         1.50),
            ('LV4', N'Cấp Phường/Xã',      1.50),
            ('LV5', N'Cấp Thành Phố',      1.80),
            ('LV6', N'Cấp Quốc gia',       2.00);
    END;

    -- 3. Thêm cột organization_level_id vào bảng dbo.extracurricular_activity
    IF COL_LENGTH('dbo.extracurricular_activity', 'organization_level_id') IS NULL
    BEGIN
        ALTER TABLE dbo.extracurricular_activity
        ADD organization_level_id INT NULL;
    END;

    -- 4. Cập nhật giá trị mặc định cho các bản ghi hoạt động hiện có trước khi đặt NOT NULL
    DECLARE @DefaultLevelId INT;
    SELECT TOP 1 @DefaultLevelId = id
    FROM dbo.activity_organization_level
    ORDER BY id ASC;

    EXEC sp_executesql
        N'UPDATE dbo.extracurricular_activity SET organization_level_id = @DefId WHERE organization_level_id IS NULL;',
        N'@DefId INT',
        @DefId = @DefaultLevelId;

    -- 5. Thiết lập ràng buộc NOT NULL cho cột organization_level_id
    ALTER TABLE dbo.extracurricular_activity
    ALTER COLUMN organization_level_id INT NOT NULL;

    -- 6. Thiết lập khóa ngoại FK_extracurricular_activity_organization_level
    IF NOT EXISTS (
        SELECT 1
        FROM sys.foreign_keys
        WHERE name = N'FK_extracurricular_activity_organization_level'
          AND parent_object_id = OBJECT_ID(N'dbo.extracurricular_activity')
    )
    BEGIN
        ALTER TABLE dbo.extracurricular_activity
        WITH CHECK ADD CONSTRAINT FK_extracurricular_activity_organization_level
        FOREIGN KEY (organization_level_id)
        REFERENCES dbo.activity_organization_level (id);

        ALTER TABLE dbo.extracurricular_activity
        CHECK CONSTRAINT FK_extracurricular_activity_organization_level;
    END;

    COMMIT TRANSACTION;
    PRINT N'Migration thành công: Đã cập nhật bảng dbo.activity_organization_level (DECIMAL(4,2)) và dbo.extracurricular_activity.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
    DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
    DECLARE @ErrorState INT = ERROR_STATE();
    RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
END CATCH;
GO
