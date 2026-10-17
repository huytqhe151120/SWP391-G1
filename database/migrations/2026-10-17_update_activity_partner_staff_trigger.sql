-- The old trigger validated partner staff against extracurricular_activity.partner_company_id
-- (single partner). With multiple partners, validate against accepted invitations instead.
-- Run once against the swp391g1 database.

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
            FROM dbo.activity_collaboration_invitation inv
            WHERE inv.activity_id = i.activity_id
              AND inv.partner_company_id = ps.company_id
              AND inv.approval_status = 'APPROVED'
        )
    )
    BEGIN
        THROW 50002, 'invalid assignment: partner staff must belong to a partner company that accepted the activity invitation.', 1;
    END
END
GO
