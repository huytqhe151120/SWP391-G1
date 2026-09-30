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

    private static boolean hasType(CurrentUser user, String type) {
        return user != null && type.equals(user.getType());
    }
}
