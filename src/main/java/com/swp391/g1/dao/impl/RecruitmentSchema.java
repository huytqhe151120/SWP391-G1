package com.swp391.g1.dao.impl;

import com.swp391.g1.common.DBContext;
import com.swp391.g1.dao.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Tạo (idempotent) các bảng của chức năng tuyển ban tổ chức. */
public final class RecruitmentSchema {

    private static volatile boolean ensured = false;

    private static final String DDL = """
            IF OBJECT_ID(N'dbo.organizer_recruitment_round', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.organizer_recruitment_round (
                    id                 INT IDENTITY(1,1) NOT NULL,
                    activity_id        INT               NOT NULL,
                    title              NVARCHAR(255)     NOT NULL,
                    description        NVARCHAR(MAX)     NULL,
                    open_at            DATETIME2         NULL,
                    close_at           DATETIME2         NULL,
                    status             VARCHAR(20)       NOT NULL CONSTRAINT DF_orr_status DEFAULT ('DRAFT'),
                    interview_required BIT               NOT NULL CONSTRAINT DF_orr_interview DEFAULT (1),
                    created_by         INT               NULL,
                    created_at         DATETIME2         NOT NULL CONSTRAINT DF_orr_created DEFAULT (SYSDATETIME()),
                    updated_at         DATETIME2         NOT NULL CONSTRAINT DF_orr_updated DEFAULT (SYSDATETIME()),
                    CONSTRAINT PK_organizer_recruitment_round PRIMARY KEY CLUSTERED (id),
                    CONSTRAINT UQ_orr_activity UNIQUE (activity_id),
                    CONSTRAINT FK_orr_activity FOREIGN KEY (activity_id) REFERENCES dbo.extracurricular_activity (id),
                    CONSTRAINT CK_orr_status CHECK (status IN ('DRAFT','OPEN','CLOSED')),
                    CONSTRAINT CK_orr_time CHECK (open_at IS NULL OR close_at IS NULL OR close_at > open_at)
                );
            END;
            IF OBJECT_ID(N'dbo.organizer_recruitment_position', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.organizer_recruitment_position (
                    id         INT IDENTITY(1,1) NOT NULL,
                    round_id   INT               NOT NULL,
                    name       NVARCHAR(150)     NOT NULL,
                    quota      INT               NOT NULL,
                    sort_order INT               NOT NULL CONSTRAINT DF_orp_sort DEFAULT (0),
                    CONSTRAINT PK_organizer_recruitment_position PRIMARY KEY CLUSTERED (id),
                    CONSTRAINT FK_orp_round FOREIGN KEY (round_id)
                        REFERENCES dbo.organizer_recruitment_round (id) ON DELETE CASCADE,
                    CONSTRAINT CK_orp_quota CHECK (quota >= 1)
                );
            END;
            IF OBJECT_ID(N'dbo.organizer_recruitment_question', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.organizer_recruitment_question (
                    id            INT IDENTITY(1,1) NOT NULL,
                    round_id      INT               NOT NULL,
                    question_text NVARCHAR(500)     NOT NULL,
                    question_type VARCHAR(20)       NOT NULL,
                    required      BIT               NOT NULL CONSTRAINT DF_orq_required DEFAULT (1),
                    options       NVARCHAR(MAX)     NULL,
                    sort_order    INT               NOT NULL CONSTRAINT DF_orq_sort DEFAULT (0),
                    CONSTRAINT PK_organizer_recruitment_question PRIMARY KEY CLUSTERED (id),
                    CONSTRAINT FK_orq_round FOREIGN KEY (round_id)
                        REFERENCES dbo.organizer_recruitment_round (id) ON DELETE CASCADE,
                    CONSTRAINT CK_orq_type CHECK (question_type IN ('TEXT','DROPDOWN','MULTI_CHOICE'))
                );
            END;
            IF OBJECT_ID(N'dbo.organizer_application', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.organizer_application (
                    id                   INT IDENTITY(1,1) NOT NULL,
                    round_id             INT               NOT NULL,
                    student_id           INT               NOT NULL,
                    choice1_position_id  INT               NOT NULL,
                    choice2_position_id  INT               NULL,
                    accepted_position_id INT               NULL,
                    status               VARCHAR(20)       NOT NULL CONSTRAINT DF_oa_status DEFAULT ('PENDING'),
                    note                 NVARCHAR(1000)    NULL,
                    internal_note        NVARCHAR(1000)    NULL,
                    decision_reason      NVARCHAR(1000)    NULL,
                    withdraw_reason      NVARCHAR(1000)    NULL,
                    submitted_at         DATETIME2         NOT NULL CONSTRAINT DF_oa_submitted DEFAULT (SYSDATETIME()),
                    decided_at           DATETIME2         NULL,
                    updated_at           DATETIME2         NOT NULL CONSTRAINT DF_oa_updated DEFAULT (SYSDATETIME()),
                    CONSTRAINT PK_organizer_application PRIMARY KEY CLUSTERED (id),
                    CONSTRAINT UQ_oa_round_student UNIQUE (round_id, student_id),
                    CONSTRAINT FK_oa_round FOREIGN KEY (round_id) REFERENCES dbo.organizer_recruitment_round (id),
                    CONSTRAINT FK_oa_student FOREIGN KEY (student_id) REFERENCES dbo.student (id),
                    CONSTRAINT FK_oa_choice1 FOREIGN KEY (choice1_position_id) REFERENCES dbo.organizer_recruitment_position (id),
                    CONSTRAINT FK_oa_choice2 FOREIGN KEY (choice2_position_id) REFERENCES dbo.organizer_recruitment_position (id),
                    CONSTRAINT FK_oa_accepted FOREIGN KEY (accepted_position_id) REFERENCES dbo.organizer_recruitment_position (id),
                    CONSTRAINT CK_oa_status CHECK (status IN ('PENDING','INTERVIEW_INVITED','INTERVIEWED','ACCEPTED','REJECTED','WITHDRAWN')),
                    CONSTRAINT CK_oa_choices CHECK (choice2_position_id IS NULL OR choice2_position_id <> choice1_position_id),
                    CONSTRAINT CK_oa_accepted CHECK (
                        (status = 'ACCEPTED' AND accepted_position_id IS NOT NULL)
                        OR (status <> 'ACCEPTED' AND accepted_position_id IS NULL))
                );
                CREATE INDEX IX_oa_round_status ON dbo.organizer_application (round_id, status);
                CREATE INDEX IX_oa_student ON dbo.organizer_application (student_id);
                CREATE INDEX IX_oa_accepted_position ON dbo.organizer_application (accepted_position_id) WHERE accepted_position_id IS NOT NULL;
            END;
            IF OBJECT_ID(N'dbo.organizer_application_answer', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.organizer_application_answer (
                    id             INT IDENTITY(1,1) NOT NULL,
                    application_id INT               NOT NULL,
                    question_id    INT               NOT NULL,
                    answer_value   NVARCHAR(MAX)     NULL,
                    CONSTRAINT PK_organizer_application_answer PRIMARY KEY CLUSTERED (id),
                    CONSTRAINT UQ_oaa_application_question UNIQUE (application_id, question_id),
                    CONSTRAINT FK_oaa_application FOREIGN KEY (application_id)
                        REFERENCES dbo.organizer_application (id) ON DELETE CASCADE,
                    CONSTRAINT FK_oaa_question FOREIGN KEY (question_id)
                        REFERENCES dbo.organizer_recruitment_question (id)
                );
            END;
            IF OBJECT_ID(N'dbo.organizer_interview', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.organizer_interview (
                    id               INT IDENTITY(1,1) NOT NULL,
                    application_id   INT               NOT NULL,
                    scheduled_at     DATETIME2         NOT NULL,
                    format           VARCHAR(10)       NOT NULL,
                    location         NVARCHAR(500)     NOT NULL,
                    note             NVARCHAR(1000)    NULL,
                    confirm_deadline DATETIME2         NOT NULL,
                    confirmed_at     DATETIME2         NULL,
                    reminder_sent_at DATETIME2         NULL,
                    status           VARCHAR(15)       NOT NULL CONSTRAINT DF_oi_status DEFAULT ('SCHEDULED'),
                    evaluation       NVARCHAR(1000)    NULL,
                    created_by       INT               NULL,
                    created_at       DATETIME2         NOT NULL CONSTRAINT DF_oi_created DEFAULT (SYSDATETIME()),
                    updated_at       DATETIME2         NOT NULL CONSTRAINT DF_oi_updated DEFAULT (SYSDATETIME()),
                    CONSTRAINT PK_organizer_interview PRIMARY KEY CLUSTERED (id),
                    CONSTRAINT UQ_oi_application UNIQUE (application_id),
                    CONSTRAINT FK_oi_application FOREIGN KEY (application_id)
                        REFERENCES dbo.organizer_application (id) ON DELETE CASCADE,
                    CONSTRAINT CK_oi_format CHECK (format IN ('OFFLINE','ONLINE')),
                    CONSTRAINT CK_oi_status CHECK (status IN ('SCHEDULED','CONFIRMED','CANCELLED','DONE'))
                );
            END;
            IF COL_LENGTH(N'dbo.activity_participant', N'participation_role') IS NULL
                ALTER TABLE dbo.activity_participant
                    ADD participation_role VARCHAR(15) NOT NULL
                        CONSTRAINT DF_activity_participant_role DEFAULT ('PARTICIPANT');
            IF COL_LENGTH(N'dbo.student_activity_result', N'participation_role') IS NULL
                ALTER TABLE dbo.student_activity_result
                    ADD participation_role VARCHAR(15) NOT NULL
                        CONSTRAINT DF_student_activity_result_role DEFAULT ('PARTICIPANT');
            IF COL_LENGTH(N'dbo.activity_registration', N'cancelled_for_organizer') IS NULL
                ALTER TABLE dbo.activity_registration
                    ADD cancelled_for_organizer BIT NOT NULL
                        CONSTRAINT DF_registration_cancelled_for_organizer DEFAULT (0);
            """;

    private RecruitmentSchema() {
    }

    public static void ensure() {
        if (ensured) {
            return;
        }
        synchronized (RecruitmentSchema.class) {
            if (ensured) {
                return;
            }
            try (Connection c = DBContext.getConnection(); Statement s = c.createStatement()) {
                c.setAutoCommit(false);
                try {
                    s.execute(DDL);
                    s.executeUpdate("UPDATE sar SET participation_role=ap.participation_role "
                            + "FROM dbo.student_activity_result sar "
                            + "JOIN dbo.activity_participant ap ON ap.activity_id=sar.activity_id "
                            + "AND ap.student_id=sar.student_id "
                            + "WHERE sar.participation_role<>ap.participation_role");
                    c.commit();
                    ensured = true;
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                throw new DataAccessException("Không thể khởi tạo cấu trúc dữ liệu tuyển ban tổ chức.", e);
            }
        }
    }
}
