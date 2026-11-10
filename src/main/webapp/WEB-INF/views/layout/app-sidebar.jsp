<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.swp391.g1.dto.CurrentUser" %>
<%@ page import="com.swp391.g1.service.AccessPolicy" %>
<%@ page import="com.swp391.g1.util.AuthContext" %>
<%
    CurrentUser navUser = AuthContext.getCurrentUser(request);
    String navType = navUser == null ? "" : navUser.getType();
    boolean navAdmin = "ADMIN".equals(navType);
    boolean navStaff = "STAFF".equals(navType);
    boolean navStudent = "STUDENT".equals(navType);
    boolean navCanTakeAttendance = AccessPolicy.canTakeAttendance(navUser);
    Object navActiveAttr = request.getAttribute("activeNav");
    String navActive = navActiveAttr == null ? "" : navActiveAttr.toString();
    String reqUri = request.getRequestURI() == null ? "" : request.getRequestURI();
    if (navActive.isEmpty()) {
        if (reqUri.contains("/qa-management") || reqUri.contains("/answer-question")) navActive = "qa-management";
        else if (reqUri.contains("/organizer-inbox") || reqUri.contains("/organizer-chat")) navActive = "organizer-inbox";
        else if (reqUri.contains("/student-qa")) navActive = "student-qa";
        else if (reqUri.contains("/student-inbox") || reqUri.contains("/student-chat")) navActive = "student-inbox";
        else if (reqUri.contains("/attendance")) navActive = "attendance";
        else if (reqUri.contains("/student-grades")) navActive = "student-grades";
    }
    boolean isActivityGroupActive = "activities".equals(navActive)
            || "registration-forms".equals(navActive)
            || "attendance".equals(navActive)
            || "student-grades".equals(navActive)
            || "partner-invitations".equals(navActive);
    boolean isHrGroupActive = "students".equals(navActive)
            || "staff".equals(navActive)
            || "accounts".equals(navActive);
    boolean isPartnerGroupActive = "partner-companies".equals(navActive)
            || "partner-staff".equals(navActive);

    String ctx = request.getContextPath();
    String displayName = navUser == null || navUser.getDisplayName() == null ? "" : navUser.getDisplayName().trim();
    String avatarInitial = displayName.isEmpty() ? "U" : displayName.substring(0, 1).toUpperCase();
    pageContext.setAttribute("navDisplayName", displayName);
    pageContext.setAttribute("navRoleLabel", navUser == null ? "" : navUser.getRoleLabel());
    pageContext.setAttribute("navAvatarInitial", avatarInitial);
