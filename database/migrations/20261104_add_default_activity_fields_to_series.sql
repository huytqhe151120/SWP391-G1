SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Add default activity fields to activity_series table
-- These fields store default values that will be copied to each occurrence
-- when generating activities from a series.
-- Run once against the swp391g1 database.

-- 1) nature column (ActivityNature enum: REQUIRE_REGISTRATION, VOLUNTARY, MANDATORY)
IF COL_LENGTH('dbo.activity_series', 'nature') IS NULL
    ALTER TABLE dbo.activity_series ADD nature VARCHAR(50) NULL;
GO

-- 2) max_participants column
IF COL_LENGTH('dbo.activity_series', 'max_participants') IS NULL
    ALTER TABLE dbo.activity_series ADD max_participants INT NULL;
GO

-- 3) summary_required column
IF COL_LENGTH('dbo.activity_series', 'summary_required') IS NULL
    ALTER TABLE dbo.activity_series ADD summary_required BIT NOT NULL CONSTRAINT df_activity_series_summary_required DEFAULT (0);
GO