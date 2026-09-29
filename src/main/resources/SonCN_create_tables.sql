-- ============================================================
-- SonCN Feature Script
-- Creates Answer and Message tables for Q&A and Inbox features
-- Run this script on your SQL Server database ONCE.
-- ============================================================

-- Table: Answer
-- Used for: Answer Question feature (Row 17)
-- Staff submits answers to student questions.
-- Answers can be DRAFT (private) or PUBLISHED (public Q&A).
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

-- Table: Message
-- Used for: Student Inbox (Row 18) + Organizer Inbox Dashboard (Row 19)
-- Private 1-on-1 messages between a student and a staff member.
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

-- ============================================================
-- Sample data for testing
-- NOTE: Update question_id / student_id / staff_id to valid IDs
--       in YOUR database before running these inserts.
-- ============================================================

-- Sample Answer (DRAFT)
INSERT INTO [dbo].[Answer] (question_id, staff_id, content, approval_status)
VALUES (
    1,
    1,
    N'Day la cau tra loi mau. Ban co the tham gia hoat dong CLB bang cach dien form dang ky.',
    'DRAFT'
);

-- Sample Answer (PUBLISHED)
INSERT INTO [dbo].[Answer] (question_id, staff_id, content, approval_status, published_at)
VALUES (
    2,
    1,
    N'Diem ren luyen se duoc cong vao cuoi ky sau khi ban to chuc duyet. Thuong mat 2-3 tuan.',
    'PUBLISHED',
    GETDATE()
);

-- Sample Message from Student to Staff
INSERT INTO [dbo].[Message] (student_id, staff_id, sender_type, content)
VALUES (
    1,
    1,
    'STUDENT',
    N'Thay/co oi, em muon hoi ve quy trinh duyet diem ren luyen a?'
);

-- Sample Reply from Staff to Student
INSERT INTO [dbo].[Message] (student_id, staff_id, sender_type, content, is_read)
VALUES (
    1,
    1,
    'STAFF',
    N'Chao em, thay se ho tro em nhe. Diem ren luyen cua em da duoc ghi nhan roi.',
    1
);
