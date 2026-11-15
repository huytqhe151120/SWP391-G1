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

    // Lifecycle of an organizer recruitment round
    public enum OrganizerRecruitmentRoundStatus {
        DRAFT, // Chưa đến thời gian nhận hồ sơ
        OPEN, // Đang nhận hồ sơ
        CLOSED // Đã hết hạn hoặc hoạt động đã bắt đầu
    }

    // Review status of an organizer recruitment application
    public enum OrganizerApplicationStatus {
        PENDING, // Chờ xét
        INTERVIEW_INVITED, // Đã mời phỏng vấn
        INTERVIEWED, // Đã phỏng vấn
        ACCEPTED, // Trúng tuyển
        REJECTED, // Không đạt
        WITHDRAWN // Đã rút
    }

    // Lifecycle of an organizer interview
    public enum OrganizerInterviewStatus {
        SCHEDULED, // Đang chờ xác nhận/phỏng vấn
        CONFIRMED, // Sinh viên đã xác nhận
        CANCELLED, // Lịch đã hủy
        DONE // Đã phỏng vấn
    }
}
