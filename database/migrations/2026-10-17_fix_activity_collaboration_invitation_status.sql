-- Fix the approval_status check constraint of activity_collaboration_invitation.
-- The original constraint allowed the misspelled value 'PPENDING' instead of 'PENDING'.
-- Run once against the swp391g1 database before using the partner invitation feature.

IF EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'chk_activity_collab_inv_status'
      AND parent_object_id = OBJECT_ID('activity_collaboration_invitation')
)
BEGIN
    ALTER TABLE activity_collaboration_invitation DROP CONSTRAINT chk_activity_collab_inv_status;
END
GO

UPDATE activity_collaboration_invitation
SET approval_status = 'PENDING'
WHERE approval_status = 'PPENDING';
GO

ALTER TABLE activity_collaboration_invitation
    ADD CONSTRAINT chk_activity_collab_inv_status
    CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));
GO
