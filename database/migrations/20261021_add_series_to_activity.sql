SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Link extracurricular_activity rows to activity_series (each activity row IS an occurrence)
-- and add the filtered unique indexes that prevent duplicate occurrences.
-- Run once against the swp391g1 database (after create_activity_series).

-- 1) New columns (idempotent)
IF COL_LENGTH('dbo.extracurricular_activity', 'series_id') IS NULL
    ALTER TABLE dbo.extracurricular_activity ADD series_id INT NULL;
GO
IF COL_LENGTH('dbo.extracurricular_activity', 'occurrence_no') IS NULL
    ALTER TABLE dbo.extracurricular_activity ADD occurrence_no INT NULL;
GO
IF COL_LENGTH('dbo.extracurricular_activity', 'occurrence_start_date') IS NULL
    ALTER TABLE dbo.extracurricular_activity ADD occurrence_start_date DATE NULL;
GO
IF COL_LENGTH('dbo.extracurricular_activity', 'approved_via_series') IS NULL
    ALTER TABLE dbo.extracurricular_activity ADD approved_via_series BIT NOT NULL
        CONSTRAINT df_extracurricular_activity_approved_via_series DEFAULT (0);
GO

-- 2) Check constraints
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_activity_occurrence_pair' AND parent_object_id = OBJECT_ID('dbo.extracurricular_activity'))
BEGIN
    ALTER TABLE dbo.extracurricular_activity ADD CONSTRAINT CK_activity_occurrence_pair CHECK (
          (series_id IS NULL AND occurrence_no IS NULL AND occurrence_start_date IS NULL)
       OR (series_id IS NOT NULL AND occurrence_no IS NOT NULL AND occurrence_start_date IS NOT NULL));
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_activity_occurrence_no' AND parent_object_id = OBJECT_ID('dbo.extracurricular_activity'))
BEGIN
    ALTER TABLE dbo.extracurricular_activity ADD CONSTRAINT CK_activity_occurrence_no
        CHECK (occurrence_no IS NULL OR occurrence_no >= 1);
END
GO

-- 3) Foreign key + indexes (filtered unique indexes)
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_activity_series' AND parent_object_id = OBJECT_ID('dbo.extracurricular_activity'))
    ALTER TABLE dbo.extracurricular_activity ADD CONSTRAINT FK_activity_series
        FOREIGN KEY (series_id) REFERENCES dbo.activity_series(id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_activity_series_id' AND object_id = OBJECT_ID('dbo.extracurricular_activity'))
    CREATE NONCLUSTERED INDEX IX_activity_series_id ON dbo.extracurricular_activity (series_id);
GO
SET QUOTED_IDENTIFIER ON;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_activity_series_occurrence' AND object_id = OBJECT_ID('dbo.extracurricular_activity'))
    CREATE UNIQUE NONCLUSTERED INDEX UX_activity_series_occurrence
        ON dbo.extracurricular_activity (series_id, occurrence_no)
        WHERE series_id IS NOT NULL;
GO
SET QUOTED_IDENTIFIER ON;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_activity_series_occurrence_date' AND object_id = OBJECT_ID('dbo.extracurricular_activity'))
    CREATE UNIQUE NONCLUSTERED INDEX UX_activity_series_occurrence_date
        ON dbo.extracurricular_activity (series_id, occurrence_start_date)
        WHERE series_id IS NOT NULL;
GO
