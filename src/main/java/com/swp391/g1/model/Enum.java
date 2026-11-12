package com.swp391.g1.model;

public class Enum {
    // Status for entities
    public enum CommonStatus {
        ACTIVE, // Äang hoáº¡t Ä‘á»™ng
        INACTIVE, // KhÃ´ng hoáº¡t Ä‘á»™ng
        BLOCKED // ÄÃ£ bá»‹ khÃ³a
    }

    // Status for activity
    public enum ActivityStatus {
        UPCOMING, // ChÆ°a diá»…n ra
        ONGOING, // Äang diá»…n ra
        COMPLETED, // ÄÃ£ hoÃ n thÃ nh
        CANCELLED // ÄÃ£ há»§y
    }

    // Status for approval process
    public enum ApprovalStatus {
        DRAFT, // Báº£n nhÃ¡p
        PENDING, // Chá» duyá»‡t
        APPROVED, // ÄÃ£ duyá»‡t
        REJECTED // ÄÃ£ tá»« chá»‘i
    }

    // Participation nature of an activity
    public enum ActivityNature {
        REQUIRE_REGISTRATION, // Cáº§n Ä‘Äƒng kÃ½
        VOLUNTARY, // Tá»± nguyá»‡n
        MANDATORY // Báº¯t buá»™c
    }

    // Recurrence mode for activity series
    public enum RecurrenceMode {
        MONTHLY,
        YEARLY
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
        DRAFT, // Chưa mở
        OPENING, // Đang mở đăng ký
        CLOSED // Đã đóng đăng ký
    }

    // Status for student application review
    public enum RegistrationApplicationStatus {
        SUBMITTED, // Chờ duyệt
        APPROVED, // Đã đăng ký
        REJECTED, // Đã từ chối
        CANCELLED, // Sinh viên đã hủy (không đăng ký lại được)
        WITHDRAWN // Sinh viên đã rút đơn (đăng ký lại được)
    }
}
