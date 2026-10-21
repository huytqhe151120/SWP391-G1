package com.swp391.g1.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Immutable view of the authenticated account.
 *
 * This is the only principal representation allowed in the HTTP session: it
 * holds no password, no password hash and no Account entity, so credentials
 * cannot leak into a JSP or a log through request/session scope.
 */
public final class CurrentUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final java.util.Map<String, String> ROLE_LABELS = java.util.Map.ofEntries(
            java.util.Map.entry("STAFF", "Nhân viên"),
            java.util.Map.entry("UNIT_HEAD", "Trưởng đơn vị"),
            java.util.Map.entry("UNIT_DEPUTY_HEAD", "Phó đơn vị"),
            java.util.Map.entry("ADMIN", "Admin"),
            java.util.Map.entry("CLASS_MONITOR", "Lớp trưởng"),
            java.util.Map.entry("PARTNER_LEADER", "Giám đốc đối tác"),
            java.util.Map.entry("PARTNER_SECRETARY", "Thư ký đối tác"),
            java.util.Map.entry("PARTNER_STAFF", "Nhân viên đối tác"),
            java.util.Map.entry("STUDENT", "Sinh viên"),
            java.util.Map.entry("SUPER_ADMIN", "Super Admin"),
            java.util.Map.entry("UNIT_SECRETARY", "Thư ký đơn vị"));

    private final int id;
    private final String username;
    private final String type;
    private final String role;
    private final Instant loginAt;
    private final String displayName;

    public CurrentUser(int id, String username, String type, String role, Instant loginAt) {
        this(id, username, type, role, loginAt, null);
    }

    public CurrentUser(int id, String username, String type, String role, Instant loginAt, String displayName) {
        this.id = id;
        this.username = username;
        this.type = type;
        this.role = role;
        this.loginAt = loginAt;
        this.displayName = displayName;
    }

    /** Vietnamese label of the role; unknown roles are shown as stored. */
    public String getRoleLabel() {
        if (role == null) {
            return "";
        }
        String label = ROLE_LABELS.get(role);
        return label == null ? role : label;
    }

    /** Full name of the profile linked to the account; falls back to the username. */
    public String getDisplayName() {
        return displayName == null || displayName.isBlank() ? username : displayName;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getType() {
        return type;
    }

    public String getRole() {
        return role;
    }

    /** When this session was established; informational only. */
    public Instant getLoginAt() {
        return loginAt;
    }
}
