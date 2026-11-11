-- =====================================================================
-- Post-Activity Assessment schema (SWP391-G1 / EAMS)
-- Migration: 20261101_create_post_activity_assessment.sql
-- =====================================================================
-- New tables for the post-activity assessment feature.
-- All new tables live in the assessment domain and reference existing
-- tables (extracurricular_activity, student) via foreign keys.
-- No existing table is modified.
-- =====================================================================

IF OBJECT_ID('dbo.assessment', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[assessment](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [activity_id] [int] NOT NULL,
        [title] [nvarchar](255) NOT NULL,
        [description] [nvarchar](max) NULL,
        [open_time] [datetime2](7) NULL,
        [close_time] [datetime2](7) NULL,
        [is_published] [bit] NOT NULL CONSTRAINT [DF_assessment_is_published] DEFAULT ((0)),
        [created_at] [datetime2](7) NOT NULL CONSTRAINT [DF_assessment_created_at] DEFAULT (sysdatetime()),
        [updated_at] [datetime2](7) NULL,
        CONSTRAINT [PK_assessment] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [UQ_assessment_activity] UNIQUE NONCLUSTERED ([activity_id] ASC),
        CONSTRAINT [FK_assessment_activity] FOREIGN KEY ([activity_id])
            REFERENCES [dbo].[extracurricular_activity]([id])
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
END
GO

IF OBJECT_ID('dbo.assessment_question', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[assessment_question](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [assessment_id] [int] NOT NULL,
        [question_type] [varchar](20) NOT NULL CONSTRAINT [DF_assessment_question_type] DEFAULT ('MCQ'),
        [content] [nvarchar](max) NOT NULL,
        [max_score] [decimal](5, 2) NOT NULL CONSTRAINT [DF_assessment_question_max_score] DEFAULT ((0)),
        [display_order] [int] NOT NULL CONSTRAINT [DF_assessment_question_display_order] DEFAULT ((0)),
        CONSTRAINT [PK_assessment_question] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_assessment_question_assessment] FOREIGN KEY ([assessment_id])
            REFERENCES [dbo].[assessment]([id]) ON DELETE CASCADE,
        CONSTRAINT [CK_assessment_question_type] CHECK ([question_type] IN ('MCQ', 'ESSAY')),
        CONSTRAINT [CK_assessment_question_max_score_positive] CHECK ([max_score] > 0)
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
END
GO

IF OBJECT_ID('dbo.assessment_option', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[assessment_option](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [question_id] [int] NOT NULL,
        [option_text] [nvarchar](1000) NOT NULL,
        [is_correct] [bit] NOT NULL CONSTRAINT [DF_assessment_option_is_correct] DEFAULT ((0)),
        [display_order] [int] NOT NULL CONSTRAINT [DF_assessment_option_display_order] DEFAULT ((0)),
        CONSTRAINT [PK_assessment_option] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [FK_assessment_option_question] FOREIGN KEY ([question_id])
            REFERENCES [dbo].[assessment_question]([id]) ON DELETE CASCADE
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
END
GO

IF OBJECT_ID('dbo.assessment_attempt', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[assessment_attempt](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [assessment_id] [int] NOT NULL,
        [student_id] [int] NOT NULL,
        [status] [varchar](20) NOT NULL CONSTRAINT [DF_assessment_attempt_status] DEFAULT ('IN_PROGRESS'),
        [submitted_at] [datetime2](7) NULL,
        [final_score] [decimal](5, 2) NULL,
        [created_at] [datetime2](7) NOT NULL CONSTRAINT [DF_assessment_attempt_created_at] DEFAULT (sysdatetime()),
        [updated_at] [datetime2](7) NULL,
        CONSTRAINT [PK_assessment_attempt] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [UQ_assessment_attempt_student] UNIQUE NONCLUSTERED ([assessment_id] ASC, [student_id] ASC),
        CONSTRAINT [FK_assessment_attempt_assessment] FOREIGN KEY ([assessment_id])
            REFERENCES [dbo].[assessment]([id]) ON DELETE CASCADE,
        CONSTRAINT [FK_assessment_attempt_student] FOREIGN KEY ([student_id])
            REFERENCES [dbo].[student]([id]),
        CONSTRAINT [CK_assessment_attempt_status] CHECK ([status] IN ('IN_PROGRESS', 'SUBMITTED', 'COMPLETED')),
        CONSTRAINT [CK_assessment_attempt_final_score_nonnegative] CHECK ([final_score] IS NULL OR [final_score] >= 0)
    ) ON [PRIMARY]
END
GO

IF OBJECT_ID('dbo.assessment_answer', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[assessment_answer](
        [id] [int] IDENTITY(1,1) NOT NULL,
        [attempt_id] [int] NOT NULL,
        [question_id] [int] NOT NULL,
        [selected_option_id] [int] NULL,
        [answer_text] [nvarchar](max) NULL,
        [awarded_score] [decimal](5, 2) NULL,
        [feedback] [nvarchar](max) NULL,
        [grading_status] [varchar](20) NOT NULL CONSTRAINT [DF_assessment_answer_grading_status] DEFAULT ('PENDING_GRADING'),
        [created_at] [datetime2](7) NOT NULL CONSTRAINT [DF_assessment_answer_created_at] DEFAULT (sysdatetime()),
        [updated_at] [datetime2](7) NULL,
        CONSTRAINT [PK_assessment_answer] PRIMARY KEY CLUSTERED ([id] ASC),
        CONSTRAINT [UQ_assessment_answer_attempt_question] UNIQUE NONCLUSTERED ([attempt_id] ASC, [question_id] ASC),
        CONSTRAINT [FK_assessment_answer_attempt] FOREIGN KEY ([attempt_id])
            REFERENCES [dbo].[assessment_attempt]([id]) ON DELETE CASCADE,
        CONSTRAINT [FK_assessment_answer_question] FOREIGN KEY ([question_id])
            REFERENCES [dbo].[assessment_question]([id]) ON DELETE CASCADE,
        CONSTRAINT [FK_assessment_answer_option] FOREIGN KEY ([selected_option_id])
            REFERENCES [dbo].[assessment_option]([id]),
        CONSTRAINT [CK_assessment_answer_grading_status] CHECK ([grading_status] IN ('PENDING_GRADING', 'GRADED')),
        CONSTRAINT [CK_assessment_answer_awarded_score_range] CHECK ([awarded_score] IS NULL OR ([awarded_score] >= 0 AND [awarded_score] <= 1000))
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
END
GO
