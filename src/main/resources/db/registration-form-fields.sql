IF OBJECT_ID(N'dbo.activity_registration_form_field', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.activity_registration_form_field (
        id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        form_id INT NOT NULL,
        field_key VARCHAR(80) NOT NULL,
        label NVARCHAR(400) NOT NULL,
        field_type VARCHAR(20) NOT NULL,
        is_required BIT NOT NULL CONSTRAINT DF_registration_form_field_required DEFAULT 0,
        options_text NVARCHAR(4000) NULL,
        save_to_profile BIT NOT NULL CONSTRAINT DF_registration_form_field_profile DEFAULT 0,
        sort_order INT NOT NULL CONSTRAINT DF_registration_form_field_sort DEFAULT 0,
        CONSTRAINT FK_registration_form_field_form FOREIGN KEY (form_id)
            REFERENCES dbo.activity_registration_form(id) ON DELETE CASCADE,
        CONSTRAINT UQ_registration_form_field_key UNIQUE (form_id, field_key)
    );
END;
GO

IF OBJECT_ID(N'dbo.activity_registration_answer', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.activity_registration_answer (
        id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        registration_id INT NOT NULL,
        field_key VARCHAR(80) NOT NULL,
        field_label NVARCHAR(400) NOT NULL,
        answer_value NVARCHAR(8000) NULL,
        CONSTRAINT FK_registration_answer_registration FOREIGN KEY (registration_id)
            REFERENCES dbo.activity_registration(id),
        CONSTRAINT UQ_registration_answer_registration_field UNIQUE (registration_id, field_key)
    );
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'IX_registration_answer_registration'
      AND object_id = OBJECT_ID(N'dbo.activity_registration_answer')
)
BEGIN
    CREATE INDEX IX_registration_answer_registration
        ON dbo.activity_registration_answer(registration_id)
        INCLUDE (field_key, field_label, answer_value);
END;
GO
