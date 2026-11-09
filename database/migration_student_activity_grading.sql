-- =============================================================================
-- Migration Script: Luồng chấm điểm hoạt động của sinh viên
-- Mô tả:
--   1. Bổ sung các cột chấm điểm đánh giá tham gia (0-5) kèm feedback và
--      điểm bài thu hoạch (0-10) vào bảng dbo.activity_participant.
--   2. Mở rộng bảng dbo.student_activity_result để lưu đầy đủ các đầu điểm:
--      attendance_score, participation_eval_score, participation_score,
--      summary_score, average_score, org_level_coefficient,
--      early_reg_coefficient, final_point.
--   3. Tạo bảng dbo.activity_grade_history lưu vết người chấm và thời gian chấm.
--   4. Đồng bộ/tính toán dữ liệu điểm cho các bản ghi hiện có.
-- =============================================================================

SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. Bổ sung các cột chấm điểm vào bảng dbo.activity_participant
    IF COL_LENGTH('dbo.activity_participant', 'participation_eval_score') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD participation_eval_score DECIMAL(4,2) NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'participation_feedback') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD participation_feedback NVARCHAR(1000) NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'participation_graded_by') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD participation_graded_by NVARCHAR(200) NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'participation_graded_by_id') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD participation_graded_by_id INT NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'participation_graded_at') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD participation_graded_at DATETIME2 NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'summary_score') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD summary_score DECIMAL(4,2) NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'summary_feedback') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD summary_feedback NVARCHAR(1000) NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'summary_graded_by') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD summary_graded_by NVARCHAR(200) NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'summary_graded_by_id') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD summary_graded_by_id INT NULL;
    END;

    IF COL_LENGTH('dbo.activity_participant', 'summary_graded_at') IS NULL
    BEGIN
        ALTER TABLE dbo.activity_participant
        ADD summary_graded_at DATETIME2 NULL;
    END;

    -- 2. Mở rộng bảng dbo.student_activity_result
    IF COL_LENGTH('dbo.student_activity_result', 'attendance_score') IS NULL
    BEGIN
        ALTER TABLE dbo.student_activity_result
        ADD attendance_score DECIMAL(5,2) NULL;
    END;

    IF COL_LENGTH('dbo.student_activity_result', 'participation_eval_score') IS NULL
    BEGIN
        ALTER TABLE dbo.student_activity_result
        ADD participation_eval_score DECIMAL(5,2) NULL;
    END;

    IF COL_LENGTH('dbo.student_activity_result', 'participation_score') IS NULL
    BEGIN
        ALTER TABLE dbo.student_activity_result
        ADD participation_score DECIMAL(5,2) NULL;
    END;

    IF COL_LENGTH('dbo.student_activity_result', 'summary_score') IS NULL
    BEGIN
        ALTER TABLE dbo.student_activity_result
        ADD summary_score DECIMAL(5,2) NULL;
    END;

    IF COL_LENGTH('dbo.student_activity_result', 'average_score') IS NULL
    BEGIN
        ALTER TABLE dbo.student_activity_result
        ADD average_score DECIMAL(5,2) NULL;
    END;

    IF COL_LENGTH('dbo.student_activity_result', 'org_level_coefficient') IS NULL
    BEGIN
        ALTER TABLE dbo.student_activity_result
        ADD org_level_coefficient DECIMAL(4,2) NULL;
    END;

    IF COL_LENGTH('dbo.student_activity_result', 'early_reg_coefficient') IS NULL
    BEGIN
        ALTER TABLE dbo.student_activity_result
        ADD early_reg_coefficient DECIMAL(4,2) NULL;
    END;

    -- Đảm bảo final_point đủ độ rộng DECIMAL(6,2) và cập nhật CHECK constraint cho phép điểm âm (-5 khi vắng) và nhân hệ số (>10)
    IF EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE name = N'CHK_Result_FinalPoint'
          AND parent_object_id = OBJECT_ID(N'dbo.student_activity_result')
    )
    BEGIN
        ALTER TABLE dbo.student_activity_result
        DROP CONSTRAINT CHK_Result_FinalPoint;
    END;

    ALTER TABLE dbo.student_activity_result
    ALTER COLUMN final_point DECIMAL(6,2) NULL;

    ALTER TABLE dbo.student_activity_result
    WITH CHECK ADD CONSTRAINT CHK_Result_FinalPoint
    CHECK (final_point IS NULL OR (final_point >= -100.00 AND final_point <= 200.00));

    -- 3. Tạo bảng lịch sử chấm điểm dbo.activity_grade_history
    IF OBJECT_ID(N'dbo.activity_grade_history', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.activity_grade_history (
            id                      INT IDENTITY(1,1) NOT NULL,
            activity_participant_id INT               NOT NULL,
            grade_type              VARCHAR(30)       NOT NULL,
            old_score               DECIMAL(5,2)      NULL,
            new_score               DECIMAL(5,2)      NULL,
            feedback                NVARCHAR(1000)    NULL,
            created_by              NVARCHAR(200)     NOT NULL,
            created_by_id           INT               NULL,
            created_at              DATETIME2         NOT NULL CONSTRAINT DF_activity_grade_history_created_at DEFAULT (SYSUTCDATETIME()),
            CONSTRAINT PK_activity_grade_history PRIMARY KEY CLUSTERED (id ASC),
            CONSTRAINT FK_activity_grade_history_participant FOREIGN KEY (activity_participant_id)
                REFERENCES dbo.activity_participant (id) ON DELETE CASCADE
        );
    END;

    COMMIT TRANSACTION;
    PRINT N'Migration bước 1-3 thành công: Đã cập nhật cấu trúc bảng chấm điểm.';
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

-- 4. Đồng bộ điểm cho toàn bộ bản ghi trong activity_participant sang student_activity_result
BEGIN TRY
    BEGIN TRANSACTION;

    MERGE dbo.student_activity_result AS target
    USING (
        SELECT
            ap.student_id,
            ap.activity_id,
            CASE ap.attendance_status
                WHEN 'ATTENDED' THEN CAST(5.00 AS DECIMAL(5,2))
                WHEN 'IN_LATE'  THEN CAST(3.00 AS DECIMAL(5,2))
                WHEN 'OUT_EARLY' THEN CAST(3.00 AS DECIMAL(5,2))
                WHEN 'ABSENT'   THEN CAST(-5.00 AS DECIMAL(5,2))
                ELSE CAST(0.00 AS DECIMAL(5,2))
            END AS calc_attendance_score,
            CASE
                WHEN ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                THEN COALESCE(ap.participation_eval_score, CAST(0.00 AS DECIMAL(5,2)))
                ELSE CAST(0.00 AS DECIMAL(5,2))
            END AS calc_eval_score,
            CASE
                WHEN ap.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY')
                THEN (
                    CASE ap.attendance_status
                        WHEN 'ATTENDED' THEN CAST(5.00 AS DECIMAL(5,2))
                        ELSE CAST(3.00 AS DECIMAL(5,2))
                    END
                    + COALESCE(ap.participation_eval_score, CAST(0.00 AS DECIMAL(5,2)))
                )
                WHEN ap.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(5,2))
                ELSE CAST(0.00 AS DECIMAL(5,2))
            END AS calc_participation_score,
            ap.summary_score AS calc_summary_score,
            COALESCE(ol.coefficient, CAST(1.00 AS DECIMAL(4,2))) AS calc_org_coeff,
            CASE WHEN ap.registration_status = 'REGISTERED'
                THEN CAST(1.10 AS DECIMAL(4,2))
                ELSE CAST(1.00 AS DECIMAL(4,2))
            END AS calc_reg_coeff,
            ea.is_summary_required,
            ap.attendance_status
        FROM dbo.activity_participant ap
        JOIN dbo.extracurricular_activity ea ON ea.id = ap.activity_id
        LEFT JOIN dbo.activity_organization_level ol ON ol.id = ea.organization_level_id
    ) AS src
    ON (target.student_id = src.student_id AND target.activity_id = src.activity_id)
    WHEN MATCHED THEN
        UPDATE SET
            attendance_score = src.calc_attendance_score,
            participation_eval_score = src.calc_eval_score,
            participation_score = src.calc_participation_score,
            summary_score = src.calc_summary_score,
            average_score = CASE
                WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                    CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                        THEN ROUND((src.calc_participation_score + src.calc_summary_score) / 2.0, 2)
                        ELSE src.calc_participation_score
                    END
                WHEN src.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(5,2))
                ELSE CAST(0.00 AS DECIMAL(5,2))
            END,
            org_level_coefficient = src.calc_org_coeff,
            early_reg_coefficient = src.calc_reg_coeff,
            final_point = CASE
                WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                    ROUND(
                        (CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                            THEN (src.calc_participation_score + src.calc_summary_score) / 2.0
                            ELSE src.calc_participation_score
                        END) * src.calc_org_coeff * src.calc_reg_coeff,
                        2
                    )
                WHEN src.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(6,2))
                ELSE CAST(0.00 AS DECIMAL(6,2))
            END,
            updated_at = SYSUTCDATETIME()
    WHEN NOT MATCHED THEN
        INSERT (
            student_id, activity_id, attendance_score, participation_eval_score,
            participation_score, summary_score, average_score,
            org_level_coefficient, early_reg_coefficient, final_point, updated_at
        )
        VALUES (
            src.student_id,
            src.activity_id,
            src.calc_attendance_score,
            src.calc_eval_score,
            src.calc_participation_score,
            src.calc_summary_score,
            CASE
                WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                    CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                        THEN ROUND((src.calc_participation_score + src.calc_summary_score) / 2.0, 2)
                        ELSE src.calc_participation_score
                    END
                WHEN src.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(5,2))
                ELSE CAST(0.00 AS DECIMAL(5,2))
            END,
            src.calc_org_coeff,
            src.calc_reg_coeff,
            CASE
                WHEN src.attendance_status IN ('ATTENDED', 'IN_LATE', 'OUT_EARLY') THEN
                    ROUND(
                        (CASE WHEN src.is_summary_required = 1 AND src.calc_summary_score IS NOT NULL
                            THEN (src.calc_participation_score + src.calc_summary_score) / 2.0
                            ELSE src.calc_participation_score
                        END) * src.calc_org_coeff * src.calc_reg_coeff,
                        2
                    )
                WHEN src.attendance_status = 'ABSENT' THEN CAST(-5.00 AS DECIMAL(6,2))
                ELSE CAST(0.00 AS DECIMAL(6,2))
            END,
            SYSUTCDATETIME()
        );

    COMMIT TRANSACTION;
    PRINT N'Đồng bộ dữ liệu student_activity_result thành công.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    DECLARE @ErrMsg NVARCHAR(4000) = ERROR_MESSAGE();
    DECLARE @ErrSev INT = ERROR_SEVERITY();
    DECLARE @ErrSt INT = ERROR_STATE();
    RAISERROR(@ErrMsg, @ErrSev, @ErrSt);
END CATCH;
GO
