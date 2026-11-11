<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.swp391.g1.dto.CurrentUser" %>
<%@ page import="com.swp391.g1.util.AuthContext" %>
<%
    CurrentUser hdrUser = AuthContext.getCurrentUser(request);
    String navType = hdrUser == null ? "" : hdrUser.getType();
    String ctx = request.getContextPath();

    Object navActiveAttr = request.getAttribute("activeNav");
    String navActive = navActiveAttr == null ? "" : navActiveAttr.toString();
    String reqUri = request.getRequestURI() == null ? "" : request.getRequestURI();
    String actionParam = request.getParameter("action") == null ? "" : request.getParameter("action");

    if (navActive.isEmpty()) {
        if (reqUri.contains("/qa-management") || reqUri.contains("/answer-question")) navActive = "qa-management";
        else if (reqUri.contains("/organizer-inbox") || reqUri.contains("/organizer-chat")) navActive = "organizer-inbox";
        else if (reqUri.contains("/student-qa")) navActive = "student-qa";
        else if (reqUri.contains("/student-inbox") || reqUri.contains("/student-chat")) navActive = "student-inbox";
        else if (reqUri.contains("/attendance")) navActive = "attendance";
        else if (reqUri.contains("/registration-forms")) navActive = "registration-forms";
        else if (reqUri.contains("/notification")) navActive = "notifications";
    }

    String breadcrumbGroup = "";
    String breadcrumbCurrent = "Tổng quan";
    String searchAction = ctx + "/activities";

    if ("activities".equals(navActive)) {
        breadcrumbGroup = "Quản lý Hoạt động";
        if ("create".equals(actionParam)) breadcrumbCurrent = "Tạo hoạt động";
        else if ("edit".equals(actionParam)) breadcrumbCurrent = "Chỉnh sửa hoạt động";
        else if ("detail".equals(actionParam)) breadcrumbCurrent = "Chi tiết hoạt động";
        else breadcrumbCurrent = "Hoạt động ngoại khóa";
    } else if ("registration-forms".equals(navActive)) {
        breadcrumbGroup = "Quản lý Hoạt động";
        if ("create".equals(actionParam)) breadcrumbCurrent = "Tạo đợt đăng ký";
        else if ("edit".equals(actionParam)) breadcrumbCurrent = "Chỉnh sửa đợt đăng ký";
        else if ("view".equals(actionParam)) breadcrumbCurrent = "Chi tiết đợt đăng ký";
        else if ("application".equals(actionParam) || "myApplication".equals(actionParam)) breadcrumbCurrent = "Chi tiết đơn đăng ký";
        else if ("myApplications".equals(actionParam) || "STUDENT".equals(navType)) breadcrumbCurrent = "Đơn đăng ký";
        else breadcrumbCurrent = "Đăng ký tham gia";
    } else if ("attendance".equals(navActive)) {
        breadcrumbGroup = "Quản lý Hoạt động";
        breadcrumbCurrent = "Điểm danh sinh viên";
    } else if ("activity-feedback".equals(navActive)) {
        breadcrumbGroup = "Hoạt động ngoại khóa";
        breadcrumbCurrent = "STUDENT".equals(navType) ? "Feedback của tôi" : "Feedback hoạt động";
    } else if ("partner-invitations".equals(navActive)) {
        breadcrumbGroup = "Quản lý Hoạt động";
        breadcrumbCurrent = "Lời mời hợp tác";
    } else if ("students".equals(navActive)) {
        breadcrumbGroup = "Quản lý Nhân sự";
        breadcrumbCurrent = "Sinh viên";
        searchAction = ctx + "/student";
    } else if ("staff".equals(navActive)) {
        breadcrumbGroup = "Quản lý Nhân sự";
        breadcrumbCurrent = "Nhân viên";
        searchAction = ctx + "/staff";
    } else if ("accounts".equals(navActive)) {
        breadcrumbGroup = "Quản lý Nhân sự";
        breadcrumbCurrent = "Tài khoản";
        searchAction = ctx + "/accounts";
    } else if ("partner-companies".equals(navActive)) {
        breadcrumbGroup = "Đối tác";
        breadcrumbCurrent = "Công ty đối tác";
        searchAction = ctx + "/partner-companies";
    } else if ("partner-staff".equals(navActive)) {
        breadcrumbGroup = "Đối tác";
        breadcrumbCurrent = "Nhân viên đối tác";
        searchAction = ctx + "/partner-staffs";
    } else if ("qa-management".equals(navActive)) {
        breadcrumbGroup = "Hỗ trợ";
        breadcrumbCurrent = "Quản lý Hỏi & Đáp";
    } else if ("organizer-inbox".equals(navActive)) {
        breadcrumbGroup = "Hỗ trợ";
        breadcrumbCurrent = "Hộp thư hỗ trợ";
    } else if ("student-qa".equals(navActive)) {
        breadcrumbGroup = "Hỗ trợ sinh viên";
        breadcrumbCurrent = "Hỏi & Đáp";
    } else if ("student-inbox".equals(navActive)) {
        breadcrumbGroup = "Hỗ trợ sinh viên";
        breadcrumbCurrent = "Hộp thư của tôi";
    } else if ("notifications".equals(navActive)) {
        breadcrumbGroup = "Tiện ích";
        breadcrumbCurrent = "Thông báo";
    }

    String currentKeyword = request.getParameter("keyword") == null ? "" : request.getParameter("keyword");
    pageContext.setAttribute("hdrCurrentKeyword", currentKeyword);
