-- activity_partner_company gets a collaboration_status (INACTIVE / ACTIVE).
--   invite             -> row created (or reused) with INACTIVE
--   accept             -> ACTIVE
--   reject / revoke    -> INACTIVE
--   terminate          -> INACTIVE (row kept, partner staff of the activity removed)
-- Run once against the swp391g1 database, after 2026-10-17_create_activity_partner_company.sql.

IF COL_LENGTH('dbo.activity_partner_company', 'collaboration_status') IS NULL
BEGIN
    ALTER TABLE dbo.activity_partner_company
        ADD collaboration_status NVARCHAR(10) NOT NULL
            CONSTRAINT df_apc_collaboration_status DEFAULT 'INACTIVE';

    -- Rows that already exist were created from accepted invitations.
    EXEC('UPDATE dbo.activity_partner_company SET collaboration_status = ''ACTIVE''');
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'chk_apc_collaboration_status')
BEGIN
    ALTER TABLE dbo.activity_partner_company
        ADD CONSTRAINT chk_apc_collaboration_status CHECK (collaboration_status IN ('INACTIVE', 'ACTIVE'));
END
GO

-- Pending invitations created before this change get their INACTIVE partner row.
INSERT INTO dbo.activity_partner_company (activity_id, partner_company_id, partner_staff_id, invitation_id, created_at, collaboration_status)
SELECT i.activity_id, i.partner_company_id, i.partner_staff_id, i.id, i.created_at, 'INACTIVE'
FROM dbo.activity_collaboration_invitation i
WHERE i.approval_status = 'PENDING'
  AND i.partner_staff_id IS NOT NULL
  AND i.id = (SELECT MAX(x.id) FROM dbo.activity_collaboration_invitation x
              WHERE x.activity_id = i.activity_id AND x.partner_company_id = i.partner_company_id
                AND x.approval_status = 'PENDING' AND x.partner_staff_id IS NOT NULL)
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
              AND apc.collaboration_status = 'ACTIVE'
        )
    )
    BEGIN
        THROW 50002, 'invalid assignment: partner staff must belong to a partner company actively collaborating in the activity.', 1;
    END
END
GO
