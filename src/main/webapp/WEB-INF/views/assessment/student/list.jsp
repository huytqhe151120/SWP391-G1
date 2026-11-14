<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeNav" value="assessments" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Bài đánh giá của tôi | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261113-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261113-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/assessment.css?v=20261113-1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-detail-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> KHẢO THÍ &amp; BÀI THU HOẠCH</span>
                    <h1>Bài đánh giá của tôi</h1>
                    <p>Thực hiện bài đánh giá, bài thu hoạch sau khi tham dự hoạt động ngoại khóa và tra cứu điểm số.</p>
                </div>
            </header>

            <c:if test="${not empty sessionScope.successMessage}">
                <div class="alert alert-success activity-alert" role="status"><c:out value="${sessionScope.successMessage}"/></div>
                <c:remove var="successMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-error activity-alert" role="alert"><c:out value="${sessionScope.errorMessage}"/></div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>

            <section class="card activity-table-card" aria-labelledby="student-assessment-list-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="student-assessment-list-title">Danh sách bài đánh giá dành cho bạn</h2>
                        <p>Chỉ các bài đánh giá đã công bố của hoạt động bạn tham dự mới xuất hiện tại đây.</p>
                    </div>
                    <span class="activity-count"><c:out value="${assessments.size()}"/> bài đánh giá</span>
                </div>

                <div class="assessment-summary-cards">
                    <div class="assessment-summary-card is-blue">
                        <span class="assessment-summary-label">Tổng bài đánh giá</span>
                        <span class="assessment-summary-value"><c:out value="${assessments.size()}"/></span>
                        <span class="assessment-summary-sub">Hoạt động bạn đã tham dự</span>
                    </div>
                    <div class="assessment-summary-card ${pendingTakeCount > 0 ? 'is-yellow' : 'is-muted'}">
                        <span class="assessment-summary-label">Chưa nộp bài</span>
                        <span class="assessment-summary-value"><c:out value="${pendingTakeCount}"/></span>
                        <span class="assessment-summary-sub">Chưa làm hoặc đang lưu nháp</span>
                    </div>
                    <div class="assessment-summary-card is-muted">
                        <span class="assessment-summary-label">Đang chờ chấm</span>
                        <span class="assessment-summary-value"><c:out value="${waitingGradeCount}"/></span>
                        <span class="assessment-summary-sub">Chờ cán bộ chấm câu tự luận</span>
                    </div>
                    <div class="assessment-summary-card is-green">
                        <span class="assessment-summary-label">Đã có điểm</span>
                        <span class="assessment-summary-value"><c:out value="${completedCount}"/></span>
                        <span class="assessment-summary-sub">Đã hoàn tất chấm điểm</span>
                    </div>
                </div>

                <c:if test="${not empty assessments}">
                    <div class="assessment-tabs" role="tablist" aria-label="Lọc bài đánh giá">
                        <button type="button" class="assessment-tab is-active" data-student-tab="ALL">
                            Tất cả (<c:out value="${assessments.size()}"/>)
                        </button>
                        <button type="button" class="assessment-tab" data-student-tab="PENDING">
                            Cần làm (<c:out value="${pendingTakeCount}"/>)
                        </button>
                        <button type="button" class="assessment-tab" data-student-tab="SUBMITTED_OR_DONE">
                            Đã nộp / Đã có điểm (<c:out value="${waitingGradeCount + completedCount}"/>)
                        </button>
                    </div>
                </c:if>

                <c:choose>
                    <c:when test="${empty assessments}">
                        <div class="assessment-empty-state">
                            <div class="assessment-empty-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
                            </div>
                            <strong>Hiện chưa có bài đánh giá nào dành cho bạn</strong>
                            <p>Sau khi bạn tham dự hoạt động ngoại khóa và ban tổ chức công bố đề thu hoạch, bài đánh giá sẽ hiển thị tại đây.</p>
                            <a class="button button-outline" href="${pageContext.request.contextPath}/activities">Xem danh sách hoạt động</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="assessment-table-wrap">
                            <table>
                                <thead>
                                <tr>
                                    <th>Mã HĐ</th>
                                    <th>Hoạt động ngoại khóa</th>
                                    <th>Tiêu đề bài đánh giá</th>
                                    <th>Thời gian mở / Hạn nộp</th>
                                    <th>Trạng thái</th>
                                    <th>Điểm đạt được</th>
                                    <th style="text-align: right;">Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="a" items="${assessments}">
                                    <c:set var="isDoneOrSubmitted" value="${a.attemptStatus == 'SUBMITTED' or a.attemptStatus == 'COMPLETED'}"/>
                                    <tr data-student-assessment-row
                                        data-bucket="${isDoneOrSubmitted ? 'SUBMITTED_OR_DONE' : 'PENDING'}">
                                        <td class="col-code">
                                            <c:choose>
                                                <c:when test="${not empty a.activityCode}">
                                                    <span class="activity-code"><c:out value="${a.activityCode}"/></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="activity-code">#<c:out value="${a.activityId}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <span class="assessment-cell-title"><c:out value="${a.activityName}"/></span>
                                        </td>
                                        <td>
                                            <span class="assessment-cell-title"><c:out value="${a.title}"/></span>
                                        </td>
                                        <td>
                                            <div class="assessment-time-range">
                                                <span class="assessment-time-row">
                                                    <span class="assessment-time-tag">Mở:</span>
                                                    <span><c:out value="${not empty openTimeDisplay[a.assessmentId] ? openTimeDisplay[a.assessmentId] : 'Ngay khi kết thúc HĐ'}"/></span>
                                                </span>
                                                <span class="assessment-time-row">
                                                    <span class="assessment-time-tag">Hạn:</span>
                                                    <span><c:out value="${not empty closeTimeDisplay[a.assessmentId] ? closeTimeDisplay[a.assessmentId] : 'Không giới hạn'}"/></span>
                                                </span>
                                            </div>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${a.attemptStatus == 'COMPLETED'}">
                                                    <span class="badge status-pill status-green">Đã có điểm</span>
                                                </c:when>
                                                <c:when test="${a.attemptStatus == 'SUBMITTED'}">
                                                    <span class="badge status-pill status-yellow">Chờ chấm điểm</span>
                                                </c:when>
                                                <c:when test="${a.attemptStatus == 'IN_PROGRESS' and a.canTake}">
                                                    <span class="badge status-pill status-blue">Đang làm dở</span>
                                                </c:when>
                                                <c:when test="${expiredMap[a.assessmentId]}">
                                                    <span class="badge status-pill status-red">Đã quá hạn</span>
                                                </c:when>
                                                <c:when test="${notOpenYetMap[a.assessmentId]}">
                                                    <span class="badge status-pill status-gray">Chưa tới giờ mở</span>
                                                </c:when>
                                                <c:when test="${not a.canTake}">
                                                    <span class="badge status-pill status-gray">Chưa mở làm bài</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge status-pill status-yellow">Chưa làm bài</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${a.finalScore != null}">
                                                    <span class="assessment-score-pill assessment-score-pill--green">
                                                        <fmt:formatNumber value="${a.finalScore}" minFractionDigits="1" maxFractionDigits="2"/> / 10
                                                    </span>
                                                </c:when>
                                                <c:when test="${a.attemptStatus == 'SUBMITTED'}">
                                                    <span class="assessment-score-pill assessment-score-pill--muted">Đang chấm</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="assessment-cell-sub">—</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: right;">
                                            <div class="assessment-row-actions">
                                                <c:choose>
                                                    <c:when test="${isDoneOrSubmitted}">
                                                        <a class="button button-outline"
                                                           href="${pageContext.request.contextPath}/assessment?action=result&amp;assessmentId=${a.assessmentId}">
                                                            Xem kết quả
                                                        </a>
                                                    </c:when>
                                                    <c:when test="${a.canTake}">
                                                        <a class="button button-primary"
                                                           href="${pageContext.request.contextPath}/assessment?action=take&amp;assessmentId=${a.assessmentId}">
                                                            ${a.attemptStatus == 'IN_PROGRESS' ? 'Tiếp tục làm' : 'Làm bài'}
                                                        </a>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <a class="button button-outline"
                                                           href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${a.activityId}">
                                                            Chi tiết HĐ
                                                        </a>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <tr id="studentAssessmentTabEmpty" hidden>
                                    <td colspan="7" style="text-align: center; padding: 32px 16px; color: #64748b;">
                                        Không có bài đánh giá nào trong nhóm này.
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script>
    (function () {
        var tabs = document.querySelectorAll('[data-student-tab]');
        var rows = document.querySelectorAll('[data-student-assessment-row]');
        var emptyRow = document.getElementById('studentAssessmentTabEmpty');
        if (!tabs.length || !rows.length) {
            return;
        }
        tabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                var target = tab.getAttribute('data-student-tab');
                tabs.forEach(function (t) {
                    t.classList.toggle('is-active', t === tab);
                });
                var visible = 0;
                rows.forEach(function (row) {
                    var show = target === 'ALL' || row.getAttribute('data-bucket') === target;
                    row.hidden = !show;
                    if (show) {
                        visible++;
                    }
                });
                if (emptyRow) {
                    emptyRow.hidden = visible > 0;
                }
            });
        });
    })();
</script>
</body>
</html>
