package com.swp391.g1.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.swp391.g1.dto.CurrentUser;

/**
 * Single entry point for session authentication state, so no servlet or JSP
 * ever reads or writes the authentication attribute directly.
 *
 * Only a CurrentUser is stored; sessions are never created just to answer
 * "is this request authenticated?".
 */
public final class AuthContext {

    /** Namespaced key: keeps future modules from colliding with a plain "user". */
    public static final String SESSION_USER_KEY = "com.swp391.g1.auth.CURRENT_USER";

    private AuthContext() {
    }

    /**
     * Establishes the authenticated session. The session id is rotated first so
     * a session id captured before login cannot be reused afterwards.
     */
    public static void createSession(HttpServletRequest request, CurrentUser currentUser) {
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(SESSION_USER_KEY, currentUser);
    }

    public static CurrentUser getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER_KEY);
        return value instanceof CurrentUser ? (CurrentUser) value : null;
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        return getCurrentUser(request) != null;
    }

    /** Logs the user out by invalidating the whole session, not one attribute. */
    public static void clearSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
