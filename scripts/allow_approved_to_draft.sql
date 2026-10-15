-- Allow APPROVED -> DRAFT (an approved activity returns to draft after its content is edited).
-- Only change vs. the existing trigger: the new transition in the validation block and
-- the audit mapping (reuses the existing action_type value RETURN_TO_DRAFT).
ALTER TRIGGER [dbo].[TR_extracurricular_activity_approval_lifecycle]
ON [dbo].[extracurricular_activity]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    /* ----------------------------------------------------
       Validate approval status transition
       ---------------------------------------------------- */

    IF EXISTS
    (
        SELECT 1
        FROM inserted i
        INNER JOIN deleted d
            ON d.id = i.id
        WHERE
            i.approval_status <> d.approval_status
            AND NOT
            (
                /* DRAFT -> PENDING */
                (d.approval_status = 'DRAFT'
                 AND i.approval_status = 'PENDING')

                OR

                /* PENDING -> APPROVED / REJECTED */
                (d.approval_status = 'PENDING'
                 AND i.approval_status IN
                 (
                     'APPROVED',
                     'REJECTED'
                 ))

                OR

                /* REJECTED -> PENDING / DRAFT */
                (d.approval_status = 'REJECTED'
                 AND i.approval_status IN
                 (
                     'PENDING',
                     'DRAFT'
                 ))

                OR

                /* APPROVED -> PENDING (unapprove) / DRAFT (content edited after approval) */
                (d.approval_status = 'APPROVED'
                 AND i.approval_status IN
                 (
                     'PENDING',
                     'DRAFT'
                 ))
            )
        )
    BEGIN
        THROW 52301,
            'Approval status transition khong hop le.',
            1;
    END;


    /* ----------------------------------------------------
       Audit INSERT
       ---------------------------------------------------- */

    INSERT INTO dbo.activity_approval_history
    (
        activity_id,
        old_approval_status,
        new_approval_status,
        action_type,
        actor_account_id,
        action_at
    )
    SELECT
        i.id,
        NULL,
        i.approval_status,

        CASE i.approval_status
            WHEN 'DRAFT' THEN 'OTHER'
            WHEN 'PENDING' THEN 'SUBMIT'
            WHEN 'APPROVED' THEN 'APPROVE'
            WHEN 'REJECTED' THEN 'REJECT'
            ELSE 'OTHER'
        END,

        TRY_CONVERT
        (
            INT,
            SESSION_CONTEXT(N'account_id')
        ),

        SYSDATETIME()

    FROM inserted i

    LEFT JOIN deleted d
        ON d.id = i.id

    WHERE d.id IS NULL;


    /* ----------------------------------------------------
       Audit UPDATE approval status
       ---------------------------------------------------- */

    INSERT INTO dbo.activity_approval_history
    (
        activity_id,
        old_approval_status,
        new_approval_status,
        action_type,
        actor_account_id,
        action_at
    )
    SELECT
        i.id,
        d.approval_status,
        i.approval_status,

        CASE

            /* DRAFT -> PENDING */
            WHEN i.approval_status = 'PENDING'
                 AND d.approval_status = 'DRAFT'
                THEN 'SUBMIT'

            /* PENDING -> APPROVED */
            WHEN i.approval_status = 'APPROVED'
                 AND d.approval_status = 'PENDING'
                THEN 'APPROVE'

            /* PENDING -> REJECTED */
            WHEN i.approval_status = 'REJECTED'
                 AND d.approval_status = 'PENDING'
                THEN 'REJECT'

            /* APPROVED -> PENDING */
            WHEN i.approval_status = 'PENDING'
                 AND d.approval_status = 'APPROVED'
                THEN 'UNAPPROVE'

            /* REJECTED -> DRAFT, APPROVED -> DRAFT */
            WHEN i.approval_status = 'DRAFT'
                 AND d.approval_status IN ('REJECTED', 'APPROVED')
                THEN 'RETURN_TO_DRAFT'

            ELSE 'OTHER'

        END,

        TRY_CONVERT
        (
            INT,
            SESSION_CONTEXT(N'account_id')
        ),

        SYSDATETIME()

    FROM inserted i

    INNER JOIN deleted d
        ON d.id = i.id

    WHERE i.approval_status <> d.approval_status;

END;
