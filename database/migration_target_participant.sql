-- =============================================================================
-- Migration Script: Thiết lập đối tượng tham gia hoạt động & Tự động ghi danh
-- Mô tả:
--   1. Tạo bảng dbo.activity_target_participant và Index IX_TargetParticipant_Activity.
--   2. Bổ sung cột registration_status và registration_by vào dbo.activity_participant.
--   3. Cập nhật dữ liệu cũ trong dbo.activity_participant (STAFF_ENROLLED, registration_by = 1).
--   4. Gán đối tượng mặc định (Toàn trường - tất cả FK = NULL) cho các hoạt động hiện có.
-- =============================================================================

SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. Tạo bảng dbo.activity_target_participant nếu chưa tồn tại
    IF OBJECT_ID(N'dbo.activity_target_participant', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.activity_target_participant (
            id                  BIGINT IDENTITY(1,1) NOT NULL,
            activity_id         INT                  NOT NULL,
            department_id       INT                  NULL,
            cohort_id           INT                  NULL,
            major_id            INT                  NULL,
            academic_program_id INT                  NULL,
            main_class_id       INT                  NULL,
            created_at          DATETIME2            NOT NULL CONSTRAINT DF_activity_target_participant_created_at DEFAULT (GETDATE()),
            CONSTRAINT PK_activity_target_participant PRIMARY KEY CLUSTERED (id ASC),
            CONSTRAINT FK_Target_Activity   FOREIGN KEY (activity_id)         REFERENCES dbo.extracurricular_activity(id) ON DELETE CASCADE,
            CONSTRAINT FK_Target_Department FOREIGN KEY (department_id)       REFERENCES dbo.department(id),
            CONSTRAINT FK_Target_Cohort     FOREIGN KEY (cohort_id)           REFERENCES dbo.academic_cohort(id),
            CONSTRAINT FK_Target_Major      FOREIGN KEY (major_id)            REFERENCES dbo.major(id),
            CONSTRAINT FK_Target_Program    FOREIGN KEY (academic_program_id) REFERENCES dbo.academic_program(id),
            CONSTRAINT FK_Target_Class      FOREIGN KEY (main_class_id)       REFERENCES dbo.main_class(id)
        );
    END;

    -- Index tối ưu truy vấn filter sinh viên theo hoạt động
    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = N'IX_TargetParticipant_Activity'
          AND object_id = OBJECT_ID(N'dbo.activity_target_participant')
    )
    BEGIN
        CREATE NONCLUSTERED INDEX IX_TargetParticipant_Activity
        ON dbo.activity_target_participant(activity_id)
        INCLUDE (department_id, cohort_id, major_id, academic_program_id, main_class_id);
    END;

    -- 2. Thêm cột registration_status và registration_by vào dbo.activity_participant
    IF COL_LENGTH('dbo.activity_participant', 'registration_status') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD registration_status VARCHAR(30) NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'registration_by') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD registration_by BIGINT NULL;
    END;

    -- 3. Cập nhật dữ liệu cũ trong bảng dbo.activity_participant
    EXEC sp_executesql N'
        UPDATE dbo.activity_participant
        SET registration_status = ''STAFF_ENROLLED'',
            registration_by = 1
        WHERE registration_status IS NULL;
    ';

    -- Áp dụng ràng buộc NOT NULL và DEFAULT cho registration_status
    ALTER TABLE dbo.activity_participant
    ALTER COLUMN registration_status VARCHAR(30) NOT NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints
        WHERE name = N'DF_activity_participant_registration_status'
          AND parent_object_id = OBJECT_ID(N'dbo.activity_participant')
    )
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD CONSTRAINT DF_activity_participant_registration_status DEFAULT ('STAFF_ENROLLED') FOR registration_status;
    END;

    -- 4. Gán đối tượng mặc định (Toàn trường) cho các hoạt động cũ
    INSERT INTO dbo.activity_target_participant (
        activity_id, department_id, cohort_id, major_id, academic_program_id, main_class_id, created_at
    )
    SELECT id, NULL, NULL, NULL, NULL, NULL, GETDATE()
    FROM dbo.extracurricular_activity
    WHERE id NOT IN (SELECT DISTINCT activity_id FROM dbo.activity_target_participant);

    COMMIT TRANSACTION;
    PRINT N'Migration thành công: Đã tạo bảng dbo.activity_target_participant và cập nhật dbo.activity_participant.';
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
