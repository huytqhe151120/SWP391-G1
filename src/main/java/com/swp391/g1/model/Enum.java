package com.swp391.g1.model;

public class Enum {
    // Status for entities
    public enum CommonStatus {
        ACTIVE,
        INACTIVE
    }

    // Status for activity
    public enum ActivityStatus {
        UPCOMING, // Sắp diễn ra
        ONGOING, // Đang diễn ra
        COMPLETED, // Đã hoàn thành
        CANCELLED // Đã hủy
    }

    // Status for approval process
    public enum ApprovalStatus {
        DRAFT, // Bản nháp
        PENDING, // Chờ duyệt
        APPROVED, // Đã duyệt
        REJECTED // Đã từ chối
    }

    // Participation nature of an activity
    public enum ActivityNature {
        REQUIRE_REGISTRATION,
        VOLUNTARY,
        MANDATORY
    }

    // Attendance status of a student in an activity (activity_participant.attendance_status)
    public enum ParticipantStatus {
        NOT_YET, // Chưa điểm danh
        ATTENDED, // Có mặt
        ABSENT, // Vắng mặt
        IN_LATE, // Đi muộn
        OUT_EARLY // Về sớm
    }
}
