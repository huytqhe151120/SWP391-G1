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

    // Registration form status
    public enum RegistrationFormStatus {
        DRAFT, // Bản nháp
        OPEN, // Đang mở đăng ký
        CLOSED // Đã đóng đăng ký
    }

    // Participation nature of an activity
    public enum ActivityNature {
        REQUIRE_REGISTRATION,
        VOLUNTARY,
        MANDATORY
    }
}
