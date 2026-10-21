<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="activity-app-header">
    <a class="activity-brand" href="${pageContext.request.contextPath}/home">
        <img class="activity-brand-logo" src="${pageContext.request.contextPath}/assets/images/logo-negative.svg" alt="" width="48" height="48">
        <span class="activity-brand-copy">
            <strong>G1 UNIVERSITY / EAMS</strong>
            <span>Hệ thống quản lý hoạt động ngoại khóa</span>
        </span>
    </a>
    <div class="activity-header-account">
        <c:if test="${not empty sessionScope['com.swp391.g1.auth.CURRENT_USER']}">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/notification.css">
        <div class="notif-wrap">
            <button type="button" id="notifBell" class="notif-bell" aria-label="Thông báo" aria-haspopup="true" aria-expanded="false">
                🔔<span id="notifBadge" class="notif-badge" hidden>0</span>
            </button>
            <div id="notifPanel" class="notif-panel" hidden>
                <div class="notif-head"><strong>Thông báo</strong>
                    <button type="button" id="notifReadAll" class="notif-link">Đánh dấu tất cả đã đọc</button></div>
                <div id="notifDropdownList" class="notif-dropdown-list"></div>
                <div class="notif-foot"><a class="notif-link" href="${pageContext.request.contextPath}/notifications">Xem tất cả</a></div>
            </div>
        </div>
        <script>window.APP_CTX = '${pageContext.request.contextPath}';</script>
        <script src="${pageContext.request.contextPath}/assets/js/notification.js" defer></script>
        </c:if>
        <span class="activity-header-user">${empty sessionScope['com.swp391.g1.auth.CURRENT_USER'] ? 'Khách' : sessionScope['com.swp391.g1.auth.CURRENT_USER'].username}</span>
        <c:choose>
            <c:when test="${not empty sessionScope['com.swp391.g1.auth.CURRENT_USER']}">
                <form method="post" action="${pageContext.request.contextPath}/logout">
                    <button class="activity-header-action" type="submit">Đăng xuất</button>
                </form>
            </c:when>
            <c:otherwise>
                <a class="activity-header-action" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
            </c:otherwise>
        </c:choose>
    </div>
</header>
