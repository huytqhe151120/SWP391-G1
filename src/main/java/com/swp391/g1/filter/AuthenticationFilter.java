package com.swp391.g1.filter;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.swp391.g1.util.AuthContext;

/**
 * Central authentication gate: it answers only "is this request
 * authenticated?". Type/role authorization is deliberately not here; it stays
 * in AccessPolicy at the controller layer.
 *
 * Everything that is not explicitly public requires a session (fail-closed).
 */
@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/login", "/logout");
    private static final String PUBLIC_PREFIX = "/assets/";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (isPublic(httpRequest) || AuthContext.isAuthenticated(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.sendRedirect(contextPath(httpRequest) + "/login");
    }

    /**
     * Login must stay reachable; the login page and static assets must load.
     *
     * The raw request URI is compared on purpose: a URL-encoded path does not
     * match the whitelist and is therefore treated as protected, which keeps
     * this check fail-closed. Do not "fix" it by decoding the path.
     */
    private static boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return PUBLIC_PATHS.contains(path) || path.startsWith(PUBLIC_PREFIX);
    }

    private static String contextPath(HttpServletRequest request) {
        String ctx = request.getContextPath();
        return ctx == null ? "" : ctx;
    }
}