%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link rel="stylesheet" href="<%= ctx %>/assets/css/sidebar.css?v=20261107-2">
<aside class="activity-side-nav" id="appSidebar" aria-label="Điều hướng hệ thống">
    <div class="activity-sidebar-head">
        <a class="activity-sidebar-brand" href="<%= ctx %>/home" title="Trang chủ EAMS">
            <img class="activity-sidebar-brand-logo" src="<%= ctx %>/assets/images/logo-positive.svg" alt="EAMS Logo" width="36" height="36">
            <span class="activity-sidebar-brand-copy">
                <strong class="activity-sidebar-brand-eams">EAMS</strong>
                <span class="activity-sidebar-brand-sub">UNIVERSITY</span>
            </span>
        </a>
        <button type="button" class="activity-sidebar-toggle" id="sidebarToggleBtn"
                aria-label="Thu gọn thanh điều hướng" aria-expanded="true" title="Thu gọn thanh bên">
            <svg class="sidebar-svg-icon activity-sidebar-toggle-icon" viewBox="0 0 24 24" aria-hidden="true">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <path d="M9 3v18"/>
                <path class="activity-sidebar-toggle-chevron" d="m15 15-3-3 3-3"/>
            </svg>
        </button>
    </div>

    <nav class="activity-nav-menu">
        <!-- Nhóm: QUẢN LÝ -->
        <span class="activity-nav-group">Quản lý</span>

        <!-- 1. Quản lý Hoạt động -->
        <div class="activity-nav-accordion is-open <%= isActivityGroupActive ? "has-active" : "" %>" data-group="activity">
            <button type="button" class="activity-nav-accordion-trigger" aria-expanded="true" title="Quản lý Hoạt động">
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                        <rect width="18" height="18" x="3" y="4" rx="2"/>
                        <line x1="16" x2="16" y1="2" y2="6"/>
                        <line x1="8" x2="8" y1="2" y2="6"/>
                        <line x1="3" x2="21" y1="10" y2="10"/>
                    </svg>
                </span>
                <span class="activity-nav-label">Quản lý Hoạt động</span>
                <svg class="sidebar-svg-icon activity-nav-chevron" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m6 9 6 6 6-6"/>
                </svg>
            </button>
            <div class="activity-nav-submenu">
                <a class="activity-nav-item <%= "activities".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/activities"
                   title="Hoạt động ngoại khóa"
                   <%= "activities".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <rect width="18" height="18" x="3" y="4" rx="2"/>
                            <line x1="16" x2="16" y1="2" y2="6"/>
                            <line x1="8" x2="8" y1="2" y2="6"/>
                            <line x1="3" x2="21" y1="10" y2="10"/>
                            <path d="M8 14h.01"/><path d="M12 14h.01"/><path d="M16 14h.01"/>
                            <path d="M8 18h.01"/><path d="M12 18h.01"/><path d="M16 18h.01"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Hoạt động ngoại khóa</span>
                </a>
                <% if (navAdmin || navStaff) { %>
                <a class="activity-nav-item <%= "registration-forms".equals(navActive) ? "active" : "" %>"
                   href="<%= ctx %>/registration-forms"
                   title="Đăng ký tham gia"
                   <%= "registration-forms".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <rect width="18" height="18" x="3" y="4" rx="2"/>
                            <line x1="16" x2="16" y1="2" y2="6"/>
                            <line x1="8" x2="8" y1="2" y2="6"/>
                            <line x1="3" x2="21" y1="10" y2="10"/>
                            <path d="M8 14h8"/>
                            <path d="M8 18h5"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Đăng ký tham gia</span>
                </a>
                <% } else if (navStudent) { %>
                <a class="activity-nav-item <%= "registration-forms".equals(navActive) ? "active" : "" %>"
                   href="<%= ctx %>/registration-forms?action=myApplications"
                   title="Đơn đăng ký"
                   <%= "registration-forms".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                            <path d="M14 2v6h6"/>
                            <path d="M8 13h8"/>
                            <path d="M8 17h8"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Đơn đăng ký</span>
                </a>
                <% } %>
                <% if (navCanTakeAttendance) { %>
                <a class="activity-nav-item <%= "attendance".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/attendance"
                   title="Điểm danh sinh viên"
                   <%= "attendance".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <rect width="8" height="4" x="8" y="2" rx="1" ry="1"/>
                            <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/>
                            <path d="m9 14 2 2 4-4"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Điểm danh sinh viên</span>
                </a>
                <% } %>
                <% if (navCanTakeAttendance || navStudent) { %>
                <a class="activity-nav-item <%= "student-grades".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/student-grades"
                   title="<%= navStudent ? "Điểm hoạt động của tôi" : "Điểm hoạt động" %>"
                   <%= "student-grades".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label"><%= navStudent ? "Điểm hoạt động của tôi" : "Điểm hoạt động" %></span>
                </a>
                <% } %>
                <a class="activity-nav-item <%= "partner-invitations".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/partner-invitations"
                   title="Lời mời hợp tác"
                   <%= "partner-invitations".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <rect width="20" height="16" x="2" y="4" rx="2"/>
                            <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Lời mời hợp tác</span>
                </a>
            </div>
        </div>

        <!-- 2. Quản lý Nhân sự -->
        <div class="activity-nav-accordion is-open <%= isHrGroupActive ? "has-active" : "" %>" data-group="hr">
            <button type="button" class="activity-nav-accordion-trigger" aria-expanded="true" title="Quản lý Nhân sự">
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
                        <circle cx="9" cy="7" r="4"/>
                        <path d="M22 21v-2a4 4 0 0 0-3-3.87"/>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                    </svg>
                </span>
                <span class="activity-nav-label">Quản lý Nhân sự</span>
                <svg class="sidebar-svg-icon activity-nav-chevron" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m6 9 6 6 6-6"/>
                </svg>
            </button>
            <div class="activity-nav-submenu">
                <a class="activity-nav-item <%= "students".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/student"
                   title="Sinh viên"
                   <%= "students".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <path d="M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z"/>
                            <path d="M22 10v6"/>
                            <path d="M6 12.5V16a6 3 0 0 0 12 0v-3.5"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Sinh viên</span>
                </a>
                <a class="activity-nav-item <%= "staff".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/staff"
                   title="Nhân viên"
                   <%= "staff".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <rect width="20" height="14" x="2" y="7" rx="2" ry="2"/>
                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Nhân viên</span>
                </a>
                <% if (navAdmin) { %>
                <a class="activity-nav-item <%= "accounts".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/accounts"
                   title="Tài khoản"
                   <%= "accounts".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Tài khoản</span>
                </a>
                <% } %>
            </div>
        </div>

        <!-- 3. Đối tác -->
        <div class="activity-nav-accordion is-open <%= isPartnerGroupActive ? "has-active" : "" %>" data-group="partner">
            <button type="button" class="activity-nav-accordion-trigger" aria-expanded="true" title="Đối tác">
                <span class="activity-nav-icon" aria-hidden="true">
                    <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                        <path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z"/>
                        <path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2"/>
                        <path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2"/>
                    </svg>
                </span>
                <span class="activity-nav-label">Đối tác</span>
                <svg class="sidebar-svg-icon activity-nav-chevron" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m6 9 6 6 6-6"/>
                </svg>
            </button>
            <div class="activity-nav-submenu">
                <a class="activity-nav-item <%= "partner-companies".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/partner-companies"
                   title="Công ty đối tác"
                   <%= "partner-companies".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <path d="M6 22V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v18Z"/>
                            <path d="M6 12H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h2"/>
                            <path d="M18 9h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-2"/>
                            <path d="M10 6h4"/><path d="M10 10h4"/><path d="M10 14h4"/><path d="M10 18h4"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Công ty đối tác</span>
                </a>
                <a class="activity-nav-item <%= "partner-staff".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/partner-staffs"
                   title="Nhân viên đối tác"
                   <%= "partner-staff".equals(navActive) ? "aria-current=\"page\"" : "" %>>
                    <span class="activity-nav-icon" aria-hidden="true">
                        <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                            <path d="M17 18a2 2 0 0 0-2-2H9a2 2 0 0 0-2 2"/>
                            <rect width="18" height="18" x="3" y="4" rx="2"/>
                            <circle cx="12" cy="10" r="2"/>
                            <line x1="8" x2="8" y1="2" y2="4"/>
                            <line x1="16" x2="16" y1="2" y2="4"/>
                        </svg>
                    </span>
                    <span class="activity-nav-label">Nhân viên đối tác</span>
                </a>
            </div>
        </div>

        <!-- Nhóm: HỖ TRỢ (Admin / Staff) -->
        <% if (navAdmin || navStaff) { %>
        <span class="activity-nav-group">Hỗ trợ</span>
        <a class="activity-nav-item <%= "qa-management".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/qa-management"
           title="Quản lý Hỏi &amp; Đáp"
           <%= "qa-management".equals(navActive) ? "aria-current=\"page\"" : "" %>>
            <span class="activity-nav-icon" aria-hidden="true">
                <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="10"/>
                    <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                    <path d="M12 17h.01"/>
                </svg>
            </span>
            <span class="activity-nav-label">Quản lý Hỏi &amp; Đáp</span>
        </a>
        <a class="activity-nav-item <%= "organizer-inbox".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/organizer-inbox"
           title="Hộp thư hỗ trợ"
           <%= "organizer-inbox".equals(navActive) ? "aria-current=\"page\"" : "" %>>
            <span class="activity-nav-icon" aria-hidden="true">
                <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
            </span>
            <span class="activity-nav-label">Hộp thư hỗ trợ</span>
        </a>
        <% } %>

        <!-- Nhóm: HỖ TRỢ SINH VIÊN -->
        <% if (navStudent) { %>
        <span class="activity-nav-group">Hỗ trợ sinh viên</span>
        <a class="activity-nav-item <%= "student-qa".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/student-qa"
           title="Hỏi &amp; Đáp"
           <%= "student-qa".equals(navActive) ? "aria-current=\"page\"" : "" %>>
            <span class="activity-nav-icon" aria-hidden="true">
                <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                    <circle cx="12" cy="12" r="10"/>
                    <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                    <path d="M12 17h.01"/>
                </svg>
            </span>
            <span class="activity-nav-label">Hỏi &amp; Đáp</span>
        </a>
        <a class="activity-nav-item <%= "student-inbox".equals(navActive) ? "active" : "" %>" href="<%= ctx %>/student-inbox"
           title="Hộp thư của tôi"
           <%= "student-inbox".equals(navActive) ? "aria-current=\"page\"" : "" %>>
            <span class="activity-nav-icon" aria-hidden="true">
                <svg class="sidebar-svg-icon" viewBox="0 0 24 24">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
            </span>
            <span class="activity-nav-label">Hộp thư của tôi</span>
        </a>
        <% } %>
    </nav>

    <!-- User Profile Box (Sticky Bottom) -->
    <% if (navUser != null) { %>
    <div class="activity-sidebar-account">
        <div class="activity-sidebar-profile" title="<c:out value='${navDisplayName}'/> - <c:out value='${navRoleLabel}'/>">
            <span class="activity-sidebar-avatar" aria-hidden="true"><c:out value="${navAvatarInitial}"/></span>
            <div class="activity-sidebar-user">
                <strong><c:out value="${navDisplayName}"/></strong>
                <span><c:out value="${navRoleLabel}"/></span>
            </div>
        </div>
        <form method="post" action="<%= ctx %>/logout">
            <button class="activity-sidebar-logout" type="submit" title="Đăng xuất">
                <svg class="sidebar-svg-icon activity-sidebar-logout-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
                <span class="activity-nav-label">Đăng xuất</span>
            </button>
        </form>
    </div>
    <% } %>
