-- =============================================================================
-- Script: Clone / Tạo mới 100 đơn đăng ký (dbo.activity_registration)
-- Hướng dẫn:
--   1. Đặt @TargetFormId = ID đợt đăng ký muốn thêm 100 đơn (VD: 27).
--      Nếu để NULL hoặc ID không tồn tại, script tự lấy đợt đăng ký mới nhất.
--   2. Script tự động tạo thêm sinh viên (dbo.account + dbo.student) nếu DB
--      chưa đủ 100 sinh viên chưa đăng ký đợt này.
--   3. Tự động điền câu trả lời hồ sơ cá nhân & clone câu trả lời cho các
--      câu hỏi bổ sung của form (dbo.activity_registration_answer).
-- =============================================================================

SET NOCOUNT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @TargetFormId INT = 27; -- Đổi thành ID form cần tạo 100 đơn (hoặc để NULL để tự chọn form mới nhất)
    DECLARE @CloneCount INT = 100;

    IF @TargetFormId IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.activity_registration_form WHERE id = @TargetFormId)
    BEGIN
        SELECT TOP 1 @TargetFormId = id
        FROM dbo.activity_registration_form
        ORDER BY id DESC;
    END

    IF @TargetFormId IS NULL
    BEGIN
        RAISERROR(N'Không tìm thấy đợt đăng ký nào trong dbo.activity_registration_form.', 16, 1);
    END

    DECLARE @ActivityId INT;
    SELECT @ActivityId = activity_id
    FROM dbo.activity_registration_form
    WHERE id = @TargetFormId;

    -- Mở rộng max_participants của hoạt động nếu đang giới hạn nhỏ hơn số lượng sau khi clone
    UPDATE dbo.extracurricular_activity
    SET max_participants = CASE
        WHEN max_participants IS NOT NULL AND max_participants < 200 THEN 200
        ELSE max_participants
    END
    WHERE id = @ActivityId;

    -- Lấy thông tin mẫu từ 1 sinh viên hiện có để clone nếu thiếu sinh viên
    DECLARE @TemplateClassId INT;
    DECLARE @TemplatePassword NVARCHAR(255);

    SELECT TOP 1 @TemplateClassId = main_class_id
    FROM dbo.student
    WHERE main_class_id IS NOT NULL
    ORDER BY id;

    SELECT TOP 1 @TemplatePassword = password
    FROM dbo.account
    WHERE password IS NOT NULL
    ORDER BY id;

    IF @TemplatePassword IS NULL
        SET @TemplatePassword = N'$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5T5k8x2b6vF1p2r3s4t5u6v7w8x9y';

    -- Đếm số sinh viên hiện có chưa đăng ký vào @TargetFormId
    DECLARE @AvailableStudents INT;
    SELECT @AvailableStudents = COUNT(*)
    FROM dbo.student s
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.activity_registration r
        WHERE r.form_id = @TargetFormId
          AND r.student_id = s.id
    );

    -- Nếu chưa đủ 100 sinh viên khả dụng thì tự động tạo thêm sinh viên
    DECLARE @NeedToCreate INT = @CloneCount - ISNULL(@AvailableStudents, 0);
    DECLARE @i INT = 1;
    DECLARE @Seq INT = ISNULL((SELECT MAX(id) FROM dbo.student), 0) + 1;

    WHILE @i <= @NeedToCreate
    BEGIN
        DECLARE @StudentCode NVARCHAR(50) = CONCAT(N'HE', RIGHT(CONCAT('000000', @Seq), 6));
        WHILE EXISTS (SELECT 1 FROM dbo.student WHERE code = @StudentCode)
           OR EXISTS (SELECT 1 FROM dbo.account WHERE username = LOWER(@StudentCode))
        BEGIN
            SET @Seq += 1;
            SET @StudentCode = CONCAT(N'HE', RIGHT(CONCAT('000000', @Seq), 6));
        END

        DECLARE @NewAccountId INT;
        INSERT INTO dbo.account (username, password, type, role, status)
        VALUES (LOWER(@StudentCode), @TemplatePassword, N'STUDENT', N'STUDENT', N'ACTIVE');
        SET @NewAccountId = SCOPE_IDENTITY();

        INSERT INTO dbo.student (main_class_id, account_id, code, name, dob, gender, email, phone_number, status)
        VALUES (
            @TemplateClassId,
            @NewAccountId,
            @StudentCode,
            CONCAT(N'Sinh viên Clone ', @Seq),
            DATEADD(DAY, -(@Seq % 1000), CAST('2004-06-15' AS DATE)),
            @Seq % 2,
            CONCAT(LOWER(@StudentCode), N'@fpt.edu.vn'),
            CONCAT(N'09', RIGHT(CONCAT('00000000', @Seq), 8)),
            N'ACTIVE'
        );

        SET @Seq += 1;
        SET @i += 1;
    END

    -- Chọn đúng 100 sinh viên chưa có đơn trong @TargetFormId
    DECLARE @SelectedStudents TABLE (
        row_num INT IDENTITY(1,1) PRIMARY KEY,
        student_id INT,
        student_code NVARCHAR(50),
        student_name NVARCHAR(255),
        class_code NVARCHAR(50),
        email NVARCHAR(255),
        phone_number NVARCHAR(30),
        dob_str NVARCHAR(20)
    );

    INSERT INTO @SelectedStudents (student_id, student_code, student_name, class_code, email, phone_number, dob_str)
    SELECT TOP (@CloneCount)
        s.id,
        s.code,
        s.name,
        ISNULL(mc.code, N'SE1801'),
        ISNULL(s.email, CONCAT(LOWER(s.code), N'@fpt.edu.vn')),
        ISNULL(s.phone_number, N'0912345678'),
        CONVERT(NVARCHAR(10), ISNULL(s.dob, CAST('2004-01-01' AS DATE)), 23)
    FROM dbo.student s
    LEFT JOIN dbo.main_class mc ON mc.id = s.main_class_id
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.activity_registration r
        WHERE r.form_id = @TargetFormId
          AND r.student_id = s.id
    )
    ORDER BY s.id;

    -- Lấy 1 đơn mẫu trong cùng đợt (nếu có) để clone câu trả lời cho các trường bổ sung
    DECLARE @TemplateRegistrationId INT;
    SELECT TOP 1 @TemplateRegistrationId = id
    FROM dbo.activity_registration
    WHERE form_id = @TargetFormId
    ORDER BY id DESC;

    DECLARE @BaseTime DATETIME2 = DATEADD(MINUTE, -@CloneCount, SYSDATETIME());
    DECLARE @Row INT = 1;
    DECLARE @TotalRows INT = (SELECT COUNT(*) FROM @SelectedStudents);

    WHILE @Row <= @TotalRows
    BEGIN
        DECLARE @StuId INT,
                @StuCode NVARCHAR(50),
                @StuName NVARCHAR(255),
                @ClsCode NVARCHAR(50),
                @Email NVARCHAR(255),
                @Phone NVARCHAR(30),
                @Dob NVARCHAR(20);

        SELECT
            @StuId = student_id,
            @StuCode = student_code,
            @StuName = student_name,
            @ClsCode = class_code,
            @Email = email,
            @Phone = phone_number,
            @Dob = dob_str
        FROM @SelectedStudents
        WHERE row_num = @Row;

        DECLARE @SubmittedAt DATETIME2 = DATEADD(SECOND, @Row * 10, @BaseTime);
        DECLARE @NewRegId INT;

        INSERT INTO dbo.activity_registration (form_id, student_id, note, status, submitted_at, pending_since)
        VALUES (
            @TargetFormId,
            @StuId,
            CONCAT(N'Đơn đăng ký clone #', @Row, N' - ', @StuCode),
            N'SUBMITTED',
            @SubmittedAt,
            @SubmittedAt
        );
        SET @NewRegId = SCOPE_IDENTITY();

        -- 1. Thêm các câu trả lời thông tin hồ sơ sinh viên chuẩn hệ thống
        INSERT INTO dbo.activity_registration_answer (registration_id, field_key, field_label, answer_value)
        VALUES
            (@NewRegId, N'application_student_id',            N'ID',                                      CAST(@StuId AS NVARCHAR(50))),
            (@NewRegId, N'application_student_code',          N'Mã sinh viên',                            @StuCode),
            (@NewRegId, N'application_student_name',          N'Họ và tên',                               @StuName),
            (@NewRegId, N'application_class_code',            N'Lớp',                                     @ClsCode),
            (@NewRegId, N'application_email',                 N'Email',                                   @Email),
            (@NewRegId, N'application_phone_number',          N'Số điện thoại',                           @Phone),
            (@NewRegId, N'application_dob',                   N'Ngày sinh',                               @Dob),
            (@NewRegId, N'application_attendance_commitment', N'Tôi cam kết tham dự đầy đủ và đúng giờ.', N'true');

        -- 2. Clone hoặc tự sinh câu trả lời cho các câu hỏi bổ sung của đợt đăng ký
        INSERT INTO dbo.activity_registration_answer (registration_id, field_key, field_label, answer_value)
        SELECT
            @NewRegId,
            ff.field_key,
            ff.label,
            COALESCE(
                template_ans.answer_value,
                CASE ff.field_type
                    WHEN N'CHECKBOX' THEN N'true'
                    WHEN N'NUMBER'   THEN N'1'
                    WHEN N'DATE'     THEN CONVERT(NVARCHAR(10), CAST(SYSDATETIME() AS DATE), 23)
                    WHEN N'EMAIL'    THEN @Email
                    WHEN N'PHONE'    THEN @Phone
                    WHEN N'SELECT'   THEN LTRIM(RTRIM(
                        CASE
                            WHEN CHARINDEX(CHAR(10), ISNULL(ff.options_text, N'')) > 0
                                THEN REPLACE(LEFT(ff.options_text, CHARINDEX(CHAR(10), ff.options_text) - 1), CHAR(13), N'')
                            ELSE ISNULL(ff.options_text, N'Lựa chọn 1')
                        END
                    ))
                    WHEN N'MULTISELECT' THEN LTRIM(RTRIM(
                        CASE
                            WHEN CHARINDEX(CHAR(10), ISNULL(ff.options_text, N'')) > 0
                                THEN REPLACE(LEFT(ff.options_text, CHARINDEX(CHAR(10), ff.options_text) - 1), CHAR(13), N'')
                            ELSE ISNULL(ff.options_text, N'Lựa chọn 1')
                        END
                    ))
                    ELSE CONCAT(N'Câu trả lời mẫu của ', @StuCode, N' cho "', ff.label, N'"')
                END
            )
        FROM dbo.activity_registration_form_field ff
        LEFT JOIN dbo.activity_registration_answer template_ans
            ON template_ans.registration_id = @TemplateRegistrationId
           AND template_ans.field_key = ff.field_key
        WHERE ff.form_id = @TargetFormId;

        SET @Row += 1;
    END

    COMMIT TRANSACTION;
    PRINT CONCAT(N'Đã tạo thành công ', @TotalRows, N' đơn đăng ký cho form_id = ', @TargetFormId, N'.');
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
    RAISERROR(@ErrorMessage, 16, 1);
END CATCH;
GO
