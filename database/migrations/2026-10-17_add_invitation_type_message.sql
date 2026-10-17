-- Invitations get a type (invite to collaborate / invite to end the collaboration) and a message.
-- Run once against the swp391g1 database, after 2026-10-17_add_collaboration_status.sql.

IF COL_LENGTH('dbo.activity_collaboration_invitation', 'invitation_type') IS NULL
BEGIN
    ALTER TABLE dbo.activity_collaboration_invitation
        ADD invitation_type NVARCHAR(20) NOT NULL
            CONSTRAINT df_aci_invitation_type DEFAULT 'COLLABORATION';
END
GO

IF COL_LENGTH('dbo.activity_collaboration_invitation', 'message') IS NULL
BEGIN
    ALTER TABLE dbo.activity_collaboration_invitation ADD message NVARCHAR(500) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'chk_aci_invitation_type')
BEGIN
    ALTER TABLE dbo.activity_collaboration_invitation
        ADD CONSTRAINT chk_aci_invitation_type CHECK (invitation_type IN ('COLLABORATION', 'TERMINATION'));
END
GO
