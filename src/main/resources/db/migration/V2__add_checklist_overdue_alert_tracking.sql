IF OBJECT_ID(N'dbo.checklist_task_overdue_alert', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.checklist_task_overdue_alert
    (
        task_id INT NOT NULL,
        alert_date DATE NOT NULL,
        sent_at DATETIME2(0) NOT NULL
            CONSTRAINT DF_checklist_task_overdue_alert_sent_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_checklist_task_overdue_alert
            PRIMARY KEY (task_id, alert_date),
        CONSTRAINT FK_checklist_task_overdue_alert_task
            FOREIGN KEY (task_id) REFERENCES dbo.checklist_task(id) ON DELETE CASCADE
    );
END;
