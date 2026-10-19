-- When an activity becomes ONGOING, its PENDING collaboration invitations are CANCELLED automatically.
-- The application applies the same rule whenever invitations are read, so changes made outside the trigger are covered too.
-- Run once against the swp391g1 database.

CREATE OR ALTER TRIGGER dbo.tr_activity_ongoing_cancel_invitations
ON dbo.extracurricular_activity
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE inv SET approval_status = 'CANCELLED'
    FROM dbo.activity_collaboration_invitation inv
    JOIN inserted i ON i.id = inv.activity_id
    WHERE i.activity_status = 'ONGOING'
      AND inv.approval_status = 'PENDING';
END
GO