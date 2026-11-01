package com.swp391.g1.model;

public class Enum {
    // Status for entities
    public enum CommonStatus {
        ACTIVE, // Đang hoạt động
        INACTIVE // Không hoạt động
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
        SUBMITTED, // Chờ xử lý
        APPROVED, // Đã duyệt
        REJECTED, // Đã từ chối
        CANCELLED, // Đã hủy (đơn đã duyệt, không đăng ký lại được)
        WITHDRAWN // Đã rút đơn (đăng ký lại được)

    }

}
