<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="activity-feedback" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý feedback | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261111-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity-feedback.css?v=20261111-10">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page feedback-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ FEEDBACK</span>
                    <h1>Feedback hoạt động</h1>
                    <p>Theo dõi mức độ phản hồi và đánh giá của sinh viên sau mỗi hoạt động.</p>
                </div>
            </header>
            <section class="feedback-card feedback-management-card">
                <div class="feedback-card-heading feedback-filter-heading">
                    <div>
                        <h2>Bộ lọc hoạt động</h2>
                        <p>Lọc theo học kỳ, loại hoạt động, đơn vị và tình trạng phản hồi.</p>
                    </div>
                    <button class="button button-secondary feedback-filter-toggle" id="feedbackFilterToggle"
                            type="button" aria-expanded="true" aria-controls="feedbackFilters">
                        <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16"/><path d="M7 12h10"/><path d="M10 17h4"/></svg>
                        <span>Thu gọn bộ lọc</span>
                    </button>
                </div>
                <div id="feedbackFilters">
                    <div class="feedback-filter-grid">
                        <label>Học kỳ
                            <select id="feedbackSemesterFilter">
                                <option value="">Tất cả</option>
                                <c:forEach var="semester" items="${feedbackSemesters}">
                                    <option value="<c:out value='${semester.name}'/>"><c:out value="${semester.name}"/></option>
                                </c:forEach>
                            </select>
                        </label>
                        <label>Loại hoạt động
                            <select id="feedbackTypeFilter">
                                <option value="">Tất cả</option>
                                <c:forEach var="summary" items="${feedbackSummaries}">
                                    <c:if test="${not empty summary.activityTypeName}">
                                        <option value="<c:out value='${summary.activityTypeName}'/>"><c:out value="${summary.activityTypeName}"/></option>
                                    </c:if>
                                </c:forEach>
                            </select>
                        </label>
                        <label>Đơn vị phụ trách
                            <select id="feedbackDepartmentFilter">
                                <option value="">Tất cả</option>
                                <c:forEach var="department" items="${feedbackDepartments}">
                                    <option value="<c:out value='${department.name}'/>"><c:out value="${department.name}"/></option>
                                </c:forEach>
                            </select>
                        </label>
                        <label>Tình trạng phản hồi
                            <select id="feedbackStatusFilter">
                                <option value="">Tất cả</option>
                                <option value="pending">Còn sinh viên chưa gửi</option>
                                <option value="done">Đã đủ phản hồi</option>
                            </select>
                        </label>
                    </div>
                    <div class="feedback-filter-actions">
                        <button class="button button-primary" id="feedbackApplyFilters" type="button">Tìm kiếm &amp; lọc</button>
                        <button class="button button-secondary" id="feedbackClearFilters" type="button">Xóa bộ lọc</button>
                    </div>
                </div>
                <div class="feedback-table-toolbar">
                    <label for="feedbackSort">Sắp xếp theo
                        <select id="feedbackSort">
                            <option value="default">Mặc định</option>
                            <option value="rating-desc">Điểm cao nhất</option>
                            <option value="rating-asc">Điểm thấp nhất</option>
                            <option value="response-desc">Tỉ lệ phản hồi cao nhất</option>
                            <option value="response-asc">Tỉ lệ phản hồi thấp nhất</option>
                        </select>
                    </label>
                </div>
                <div class="table-wrap">
                    <table class="feedback-management-table">
                        <thead><tr><th>Mã</th><th>Hoạt động</th><th>Kết thúc</th><th>Đã phản hồi</th><th>Điểm trung bình</th><th>Thao tác</th></tr></thead>
                        <tbody>
                        <c:forEach var="summary" items="${feedbackSummaries}">
                            <tr data-semester="<c:out value='${summary.semesterName}'/>"
                                data-type="<c:out value='${summary.activityTypeName}'/>"
                                data-department="<c:out value='${summary.departmentName}'/>"
                                data-status="${summary.pendingCount > 0 ? 'pending' : 'done'}"
                                data-report-href="${pageContext.request.contextPath}/activity-feedback?action=report&amp;activityId=${summary.activityId}"
                                data-average="${summary.submittedCount == 0 ? -1 : summary.averageRating}"
                                data-response-rate="${summary.participantCount == 0 ? 0 : summary.submittedCount * 100.0 / summary.participantCount}"
                                tabindex="0" aria-label="Xem kết quả feedback: <c:out value='${summary.activityName}'/>">
                                <td><span class="feedback-activity-code"><c:out value="${summary.activityCode}"/></span></td>
                                <td><strong><c:out value="${summary.activityName}"/></strong><small><c:out value="${summary.activityTypeName}"/> · <c:out value="${summary.departmentName}"/></small></td>
                                <td><c:out value="${dt:format(summary.endTime)}"/></td>
                                <td>
                                    <div class="feedback-progress-cell"><span class="feedback-progress"><i style="width:${summary.participantCount == 0 ? 0 : summary.submittedCount * 100 / summary.participantCount}%"></i></span>
                                        <span><c:out value="${summary.submittedCount}"/> / <c:out value="${summary.participantCount}"/></span></div>
                                </td>
                                <td><span class="feedback-average">★ <c:choose><c:when test="${summary.submittedCount == 0}">—</c:when><c:otherwise><fmt:formatNumber value="${summary.averageRating}" minFractionDigits="2" maxFractionDigits="2"/></c:otherwise></c:choose></span></td>
                                <td><a class="button button-secondary button-sm" href="${pageContext.request.contextPath}/activity-feedback?action=report&amp;activityId=${summary.activityId}">Xem kết quả</a></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty feedbackSummaries}">
                            <tr><td class="feedback-table-empty" colspan="6">Chưa có hoạt động đã kết thúc trong phạm vi quản lý.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </div>
                <div class="activity-pagination feedback-management-pagination">
                    <span class="activity-pagination-summary" id="feedbackPaginationSummary"></span>
                    <nav class="activity-pagination-nav" id="feedbackPagination"
                         aria-label="Phân trang hoạt động feedback"></nav>
                </div>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity-feedback.js?v=20261111-11" defer></script>
</body>
</html>
