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