%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/header.css?v=20261107-2">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity-feedback.css?v=20261111-9">
<header class="activity-app-header" id="appHeader">
    <!-- Left Zone: Breadcrumb & Navigation Context -->
    <div class="activity-header-left">
        <nav class="activity-header-breadcrumb" aria-label="Breadcrumb">
            <ol class="activity-breadcrumb-list">
                <li class="activity-breadcrumb-item">
                    <a class="activity-breadcrumb-link" href="<%= ctx %>/home">Trang chủ</a>
                </li>
                <% if (!breadcrumbGroup.isEmpty()) { %>
                <li class="activity-breadcrumb-item activity-breadcrumb-group">
                    <span class="activity-breadcrumb-sep" aria-hidden="true">/</span>
                    <span class="activity-breadcrumb-muted"><%= breadcrumbGroup %></span>
                </li>
                <% } %>
                <li class="activity-breadcrumb-item">
                    <span class="activity-breadcrumb-sep" aria-hidden="true">/</span>
                    <strong class="activity-breadcrumb-current" aria-current="page"><%= breadcrumbCurrent %></strong>
                </li>
            </ol>
        </nav>
    </div>

    <!-- Right Zone: Global Search Bar + Notification Bell -->
    <div class="activity-header-right">
        <div class="activity-header-search" id="headerSearchContainer">
            <form class="activity-global-search" action="<%= searchAction %>" method="get" role="search">
                <svg class="activity-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <input type="search"
                       id="globalSearchInput"
                       name="keyword"
                       class="activity-global-search-input"
                       value="<c:out value='${hdrCurrentKeyword}'/>"
                       placeholder="Tìm kiếm nhanh hoạt động"
                       autocomplete="off"
                       aria-label="Tìm kiếm toàn hệ thống">
                <kbd class="activity-search-kbd" aria-hidden="true">Ctrl + K</kbd>
            </form>
        </div>

        <div class="activity-header-account">
            <button type="button" id="headerMobileSearchBtn" class="header-search-mobile-toggle"
                    aria-label="Mở tìm kiếm" title="Tìm kiếm (Ctrl + K)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
            </button>

            <% if (!navType.isEmpty()) { %>
            <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/notification.css?v=20261111-3">
            <div class="notif-wrap">
                <button type="button" id="notifBell" class="notif-bell" aria-label="Thông báo" aria-haspopup="true" aria-expanded="false" title="Thông báo">
                    <svg class="ui-icon notif-bell-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                    </svg>
                    <span id="notifBadge" class="notif-badge" hidden>0</span>
                </button>
                <div id="notifPanel" class="notif-panel" hidden>
                    <div class="notif-head">
                        <strong class="notif-head-title">
                            <svg class="ui-icon notif-head-icon" viewBox="0 0 24 24" aria-hidden="true">
                                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                                <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                            </svg>
                            Thông báo
                        </strong>
                        <button type="button" id="notifReadAll" class="notif-link">Đánh dấu tất cả đã đọc</button>
                    </div>
                    <div id="notifDropdownList" class="notif-dropdown-list"></div>
                    <div class="notif-foot"><a class="notif-link" href="${pageContext.request.contextPath}/notifications">Xem tất cả thông báo</a></div>
                </div>
            </div>
            <script>window.APP_CTX = '${pageContext.request.contextPath}';</script>
            <script src="${pageContext.request.contextPath}/assets/js/notification.js?v=20261111-3" defer></script>
            <% } %>
        </div>
    </div>
</header>
<script>
    (function () {
        var header = document.getElementById('appHeader');
        var searchInput = document.getElementById('globalSearchInput');
        var mobileSearchBtn = document.getElementById('headerMobileSearchBtn');
        if (!header) return;

        document.addEventListener('keydown', function (e) {
            if ((e.ctrlKey || e.metaKey) && e.key && e.key.toLowerCase() === 'k') {
                if (searchInput) {
                    e.preventDefault();
                    header.classList.add('is-search-open');
                    searchInput.focus();
                    searchInput.select();
                }
            }
        });

        if (mobileSearchBtn && searchInput) {
            mobileSearchBtn.addEventListener('click', function () {
                var open = header.classList.toggle('is-search-open');
                if (open) searchInput.focus();
            });
        }

    })();
</script>
