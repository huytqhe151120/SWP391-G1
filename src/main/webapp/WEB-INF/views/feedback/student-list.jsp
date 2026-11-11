<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="activity-feedback" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Feedback của tôi | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261111-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity-feedback.css?v=20261111-7">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page feedback-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> FEEDBACK SINH VIÊN</span>
                    <h1>Feedback của tôi</h1>
                    <p>Chia sẻ cảm nhận về những hoạt động bạn đã tham gia.</p>
                </div>
            </header>

            <c:if test="${not empty successMessage}">
                <div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-error" role="alert"><c:out value="${errorMessage}"/></div>
            </c:if>

            <section class="feedback-card" aria-labelledby="student-feedback-title">
                <div class="feedback-card-heading">
                    <div>
                        <h2 id="student-feedback-title">Hoạt động của bạn</h2>
                        <p>Chỉ hoạt động bạn đã tham dự mới xuất hiện trong danh sách này.</p>
                    </div>
                    <span class="feedback-count-pill"><c:out value="${pendingFeedbackCount}"/> cần feedback</span>
                </div>
                <div class="feedback-tabs" role="tablist" aria-label="Lọc trạng thái feedback">
                    <button class="feedback-tab is-active" type="button" role="tab" aria-selected="true"
                            data-feedback-filter="pending">Cần feedback (<c:out value="${pendingFeedbackCount}"/>)</button>
                    <button class="feedback-tab" type="button" role="tab" aria-selected="false"
                            data-feedback-filter="submitted">Đã gửi (<c:out value="${feedbackActivities.size() - pendingFeedbackCount}"/>)</button>
                </div>
                <div class="feedback-activity-list">
                    <c:forEach var="activity" items="${feedbackActivities}">
                        <article class="feedback-activity-row" data-feedback-state="${activity.submitted ? 'submitted' : 'pending'}">
                            <div class="feedback-activity-copy">
                                <span class="feedback-activity-code"><c:out value="${activity.activityCode}"/></span>
                                <h3><c:out value="${activity.activityName}"/></h3>
                                <p>Kết thúc: <c:out value="${dt:format(activity.endTime)}"/> · đã kết thúc <c:out value="${activity.daysSinceEnd}"/> ngày</p>
                            </div>
                            <div class="feedback-row-actions">
                                <c:choose>
                                    <c:when test="${activity.submitted}">
                                        <a class="button button-secondary" href="${pageContext.request.contextPath}/activity-feedback?action=view&amp;activityId=${activity.activityId}">Xem lại</a>
                                    </c:when>
                                    <c:otherwise>
                                        <a class="button button-primary" href="${pageContext.request.contextPath}/activity-feedback?action=write&amp;activityId=${activity.activityId}">Gửi feedback</a>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </article>
                    </c:forEach>
                    <div class="feedback-empty feedback-tab-empty" data-feedback-empty="pending"
                         ${pendingFeedbackCount > 0 ? 'hidden' : ''}>
                        <span class="feedback-empty-icon" aria-hidden="true">✓</span>
                        <h3>Chưa có hoạt động cần feedback</h3>
                        <p>Hoạt động sẽ xuất hiện tại đây sau khi kết thúc và bạn được ghi nhận đã tham dự.</p>
                    </div>
                    <div class="feedback-empty feedback-tab-empty" data-feedback-empty="submitted" hidden>
                        <h3>Bạn chưa gửi feedback nào</h3>
                    </div>
                </div>
            </section>
            <p class="feedback-privacy-note">Feedback được ẩn danh với người xem báo cáo; hệ thống vẫn ghi nhận trạng thái đã gửi để quản lý nhắc nhở. Bạn vẫn có thể sử dụng các chức năng khác khi chưa gửi feedback.</p>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity-feedback.js?v=20261111-11" defer></script>
</body>
</html>
