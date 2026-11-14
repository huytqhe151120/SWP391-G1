<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="assessments" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Bài đánh giá sau hoạt động | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261113-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261113-1">
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
                    <h1>Quản lý bài đánh giá</h1>
                    <p>Thiết lập câu hỏi trắc nghiệm, tự luận, theo dõi tiến độ nộp bài và chấm điểm sau hoạt động.</p>
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

            <section class="card activity-table-card" aria-labelledby="assessment-staff-list-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="assessment-staff-list-title">Danh sách bài đánh giá</h2>
                        <p>Mỗi hoạt động ngoại khóa được gắn tối đa một bài đánh giá hoặc bài thu hoạch.</p>
                    </div>
                    <div class="activity-heading-actions">
                        <span class="activity-count"><c:out value="${assessments.size()}"/> bài đánh giá</span>
                        <a class="btn-create-activity" href="${pageContext.request.contextPath}/assessment?action=manage&amp;mode=create">
                            <span class="plus-icon" aria-hidden="true">+</span>
                            Tạo bài đánh giá
                        </a>
                    </div>
                </div>

                <div class="assessment-summary-cards">
                    <div class="assessment-summary-card is-blue">
                        <span class="assessment-summary-label">Tổng bài đánh giá</span>
                        <span class="assessment-summary-value"><c:out value="${assessments.size()}"/></span>
                        <span class="assessment-summary-sub">Thuộc phạm vi quản lý của bạn</span>
                    </div>
                    <div class="assessment-summary-card is-green">
                        <span class="assessment-summary-label">Đã công bố</span>
                        <span class="assessment-summary-value"><c:out value="${publishedCount}"/></span>
                        <span class="assessment-summary-sub">Sinh viên có thể truy cập làm bài</span>
                    </div>
                    <div class="assessment-summary-card is-muted">
                        <span class="assessment-summary-label">Chưa công bố</span>
                        <span class="assessment-summary-value"><c:out value="${draftCount}"/></span>
                        <span class="assessment-summary-sub">Đang soạn thảo câu hỏi / cấu hình</span>
                    </div>
                    <div class="assessment-summary-card ${totalPendingGradingCount > 0 ? 'is-yellow' : 'is-muted'}">
                        <span class="assessment-summary-label">Bài nộp chờ chấm</span>
                        <span class="assessment-summary-value"><c:out value="${totalPendingGradingCount}"/></span>
                        <span class="assessment-summary-sub">Trên tổng số <c:out value="${totalSubmissionsCount}"/> bài sinh viên đã làm</span>
                    </div>
                </div>

                <c:if test="${not empty assessments}">
                    <div class="assessment-filter-bar">
                        <div class="assessment-filter-group">
                            <div class="assessment-filter-field assessment-filter-field--search">
                                <label for="staffAssessmentSearch">Tìm kiếm nhanh</label>
                                <input id="staffAssessmentSearch" type="search"
                                       placeholder="Nhập mã hoạt động, tên hoạt động hoặc tiêu đề bài đánh giá...">
                            </div>
                            <div class="assessment-filter-field">
                                <label for="staffAssessmentStatus">Trạng thái công bố</label>
                                <select id="staffAssessmentStatus">
                                    <option value="ALL">Tất cả trạng thái</option>
                                    <option value="PUBLISHED">Đã công bố</option>
                                    <option value="DRAFT">Chưa công bố</option>
                                    <option value="PENDING_GRADING">Có bài chờ chấm</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </c:if>

                <c:choose>
                    <c:when test="${empty assessments}">
                        <div class="assessment-empty-state">
                            <div class="assessment-empty-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
                            </div>
                            <strong>Chưa có bài đánh giá nào</strong>
                            <p>Hãy tạo bài đánh giá đầu tiên cho hoạt động ngoại khóa để thu thập bài thu hoạch và chấm điểm sinh viên.</p>
                            <a class="button button-primary" href="${pageContext.request.contextPath}/assessment?action=manage&amp;mode=create">Tạo bài đánh giá đầu tiên</a>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="assessment-table-wrap">
                            <table id="staffAssessmentTable">
                                <thead>
                                <tr>
                                    <th>Mã HĐ</th>
                                    <th>Hoạt động ngoại khóa</th>
                                    <th>Tiêu đề bài đánh giá</th>
                                    <th>Thời gian mở / đóng</th>
                                    <th>Trạng thái</th>
                                    <th>Bài nộp</th>
                                    <th>Chờ chấm</th>
                                    <th style="text-align: right;">Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="a" items="${assessments}">
                                    <tr data-assessment-row
                                        data-published="${a.published ? 'PUBLISHED' : 'DRAFT'}"
                                        data-pending="${a.pendingGrading > 0 ? 'YES' : 'NO'}">
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
                                            <div class="assessment-cell-stack">
                                                <span class="assessment-cell-title"><c:out value="${a.activityName}"/></span>
                                            </div>
                                        </td>
                                        <td>
                                            <span class="assessment-cell-title"><c:out value="${a.title}"/></span>
                                        </td>
                                        <td>
                                            <div class="assessment-time-range">
                                                <span class="assessment-time-row">
                                                    <span class="assessment-time-tag">Mở:</span>
                                                    <span><c:out value="${not empty openTimeDisplay[a.assessmentId] ? openTimeDisplay[a.assessmentId] : 'Không giới hạn'}"/></span>
                                                </span>
                                                <span class="assessment-time-row">
                                                    <span class="assessment-time-tag">Đóng:</span>
                                                    <span><c:out value="${not empty closeTimeDisplay[a.assessmentId] ? closeTimeDisplay[a.assessmentId] : 'Không giới hạn'}"/></span>
                                                </span>
                                            </div>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${a.published}">
                                                    <span class="badge status-pill status-green">Đã công bố</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge status-pill status-gray">Chưa công bố</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <span class="assessment-count-badge"><c:out value="${a.totalAttempts}"/></span>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${a.pendingGrading > 0}">
                                                    <span class="assessment-count-badge assessment-count-badge--warning">
                                                        <c:out value="${a.pendingGrading}"/> chờ chấm
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="assessment-cell-sub">0</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: right;">
                                            <div class="assessment-row-actions">
                                                <a class="button button-primary"
                                                   href="${pageContext.request.contextPath}/assessment?action=manage&amp;activityId=${a.activityId}">
                                                    Chi tiết
                                                </a>
                                                <a class="button button-outline"
                                                   href="${pageContext.request.contextPath}/assessment?action=manage&amp;activityId=${a.activityId}&amp;tab=submissions">
                                                    Bài nộp
                                                </a>
                                                <details class="action-menu">
                                                    <summary aria-label="Thao tác khác"><span aria-hidden="true">⋯</span></summary>
                                                    <div class="action-menu__items">
                                                        <a class="action-menu-item action-menu-item--edit"
                                                           href="${pageContext.request.contextPath}/assessment?action=manage&amp;activityId=${a.activityId}&amp;mode=edit#question-builder">
                                                            <span class="action-menu-item__icon" aria-hidden="true">
                                                                <svg viewBox="0 0 24 24"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
                                                            </span>
                                                            Câu hỏi &amp; đáp án
                                                        </a>
                                                        <a class="action-menu-item action-menu-item--edit"
                                                           href="${pageContext.request.contextPath}/assessment?action=manage&amp;activityId=${a.activityId}&amp;mode=edit">
                                                            <span class="action-menu-item__icon" aria-hidden="true">
                                                                <svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"/></svg>
                                                            </span>
                                                            Chỉnh sửa đề đánh giá
                                                        </a>
                                                        <a class="action-menu-item action-menu-item--view"
                                                           href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${a.activityId}">
                                                            <span class="action-menu-item__icon" aria-hidden="true">
                                                                <svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>
                                                            </span>
                                                            Chi tiết hoạt động
                                                        </a>
                                                        <c:if test="${a.totalAttempts == 0}">
                                                            <form method="post" action="${pageContext.request.contextPath}/assessment"
                                                                  onsubmit="return confirm('Bạn chắc chắn muốn xóa bài đánh giá này? Toàn bộ câu hỏi và đáp án bên trong sẽ bị xóa.')">
                                                                <input type="hidden" name="action" value="delete">
                                                                <input type="hidden" name="id" value="${a.assessmentId}">
                                                                <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>">
                                                                <button class="action-menu-item action-menu-item--delete" type="submit">
                                                                    <span class="action-menu-item__icon" aria-hidden="true">
                                                                        <svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg>
                                                                    </span>
                                                                    Xóa bài đánh giá
                                                                </button>
                                                            </form>
                                                        </c:if>
                                                    </div>
                                                </details>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <tr id="staffAssessmentFilterEmpty" hidden>
                                    <td colspan="8" style="text-align: center; padding: 32px 16px; color: #64748b;">
                                        Không tìm thấy bài đánh giá nào khớp với điều kiện lọc.
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
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script>
    (function () {
        var searchInput = document.getElementById('staffAssessmentSearch');
        var statusSelect = document.getElementById('staffAssessmentStatus');
        var rows = document.querySelectorAll('[data-assessment-row]');
        var emptyRow = document.getElementById('staffAssessmentFilterEmpty');
        if (!searchInput || !statusSelect || !rows.length) {
            return;
        }
        function applyFilter() {
            var q = searchInput.value.trim().toLowerCase();
            var st = statusSelect.value;
            var visible = 0;
            rows.forEach(function (row) {
                var textMatch = !q || row.textContent.toLowerCase().indexOf(q) !== -1;
                var statusMatch = st === 'ALL'
                    || (st === 'PUBLISHED' && row.getAttribute('data-published') === 'PUBLISHED')
                    || (st === 'DRAFT' && row.getAttribute('data-published') === 'DRAFT')
                    || (st === 'PENDING_GRADING' && row.getAttribute('data-pending') === 'YES');
                var show = textMatch && statusMatch;
                row.hidden = !show;
                if (show) {
                    visible++;
                }
            });
            if (emptyRow) {
                emptyRow.hidden = visible > 0;
            }
        }
        searchInput.addEventListener('input', applyFilter);
        statusSelect.addEventListener('change', applyFilter);
    })();
</script>
</body>
</html>
