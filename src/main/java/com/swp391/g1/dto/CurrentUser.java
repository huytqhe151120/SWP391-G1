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

    private final int id;
    private final String username;
    private final String type;
    private final String role;
    private final Instant loginAt;

    public CurrentUser(int id, String username, String type, String role, Instant loginAt) {
        this.id = id;
        this.username = username;
        this.type = type;
        this.role = role;
        this.loginAt = loginAt;
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
