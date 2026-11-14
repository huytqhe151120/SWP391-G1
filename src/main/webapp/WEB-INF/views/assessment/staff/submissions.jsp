<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="activeNav" value="assessments" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh sách bài nộp | EAMS</title>
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
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> CHẤM ĐIỂM BÀI THU HOẠCH</span>
                    <h1>Danh sách bài nộp của sinh viên</h1>
                    <p>
                        <c:if test="${not empty activity}">
                            <span class="activity-code"><c:out value="${activity.code}"/></span>
                            <c:out value="${activity.name}"/>
                        </c:if>
                        <c:if test="${not empty assessment}">
                            · <c:out value="${assessment.title}"/>
                        </c:if>
                    </p>
                </div>
            </header>

            <div class="assessment-page-nav">
                <div class="assessment-page-nav-links">
                    <a class="button button-back" href="${pageContext.request.contextPath}/assessment?action=manage&amp;activityId=${activityId}">
                        <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                        Quay lại quản lý bài đánh giá
                    </a>
                    <a class="button button-outline" href="${pageContext.request.contextPath}/assessment?action=list">
                        Tất cả bài đánh giá
                    </a>
                </div>
            </div>

            <c:if test="${not empty sessionScope.successMessage}">
                <div class="alert alert-success activity-alert" role="status"><c:out value="${sessionScope.successMessage}"/></div>
                <c:remove var="successMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-error activity-alert" role="alert"><c:out value="${sessionScope.errorMessage}"/></div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>

            <section class="card activity-table-card">
                <div class="activity-table-heading">
                    <div>
                        <h2>Tiến độ làm bài &amp; chấm điểm</h2>
                        <p>Các câu trắc nghiệm được chấm điểm tự động ngay khi nộp; câu tự luận cần cán bộ chấm điểm để hoàn tất.</p>
                    </div>
                    <span class="activity-count"><c:out value="${submissions.size()}"/> lượt làm bài</span>
                </div>

                <div class="assessment-summary-cards">
                    <div class="assessment-summary-card is-blue">
                        <span class="assessment-summary-label">Tổng lượt làm bài</span>
                        <span class="assessment-summary-value"><c:out value="${submissions.size()}"/></span>
                        <span class="assessment-summary-sub">Tất cả trạng thái ghi nhận</span>
                    </div>
                    <div class="assessment-summary-card is-green">
                        <span class="assessment-summary-label">Đã hoàn tất điểm</span>
                        <span class="assessment-summary-value"><c:out value="${completedCount}"/></span>
                        <span class="assessment-summary-sub">Đã có điểm tổng kết cuối cùng</span>
                    </div>
                    <div class="assessment-summary-card ${pendingCount > 0 ? 'is-yellow' : 'is-muted'}">
                        <span class="assessment-summary-label">Chờ chấm tự luận</span>
                        <span class="assessment-summary-value"><c:out value="${pendingCount}"/></span>
                        <span class="assessment-summary-sub">${pendingCount > 0 ? 'Cần chấm điểm câu tự luận' : 'Đã chấm hết bài nộp'}</span>
                    </div>
                    <div class="assessment-summary-card is-muted">
                        <span class="assessment-summary-label">Đang làm bài</span>
                        <span class="assessment-summary-value"><c:out value="${inProgressCount}"/></span>
                        <span class="assessment-summary-sub">Sinh viên chưa nhấn nộp bài</span>
                    </div>
                </div>

                <c:if test="${not empty submissions}">
                    <div class="assessment-filter-bar">
                        <div class="assessment-filter-group">
                            <div class="assessment-filter-field assessment-filter-field--search">
                                <label for="submissionSearch">Tìm sinh viên</label>
                                <input id="submissionSearch" type="search" placeholder="Nhập mã sinh viên, họ tên hoặc lớp..."/>
                            </div>
                            <div class="assessment-filter-field">
                                <label for="submissionStatusFilter">Trạng thái bài làm</label>
                                <select id="submissionStatusFilter">
                                    <option value="ALL">Tất cả trạng thái</option>
                                    <option value="SUBMITTED">Chờ chấm điểm</option>
                                    <option value="COMPLETED">Đã hoàn thành</option>
                                    <option value="IN_PROGRESS">Đang làm</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </c:if>

                <c:choose>
                    <c:when test="${empty submissions}">
                        <div class="assessment-empty-state">
                            <div class="assessment-empty-icon" aria-hidden="true">
                                <svg viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
                            </div>
                            <strong>Chưa có sinh viên nào làm bài đánh giá này</strong>
                            <p>Danh sách bài làm sẽ xuất hiện tại đây sau khi bài đánh giá được công bố và sinh viên bắt đầu làm bài.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="assessment-table-wrap">
                            <table>
                                <thead>
                                <tr>
                                    <th>Mã SV</th>
                                    <th>Họ và tên sinh viên</th>
                                    <th>Khoa / Lớp hành chính</th>
                                    <th>Thời gian nộp</th>
                                    <th>Trạng thái</th>
                                    <th>Điểm cuối cùng</th>
                                    <th style="text-align: right;">Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="s" items="${submissions}">
                                    <c:set var="stObj" value="${studentMap[s.studentId]}"/>
                                    <tr data-submission-row data-status="${s.status}">
                                        <td>
                                            <span class="activity-code">
                                                <c:out value="${not empty stObj and not empty stObj.code ? stObj.code : s.studentId}"/>
                                            </span>
                                        </td>
                                        <td>
                                            <div class="assessment-cell-stack">
                                                <span class="assessment-cell-title">
                                                    <c:choose>
                                                        <c:when test="${not empty stObj and not empty stObj.name}"><c:out value="${stObj.name}"/></c:when>
                                                        <c:otherwise>Sinh viên #<c:out value="${s.studentId}"/></c:otherwise>
                                                    </c:choose>
                                                </span>
                                                <c:if test="${not empty stObj and not empty stObj.email}">
                                                    <span class="assessment-cell-sub"><c:out value="${stObj.email}"/></span>
                                                </c:if>
                                            </div>
                                        </td>
                                        <td>
                                            <div class="assessment-cell-stack">
                                                <span><c:out value="${not empty stObj and not empty stObj.mainClassCode ? stObj.mainClassCode : '—'}"/></span>
                                                <c:if test="${not empty stObj and not empty stObj.departmentName}">
                                                    <span class="assessment-cell-sub"><c:out value="${stObj.departmentName}"/></span>
                                                </c:if>
                                            </div>
                                        </td>
                                        <td>
                                            <c:out value="${not empty submittedAtDisplay[s.id] ? submittedAtDisplay[s.id] : 'Chưa nộp bài'}"/>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${s.status == 'COMPLETED'}">
                                                    <span class="badge status-pill status-green">Đã hoàn thành</span>
                                                </c:when>
                                                <c:when test="${s.status == 'SUBMITTED'}">
                                                    <span class="badge status-pill status-yellow">Chờ chấm điểm</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge status-pill status-blue">Đang làm</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${s.finalScore != null}">
                                                    <span class="assessment-score-pill assessment-score-pill--green">
                                                        <fmt:formatNumber value="${s.finalScore}" minFractionDigits="1" maxFractionDigits="2"/> / 10
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="assessment-score-pill assessment-score-pill--muted">Chưa có điểm</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: right;">
                                            <c:choose>
                                                <c:when test="${s.status == 'SUBMITTED'}">
                                                    <a class="button button-primary"
                                                       href="${pageContext.request.contextPath}/assessment?action=grade&amp;attemptId=${s.id}">
                                                        Chấm bài
                                                    </a>
                                                </c:when>
                                                <c:otherwise>
                                                    <a class="button button-outline"
                                                       href="${pageContext.request.contextPath}/assessment?action=grade&amp;attemptId=${s.id}">
                                                        Xem chi tiết
                                                    </a>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <tr id="submissionFilterEmpty" hidden>
                                    <td colspan="7" style="text-align: center; padding: 32px 16px; color: #64748b;">
                                        Không tìm thấy bài nộp nào khớp với bộ lọc hiện tại.
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
        var searchInput = document.getElementById('submissionSearch');
        var statusSelect = document.getElementById('submissionStatusFilter');
        var rows = document.querySelectorAll('[data-submission-row]');
        var emptyRow = document.getElementById('submissionFilterEmpty');
        if (!searchInput || !statusSelect || !rows.length) {
            return;
        }
        function filterSubmissions() {
            var q = searchInput.value.trim().toLowerCase();
            var st = statusSelect.value;
            var visible = 0;
            rows.forEach(function (row) {
                var textMatch = !q || row.textContent.toLowerCase().indexOf(q) !== -1;
                var statusMatch = st === 'ALL' || row.getAttribute('data-status') === st;
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
        searchInput.addEventListener('input', filterSubmissions);
        statusSelect.addEventListener('change', filterSubmissions);
    })();
</script>
</body>
</html>