</aside>
<script>
    (function () {
        var COLLAPSE_KEY = 'eams_sidebar_collapsed';
        var GROUP_PREFIX = 'eams_sidebar_group_';
        var sidebar = document.getElementById('appSidebar');
        var shell = sidebar ? sidebar.closest('.activity-app-shell') : null;
        var toggleBtn = document.getElementById('sidebarToggleBtn');
        if (!sidebar || !toggleBtn) return;

        function applyCollapse(collapsed) {
            sidebar.classList.toggle('is-collapsed', collapsed);
            if (shell) shell.classList.toggle('is-sidebar-collapsed', collapsed);
            toggleBtn.setAttribute('aria-expanded', String(!collapsed));
            toggleBtn.setAttribute('aria-label', collapsed ? 'Mở rộng thanh điều hướng' : 'Thu gọn thanh điều hướng');
            toggleBtn.setAttribute('title', collapsed ? 'Mở rộng thanh bên' : 'Thu gọn thanh bên');
        }

        try {
            if (localStorage.getItem(COLLAPSE_KEY) === '1') {
                applyCollapse(true);
            }
        } catch (e) {}

        toggleBtn.addEventListener('click', function () {
            var next = !sidebar.classList.contains('is-collapsed');
            applyCollapse(next);
            try {
                localStorage.setItem(COLLAPSE_KEY, next ? '1' : '0');
            } catch (e) {}
        });

        var accordions = sidebar.querySelectorAll('.activity-nav-accordion');
        accordions.forEach(function (acc) {
            var groupName = acc.getAttribute('data-group');
            var trigger = acc.querySelector('.activity-nav-accordion-trigger');
            var hasActive = acc.classList.contains('has-active');
            if (!trigger) return;

            try {
                var saved = localStorage.getItem(GROUP_PREFIX + groupName);
                if (hasActive) {
                    acc.classList.add('is-open');
                    trigger.setAttribute('aria-expanded', 'true');
                } else if (saved === '0') {
                    acc.classList.remove('is-open');
                    trigger.setAttribute('aria-expanded', 'false');
                }
            } catch (e) {}

            trigger.addEventListener('click', function () {
                var open = !acc.classList.contains('is-open');
                acc.classList.toggle('is-open', open);
                trigger.setAttribute('aria-expanded', String(open));
                try {
                    localStorage.setItem(GROUP_PREFIX + groupName, open ? '1' : '0');
                } catch (e) {}
            });
        });
    })();
</script>
