SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Create activity_series: shared metadata + recurrence configuration for
-- recurring extracurricular activities (Series 1:N Activity).
-- Run once against the swp391g1 database.
IF OBJECT_ID('dbo.activity_series', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.activity_series (
        id                          INT IDENTITY(1,1) PRIMARY KEY,
        series_code                 VARCHAR(50)   NOT NULL,
        name                        NVARCHAR(255) NOT NULL,
        activity_type_id            INT           NOT NULL,
        responsible_department_id   INT           NOT NULL,
        responsible_staff_id        INT           NOT NULL,
        description                 NVARCHAR(MAX) NULL,
        registration_form_required  BIT           NOT NULL CONSTRAINT df_activity_series_registration_form_required DEFAULT (0),
        recurrence_mode             VARCHAR(20)   NOT NULL,
        recurrence_interval         INT           NULL,
        series_start_date           DATE          NOT NULL,
        series_end_date             DATE          NULL,
        max_occurrences             INT           NULL,
        occurrence_duration_minutes INT           NOT NULL,
        time_of_day                 TIME(0)       NOT NULL,
        semester_id                 INT           NULL,
        approval_status             VARCHAR(50)   NOT NULL CONSTRAINT df_activity_series_approval_status DEFAULT ('DRAFT'),
        created_at                  DATETIME2(7)  NOT NULL CONSTRAINT df_activity_series_created_at DEFAULT SYSDATETIME(),
        updated_at                  DATETIME2(7)  NULL,
        CONSTRAINT uq_activity_series_code UNIQUE (series_code),
        CONSTRAINT ck_series_mode CHECK (recurrence_mode IN ('MONTHLY','YEARLY')),
        CONSTRAINT ck_series_interval CHECK (
            (recurrence_mode IN ('MONTHLY','YEARLY') AND recurrence_interval IS NOT NULL AND recurrence_interval >= 1)
        ),
        CONSTRAINT ck_series_bounds CHECK (series_end_date IS NULL OR series_end_date >= series_start_date),
        CONSTRAINT ck_series_max_occ CHECK (max_occurrences IS NULL OR max_occurrences >= 1),
        CONSTRAINT ck_series_duration CHECK (occurrence_duration_minutes > 0),
        CONSTRAINT ck_series_approval_status CHECK (approval_status IN ('DRAFT','PENDING','APPROVED','REJECTED')),
        CONSTRAINT fk_activity_series_activity_type FOREIGN KEY (activity_type_id) REFERENCES dbo.activity_type(id),
        CONSTRAINT fk_activity_series_responsible FOREIGN KEY (responsible_staff_id, responsible_department_id)
            REFERENCES dbo.staff_assignment (staff_id, department_id),
        CONSTRAINT fk_activity_series_semester FOREIGN KEY (semester_id) REFERENCES dbo.semester(id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_activity_series_approval_status' AND object_id = OBJECT_ID('dbo.activity_series'))
    CREATE NONCLUSTERED INDEX IX_activity_series_approval_status ON dbo.activity_series (approval_status);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_activity_series_semester_id' AND object_id = OBJECT_ID('dbo.activity_series'))
    CREATE NONCLUSTERED INDEX IX_activity_series_semester_id ON dbo.activity_series (semester_id);
GO
