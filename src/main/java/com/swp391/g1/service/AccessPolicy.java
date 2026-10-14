package com.swp391.g1.service;

import com.swp391.g1.dto.CurrentUser;

/**
 * Central authorization policy: feature rules live here so callers never
 * duplicate type/role checks.
 */
public final class AccessPolicy {

    /** Account Management is owned by the ADMIN account type (confirmed rule). */
    private static final String ADMIN_TYPE = "ADMIN";

    private AccessPolicy() {
    }

    public static boolean canManageAccounts(CurrentUser user) {
        return hasType(user, ADMIN_TYPE);
    }

    /** ADMIN and STAFF can create, edit, and delete extracurricular activities. */
    public static boolean canManageActivities(CurrentUser user) {
        return hasType(user, ADMIN_TYPE) || hasType(user, "STAFF");
    }

    /** ADMIN and selected STAFF roles can assign or remove staff from an activity. */
    public static boolean canAssignStaffToActivity(CurrentUser user) {
        if (hasType(user, ADMIN_TYPE)) {
            return true;
        }
        if (!hasType(user, "STAFF") || user.getRole() == null) {
            return false;
        }
        return switch (user.getRole()) {
            case "UNIT_DEPUTY_HEAD", "UNIT_HEAD", "UNIT_SECRETARY" -> true;
            default -> false;
        };
    }

    /** PARTNER_STAFF accounts with leader or secretary role can manage partner staff of an activity. */
    public static boolean canAssignPartnerStaffToActivity(CurrentUser user) {
        if (user == null || user.getRole() == null || !"PARTNER_STAFF".equalsIgnoreCase(user.getType())) {
            return false;
        }
        return "PARTNER_LEADER".equals(user.getRole()) || "PARTNER_SECRETARY".equals(user.getRole());
    }

    /** ADMIN and STAFF own the Q&A moderation workflow. */
    public static boolean canManageQuestions(CurrentUser user) {
        return hasType(user, ADMIN_TYPE) || hasType(user, "STAFF");
    }

    /** Only students can submit questions and use the student Q&A page. */
    public static boolean canUseStudentQuestions(CurrentUser user) {
        return hasType(user, "STUDENT");
    }

    /** ADMIN organizers and STAFF can access their resolved 1-to-1 inbox. */
    public static boolean canUseSupportInbox(CurrentUser user) {
        return hasType(user, ADMIN_TYPE) || hasType(user, "STAFF");
    }

    private static boolean hasType(CurrentUser user, String type) {
        return user != null && type.equals(user.getType());
    }
}
