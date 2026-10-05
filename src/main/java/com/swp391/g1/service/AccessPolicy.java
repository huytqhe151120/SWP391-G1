package com.swp391.g1.service;

import com.swp391.g1.dto.CurrentUser;

/**
 * Central authorization policy: feature rules live here only, so callers never
 * duplicate type/role checks. The role list is not final yet, therefore only
 * rules confirmed by the business are implemented.
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
