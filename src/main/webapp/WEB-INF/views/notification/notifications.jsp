<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<% request.setAttribute("activeNav", "notifications"); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Thông báo | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <div class="activity-app-main">
            <main class="page">
                <h1>Thông báo</h1>
                <div class="notif-toolbar">
                    <span id="notifFilter">
                        <button type="button" class="active" data-filter="all">Tất cả</button>
                        <button type="button" data-filter="unread">Chưa đọc</button>
                        <button type="button" data-filter="read">Đã đọc</button>
                    </span>
                    <span class="spacer"></span>
                    <button type="button" id="notifPageReadAll">Đánh dấu tất cả đã đọc</button>
                </div>
                <div id="notifList" class="notif-page-list"></div>
                <div id="notifPager" class="notif-pager"></div>
            </main>
        </div>
    </div>
</div>
</body>
</html>