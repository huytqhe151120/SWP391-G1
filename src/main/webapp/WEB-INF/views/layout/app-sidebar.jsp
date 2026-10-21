<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.swp391.g1.dto.CurrentUser" %>
<%@ page import="com.swp391.g1.util.AuthContext" %>
<%
    CurrentUser navUser = AuthContext.getCurrentUser(request);
    String navType = navUser == null ? "" : navUser.getType();

    // Allow testing different roles via ?previewRole parameter
    String previewRole = request.getParameter("previewRole");
    if (previewRole != null && !previewRole.trim().isEmpty()) {
        navType = previewRole.trim().toUpperCase();
    }

    boolean navAdmin = "ADMIN".equals(navType);
    boolean navStaff = "STAFF".equals(navType);
    boolean navPartnerStaff = "PARTNER_STAFF".equals(navType);
    boolean navStudent = "STUDENT".equals(navType);
    boolean isUniversityStaffOrAdmin = navAdmin || navStaff;
    boolean isGuest = !navAdmin && !navStaff && !navPartnerStaff && !navStudent;

    Object navActiveAttr = request.getAttribute("activeNav");
    String navActive = navActiveAttr == null ? "" : navActiveAttr.toString();
    String uri = request.getRequestURI();
    if (navActive.isEmpty()) {
        if (uri.endsWith("/home") || uri.contains("/home.")) navActive = "home";
        else if (uri.contains("/activity/approval")) navActive = "activity-approval";
        else if (uri.contains("/activities")) navActive = "activities";
        else if (uri.contains("/student-qa")) navActive = "student-qa";
        else if (uri.contains("/qa-management")) navActive = "qa-management";
        else if (uri.contains("/appeal")) navActive = "appeal";
        else if (uri.contains("/notifications")) navActive = "notifications";
        else if (uri.contains("/profile")) navActive = "profile";
        else if (uri.contains("/student-inbox") || uri.contains("/organizer-inbox")) navActive = "inbox";
        else if (uri.contains("/student")) navActive = "students";
        else if (uri.contains("/staff")) navActive = "staff";
        else if (uri.contains("/partner-companies")) navActive = "partner-companies";
        else if (uri.contains("/partner-staffs")) navActive = "partner-staff";
        else if (uri.contains("/accounts")) navActive = "accounts";
    }
    String ctx = request.getContextPath();
%>
<aside class="activity-side-nav" aria-label="Điều hướng hệ thống">
    <nav class="activity-nav-menu">
        <%-- ========================================== --%>
        <%-- 1. STUDENT MENU                            --%>
        <%-- Column 1 in Image 3: Student               --%>
        <%-- ========================================== --%>
        <% if (navStudent || isGuest) { %>
            <a class="<%= "home".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/home"
               <%= "home".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
                </span>Trang chủ
            </a>

            <span class="activity-nav-group">Hoạt động</span>
            <a class="<%= "activities".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/activities"
               <%= "activities".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                </span>Hoạt động ngoại khóa
            </a>

            <span class="activity-nav-group">Hỏi đáp &amp; Kết quả</span>
            <a class="<%= "student-qa".equals(navActive) || "qa".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/student-qa"
               <%= "student-qa".equals(navActive) || "qa".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                </span>Hỏi &amp; Đáp
            </a>

            <a class="<%= "points".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/appeal"
               <%= "points".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                </span>Điểm &amp; Kết quả
            </a>

            <a class="<%= "appeal".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/appeal"
               <%= "appeal".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>
                </span>Phúc khảo
            </a>

        <% } %>

        <%-- ========================================== --%>
        <%-- 2. UNIVERSITY STAFF / ADMIN MENU          --%>
        <%-- Column 2 in Image 3: Staff/Admin           --%>
        <%-- ========================================== --%>
        <% if (isUniversityStaffOrAdmin) { %>
            <a class="<%= "home".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/home"
               <%= "home".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                </span>Trang chủ
            </a>

            <a href="<%= ctx %>/partner-invitations">Lời mời phối hợp</a>
            <span class="activity-nav-group">Quản lý</span>
            <a class="<%= "activities".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/activities"
               <%= "activities".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                </span>Hoạt động ngoại khóa
            </a>

            <a class="<%= "attendance".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/activities"
               <%= "attendance".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"></path><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path></svg>
                </span>Điểm danh &amp; Đăng ký
            </a>

            <a class="<%= "students".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/student"
               <%= "students".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 10v6M2 10l10-5 10 5-10 5z"></path><path d="M6 12v5c0 2 3 3 6 3s6-1 6-3v-5"></path></svg>
                </span>Sinh viên
            </a>

            <a class="<%= "staff".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/staff"
               <%= "staff".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                </span>Nhân viên
            </a>

            <a class="<%= "partner-companies".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/partner-companies"
               <%= "partner-companies".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path></svg>
                </span>Công ty đối tác
            </a>

            <a class="<%= "partner-staff".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/partner-staffs"
               <%= "partner-staff".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                </span>Nhân viên đối tác
            </a>

            <% if (navAdmin) { %>
            <a class="<%= "accounts".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/accounts"
               <%= "accounts".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                </span>Tài khoản
            </a>
            <% } %>

            <span class="activity-nav-group">Hỗ trợ</span>
            <a class="<%= "qa-management".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/qa-management"
               <%= "qa-management".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                </span>Quản lý Hỏi &amp; Đáp
            </a>

            <a class="<%= "points-management".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/appeal?action=manage"
               <%= "points-management".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                </span>Quản lý điểm
            </a>

            <a class="<%= "appeal".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/appeal?action=manage"
               <%= "appeal".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>
                </span>Quản lý phúc khảo
            </a>

            <a class="<%= "reports".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/activities"
               <%= "reports".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                </span>Báo cáo
            </a>
        <% } %>

        <%-- ========================================== --%>
        <%-- 3. PARTNER'S STAFF MENU                   --%>
        <%-- Column 3 in Image 3: Partner's Staff       --%>
        <%-- ========================================== --%>
        <% if (navPartnerStaff) { %>
            <a class="<%= "home".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/home"
               <%= "home".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                </span>Trang chủ
            </a>

            <span class="activity-nav-group">Hoạt động</span>
            <a class="<%= "activities".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/activities"
               <%= "activities".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                </span>Hoạt động được phân công
            </a>

            <a class="<%= "participants".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/activities"
               <%= "participants".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                </span>Danh sách người tham gia
            </a>

            <a class="<%= "activity-info".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/activities"
               <%= "activity-info".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                </span>Thông tin hoạt động
            </a>

            <span class="activity-nav-group">Hỗ trợ</span>
            <a class="<%= "qa-management".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/qa-management"
               <%= "qa-management".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                </span>Quản lý Hỏi &amp; Đáp
            </a>
        <% } %>
        <% if (!isGuest) { %>
            <span class="activity-nav-group">Cá nhân</span>
            <a class="<%= "notifications".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/notifications"
               <%= "notifications".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                </span>Thông báo
            </a>

            <a class="<%= "profile".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/profile"
               <%= "profile".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                </span>Hồ sơ cá nhân
            </a>
        <% } %>
    </nav>
</aside>
