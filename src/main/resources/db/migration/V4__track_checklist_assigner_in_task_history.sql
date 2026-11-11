IF COL_LENGTH(N'dbo.checklist_task_history', N'changed_by_account_id') IS NULL
BEGIN
    ALTER TABLE dbo.checklist_task_history
        ADD changed_by_account_id INT NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = N'FK_checklist_task_history_changed_by_account'
      AND parent_object_id = OBJECT_ID(N'dbo.checklist_task_history')
)
BEGIN
    ALTER TABLE dbo.checklist_task_history
        ADD CONSTRAINT FK_checklist_task_history_changed_by_account
        FOREIGN KEY (changed_by_account_id)
        REFERENCES dbo.account(id)
        ON DELETE SET NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = N'IX_checklist_task_history_task_field_changed'
      AND object_id = OBJECT_ID(N'dbo.checklist_task_history')
)
BEGIN
    CREATE INDEX IX_checklist_task_history_task_field_changed
        ON dbo.checklist_task_history (task_id, field_name, changed_at DESC, id DESC)
        INCLUDE (new_value);
END;
