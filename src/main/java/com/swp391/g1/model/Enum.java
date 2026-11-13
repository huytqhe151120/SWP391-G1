package com.swp391.g1.model;

public class Enum {
    // Status for entities
    public enum CommonStatus {
        ACTIVE, // Đang hoạt động
        INACTIVE, // Không hoạt động
        BLOCKED // Đã bị khóa
    }

    // Status for activity
    public enum ActivityStatus {
        UPCOMING, // Chưa diễn ra
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
        REQUIRE_REGISTRATION, // Cần đăng ký
        VOLUNTARY, // Tự nguyện
        MANDATORY // Bắt buộc
    }

    // Attendance status of a student in an activity (activity_participant.attendance_status)
    public enum ParticipantStatus {
        NOT_YET, // Chưa điểm danh
        ATTENDED, // Có mặt
        ABSENT, // Vắng mặt
        IN_LATE, // Đi muộn
        OUT_EARLY // Về sớm
    }

    // Registration source status in activity_participant.registration_status
    public enum ParticipantRegistrationStatus {
        REGISTERED, // Sinh viên tự đăng ký
        AUTO_ENROLLED, // Hệ thống tự động ghi danh
        STAFF_ENROLLED // Cán bộ/Admin thêm thủ công
    }
    // Status for registration form
    public enum RegistrationFormStatus {
        DRAFT, // Chưa mở đăng ký
        OPENING, // Đang mở đăng ký
        CLOSED // Đã đóng đăng ký
    }

    // Status for student application review
    public enum RegistrationApplicationStatus {
        SUBMITTED, // Chờ xử lý
        APPROVED, // Đã duyệt
        REJECTED, // Bị từ chối
        CANCELLED, // Sinh viên đã hủy (không đăng ký lại được)
        WITHDRAWN // Sinh viên đã rút đơn (đăng ký lại được)
    }

}
