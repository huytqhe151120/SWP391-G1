-- ============================================================
-- SonCN Feature Script
-- Creates Answer and Message tables for Q&A and Inbox features
-- Run this script on your SQL Server database ONCE.
-- ============================================================

-- Table: Answer
-- Used for: Answer Question feature (Row 17)
-- Staff submits answers to student questions.
-- Answers can be DRAFT (private) or PUBLISHED (public Q&A).
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Answer' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
CREATE TABLE [dbo].[Answer] (
    [id]              INT            IDENTITY(1,1) NOT NULL,
    [question_id]     INT            NOT NULL,
    [staff_id]        INT            NOT NULL,
    [content]         NVARCHAR(MAX)  NOT NULL,
    [approval_status] NVARCHAR(20)   NOT NULL DEFAULT ('DRAFT'),
    [created_at]      DATETIME2      NOT NULL DEFAULT (GETDATE()),
    [published_at]    DATETIME2      NULL,

    CONSTRAINT [PK__Answer__id] PRIMARY KEY ([id]),
    CONSTRAINT [FK__Answer__question] FOREIGN KEY ([question_id])
        REFERENCES [dbo].[Question] ([id]) ON DELETE CASCADE,
    CONSTRAINT [FK__Answer__staff] FOREIGN KEY ([staff_id])
        REFERENCES [dbo].[staff] ([id]),
    CONSTRAINT [CK__Answer__status]
        CHECK ([approval_status] IN ('DRAFT', 'PUBLISHED'))
);
PRINT 'Da tao bang Answer.';
END
ELSE PRINT 'Bang Answer da ton tai, bo qua.';

-- Table: Message
-- Used for: Student Inbox (Row 18) + Organizer Inbox Dashboard (Row 19)
-- Private 1-on-1 messages between a student and a staff member.
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'Message' AND schema_id = SCHEMA_ID('dbo'))
BEGIN
CREATE TABLE [dbo].[Message] (
    [id]          INT            IDENTITY(1,1) NOT NULL,
    [student_id]  INT            NOT NULL,
    [staff_id]    INT            NOT NULL,
    [sender_type] NVARCHAR(10)   NOT NULL,
    [content]     NVARCHAR(MAX)  NOT NULL,
    [sent_at]     DATETIME2      NOT NULL DEFAULT (GETDATE()),
    [is_read]     BIT            NOT NULL DEFAULT (0),

    CONSTRAINT [PK__Message__id] PRIMARY KEY ([id]),
    CONSTRAINT [FK__Message__student] FOREIGN KEY ([student_id])
        REFERENCES [dbo].[student] ([id]),
    CONSTRAINT [FK__Message__staff] FOREIGN KEY ([staff_id])
        REFERENCES [dbo].[staff] ([id]),
    CONSTRAINT [CK__Message__sender]
        CHECK ([sender_type] IN ('STUDENT', 'STAFF'))
);
PRINT 'Da tao bang Message.';
END
ELSE PRINT 'Bang Message da ton tai, bo qua.';

-- ============================================================
-- Sample data for testing
-- Tu dong lay ID hop le tu DB, khong can sua tay.
-- ============================================================

-- Lay ID dau tien hop le tu cac bang
DECLARE @questionId1 INT = (SELECT TOP 1 id FROM [dbo].[Question] ORDER BY id);
DECLARE @questionId2 INT = (SELECT TOP 1 id FROM [dbo].[Question] WHERE id > @questionId1 ORDER BY id);
DECLARE @staffId    INT = (SELECT TOP 1 id FROM [dbo].[staff]    ORDER BY id);
DECLARE @studentId  INT = (SELECT TOP 1 id FROM [dbo].[student]  ORDER BY id);

-- Kiem tra neu cac bang chua co du lieu thi bao loi ro rang
IF @questionId1 IS NULL
    THROW 50001, 'Bang Question chua co du lieu. Hay chay script tao du lieu Question truoc.', 1;
IF @staffId IS NULL
    THROW 50002, 'Bang staff chua co du lieu. Hay chay script tao du lieu Staff truoc.', 1;
IF @studentId IS NULL
    THROW 50003, 'Bang student chua co du lieu. Hay chay script tao du lieu Student truoc.', 1;

-- Neu chi co 1 question thi dung lai question do cho ca 2 answer
IF @questionId2 IS NULL SET @questionId2 = @questionId1;

-- Sample Answer (DRAFT)
INSERT INTO [dbo].[Answer] (question_id, staff_id, content, approval_status)
VALUES (
    @questionId1,
    @staffId,
    N'Day la cau tra loi mau. Ban co the tham gia hoat dong CLB bang cach dien form dang ky.',
    'DRAFT'
);

-- Sample Answer (PUBLISHED)
INSERT INTO [dbo].[Answer] (question_id, staff_id, content, approval_status, published_at)
VALUES (
    @questionId2,
    @staffId,
    N'Diem ren luyen se duoc cong vao cuoi ky sau khi ban to chuc duyet. Thuong mat 2-3 tuan.',
    'PUBLISHED',
    GETDATE()
);

-- Sample Message: Student gui Staff
INSERT INTO [dbo].[Message] (student_id, staff_id, sender_type, content)
VALUES (
    @studentId,
    @staffId,
    'STUDENT',
    N'Thay/co oi, em muon hoi ve quy trinh duyet diem ren luyen a?'
);

-- Sample Message: Staff tra loi Student
INSERT INTO [dbo].[Message] (student_id, staff_id, sender_type, content, is_read)
VALUES (
    @studentId,
    @staffId,
    'STAFF',
    N'Chao em, thay se ho tro em nhe. Diem ren luyen cua em da duoc ghi nhan roi.',
    1
);

PRINT 'Setup xong! Tables Answer va Message da duoc tao va co sample data.';
