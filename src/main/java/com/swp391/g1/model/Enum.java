package com.swp391.g1.model;

public class Enum {
    // Status for entities
    public enum CommonStatus {
        ACTIVE,
        INACTIVE
    }

    // Status for
    public enum ActivityStatus {
        UPCOMING,
        ONGOING,
        COMPLETED,
        CANCELLED
    }

    // Status for
    public enum ApprovalStatus {
        DRAFT,
        PENDING,
        APPROVED,
        REJECTED
    }
}
