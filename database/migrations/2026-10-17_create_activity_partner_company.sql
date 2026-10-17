-- Option 2: partners that take part in an activity live in activity_partner_company.
-- activity_collaboration_invitation keeps the invitation history.
-- Run once against the swp391g1 database (after the invitation status fix script).

IF OBJECT_ID('dbo.activity_partner_company', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.activity_partner_company (
        id INT IDENTITY(1,1) PRIMARY KEY,
        activity_id INT NOT NULL,
        partner_company_id INT NOT NULL,
        partner_staff_id INT NOT NULL,
        invitation_id INT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_activity_partner_company_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT fk_apc_activity FOREIGN KEY (activity_id) REFERENCES dbo.extracurricular_activity(id),
        CONSTRAINT fk_apc_company FOREIGN KEY (partner_company_id) REFERENCES dbo.partner_company(id),
        CONSTRAINT fk_apc_staff FOREIGN KEY (partner_staff_id) REFERENCES dbo.partner_staff(id),
        CONSTRAINT fk_apc_invitation FOREIGN KEY (invitation_id) REFERENCES dbo.activity_collaboration_invitation(id),
        CONSTRAINT uq_apc_activity_company UNIQUE (activity_id, partner_company_id)
    );
END
GO

-- Move the already approved invitations (latest one per activity + company).
INSERT INTO dbo.activity_partner_company (activity_id, partner_company_id, partner_staff_id, invitation_id, created_at)
SELECT i.activity_id, i.partner_company_id, i.partner_staff_id, i.id, i.created_at
FROM dbo.activity_collaboration_invitation i
WHERE i.approval_status = 'APPROVED'
  AND i.partner_staff_id IS NOT NULL
  AND i.id = (SELECT MAX(x.id) FROM dbo.activity_collaboration_invitation x
              WHERE x.activity_id = i.activity_id AND x.partner_company_id = i.partner_company_id
                AND x.approval_status = 'APPROVED' AND x.partner_staff_id IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM dbo.activity_partner_company c
                  WHERE c.activity_id = i.activity_id AND c.partner_company_id = i.partner_company_id);
GO

CREATE OR ALTER TRIGGER dbo.tr_activity_partner_staff_check
ON dbo.activity_partner_staff
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN dbo.partner_staff ps ON ps.id = i.partner_staff_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM dbo.activity_partner_company apc
            WHERE apc.activity_id = i.activity_id
              AND apc.partner_company_id = ps.company_id
        )
    )
    BEGIN
        THROW 50002, 'invalid assignment: partner staff must belong to a partner company participating in the activity.', 1;
    END
END
GO
