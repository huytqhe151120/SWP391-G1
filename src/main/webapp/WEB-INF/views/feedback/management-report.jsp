<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="activity-feedback" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Báo cáo feedback | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261111-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity-feedback.css?v=20261111-8">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page feedback-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> BÁO CÁO FEEDBACK</span>
                    <h1><c:out value="${feedbackReport.summary.activityName}"/></h1>
                    <p><c:out value="${feedbackReport.summary.activityCode}"/> · Kết thúc <c:out value="${dt:format(feedbackReport.summary.endTime)}"/></p>
                </div>
            </header>
            <div class="feedback-back-row">
                <a class="button button-secondary feedback-back-link" href="${pageContext.request.contextPath}/activity-feedback">← Danh sách feedback hoạt động</a>
            </div>
            <c:if test="${not empty successMessage}">
                <div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-error" role="alert"><c:out value="${errorMessage}"/></div>
            </c:if>

            <section class="feedback-kpi-grid">
                <article class="feedback-kpi"><span>Điểm đánh giá chung</span><strong><c:out value="${feedbackReport.summary.submittedCount == 0 ? '—' : feedbackReport.summary.averageRating}"/> <small>/ 5</small></strong><p><c:out value="${feedbackReport.summary.submittedCount}"/> phản hồi</p></article>
                <article class="feedback-kpi"><span>Đã phản hồi</span><strong><c:out value="${feedbackReport.summary.submittedCount}"/> <small>/ <c:out value="${feedbackReport.summary.participantCount}"/></small></strong><p>Sinh viên đã tham dự</p></article>
                <article class="feedback-kpi"><span>Chưa phản hồi</span><strong><c:out value="${feedbackReport.summary.pendingCount}"/></strong><p>Đã tham dự, chưa gửi</p></article>
                <article class="feedback-kpi"><span>Có ghi chú</span><strong><c:out value="${feedbackReport.noteCount}"/></strong><p>Trong số phản hồi</p></article>
            </section>

            <div class="feedback-report-grid">
                <section class="feedback-card">
                    <h2>Điểm trung bình theo mục</h2>
                    <div class="feedback-metric-row"><span>Đánh giá chung</span><span class="feedback-metric-track"><i style="width:${feedbackReport.summary.averageRating * 20}%"></i></span><strong><c:out value="${feedbackReport.summary.averageRating}"/></strong></div>
                    <div class="feedback-metric-row"><span>Nội dung hoạt động</span><span class="feedback-metric-track"><i style="width:${feedbackReport.averageContentRating * 20}%"></i></span><strong><c:out value="${feedbackReport.averageContentRating}"/></strong></div>
                    <div class="feedback-metric-row"><span>Công tác tổ chức</span><span class="feedback-metric-track"><i style="width:${feedbackReport.averageOrganizationRating * 20}%"></i></span><strong><c:out value="${feedbackReport.averageOrganizationRating}"/></strong></div>
                    <div class="feedback-metric-row"><span>Địa điểm, thời gian</span><span class="feedback-metric-track"><i style="width:${feedbackReport.averageVenueRating * 20}%"></i></span><strong><c:out value="${feedbackReport.averageVenueRating}"/></strong></div>
                </section>
                <section class="feedback-card">
                    <h2>Phân bố đánh giá chung</h2>
                    <c:forEach begin="1" end="5" var="rating">
                        <c:set var="distributionCount" value="${feedbackReport.ratingDistribution[5 - rating]}"/>
                        <div class="feedback-metric-row"><span><c:out value="${6 - rating}"/> sao</span><span class="feedback-metric-track"><i style="width:${feedbackReport.summary.submittedCount == 0 ? 0 : distributionCount * 100 / feedbackReport.summary.submittedCount}%"></i></span><strong><c:out value="${distributionCount}"/></strong></div>
                    </c:forEach>
                </section>
            </div>

            <div class="feedback-report-grid feedback-answer-grid">
                <section class="feedback-card">
                    <div class="feedback-card-heading"><div><h2>Câu trả lời tự luận</h2><p>Phản hồi được ẩn danh với người xem báo cáo.</p></div></div>
                    <div class="feedback-tabs feedback-question-tabs" role="tablist" aria-label="Chọn câu trả lời">
                        <button class="feedback-tab is-active" type="button" data-answer-tab="bestPart">Câu 1</button>
                        <button class="feedback-tab" type="button" data-answer-tab="improvement">Câu 2</button>
                        <button class="feedback-tab" type="button" data-answer-tab="favorite">Câu 3</button>
                        <button class="feedback-tab" type="button" data-answer-tab="suggestions">Câu 4</button>
                        <button class="feedback-tab" type="button" data-answer-tab="note">Ghi chú</button>
                    </div>
                    <div class="feedback-answer-question" data-question-label="bestPart">Điều bạn hài lòng nhất ở hoạt động này là gì?</div>
                    <div class="feedback-answers">
                        <c:forEach var="answer" items="${feedbackReport.answers}">
                            <article class="feedback-answer" data-answer-bestpart="<c:out value='${answer.bestPart}'/>"
                                     data-answer-improvement="<c:out value='${answer.improvement}'/>"
                                     data-answer-favorite="<c:out value='${answer.favorite}'/>"
                                     data-answer-suggestions="<c:out value='${answer.suggestions}'/>"
                                     data-answer-note="<c:out value='${answer.note}'/>">
                                <p class="feedback-answer-body"><c:out value="${answer.bestPart}"/></p>
                            </article>
                        </c:forEach>
                        <div class="feedback-empty feedback-answer-empty" hidden><p>Không có câu trả lời cho mục này.</p></div>
                        <c:if test="${empty feedbackReport.answers}"><div class="feedback-empty"><p>Chưa có phản hồi được gửi.</p></div></c:if>
                    </div>
                    <nav class="activity-pagination-nav feedback-answer-pagination" id="feedbackAnswerPagination"
                         aria-label="Phân trang câu trả lời"></nav>
                </section>
                <aside class="feedback-card feedback-pending-card">
                    <h2>Chưa gửi feedback (<c:out value="${feedbackReport.summary.pendingCount}"/>)</h2>
                    <p>Sinh viên đã tham dự nhưng chưa gửi phản hồi.</p>
                    <c:if test="${not empty feedbackReport.pendingStudents}">
                        <label class="feedback-pending-search" for="feedbackPendingSearch">Tìm sinh viên
                            <input id="feedbackPendingSearch" type="search" placeholder="Mã hoặc tên sinh viên..."
                                   autocomplete="off">
                        </label>
                    </c:if>
                    <ul>
                        <c:forEach var="student" items="${feedbackReport.pendingStudents}">
                            <li data-pending-student data-search="<c:out value='${student.studentCode}'/> <c:out value='${student.studentName}'/>">
                                <span><c:out value="${student.studentCode}"/></span><strong><c:out value="${student.studentName}"/></strong>
                            </li>
                        </c:forEach>
                        <li class="feedback-no-search-results" id="feedbackNoSearchResults" hidden>Không tìm thấy sinh viên phù hợp.</li>
                        <c:if test="${empty feedbackReport.pendingStudents}"><li class="feedback-no-pending">Tất cả sinh viên đã gửi feedback.</li></c:if>
                    </ul>
                    <c:if test="${not empty feedbackReport.pendingStudents}">
                        <form class="feedback-remind-form" action="${pageContext.request.contextPath}/activity-feedback" method="post">
                            <input type="hidden" name="action" value="remind">
                            <input type="hidden" name="activityId" value="${feedbackReport.summary.activityId}">
                            <button class="button button-secondary" type="submit">Gửi nhắc nhở</button>
                        </form>
                    </c:if>
                </aside>
            </div>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity-feedback.js?v=20261111-11" defer></script>
</body>
</html>
