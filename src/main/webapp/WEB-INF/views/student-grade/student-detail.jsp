<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="student-grades" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Chi tiết điểm hoạt động của sinh viên</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261109-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/student-grade.css?v=20261110-1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> CHI TIẾT ĐIỂM HOẠT ĐỘNG SINH VIÊN</span>
                    <h1>Chi tiết điểm hoạt động của sinh viên</h1>
                    <p>Xem tiến trình, bảng điểm tổng quan và lịch sử tham gia tất cả các hoạt động ngoại khóa của sinh viên.</p>
                </div>
            </header>

            <c:if test="${not isStudent}">
                <div class="activity-page-back">
                    <a class="button button-back" href="${pageContext.request.contextPath}/student-grades?semesterId=${selectedSemesterId}">
                        <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                        Quay lại danh sách điểm sinh viên
                    </a>
                </div>
            </c:if>

            <section class="card activity-table-card" aria-labelledby="student-grade-detail-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="student-grade-detail-title">
                            <c:choose>
                                <c:when test="${not empty selectedStudent}">
                                    Hồ sơ điểm hoạt động: <c:out value="${selectedStudent.studentCode}"/> – <c:out value="${selectedStudent.studentName}"/>
                                </c:when>
                                <c:otherwise>Hồ sơ điểm hoạt động sinh viên</c:otherwise>
                            </c:choose>
                        </h2>
                        <p>
                            <c:choose>
                                <c:when test="${not empty selectedStudent}">
                                    Khoa / Ngành: <c:out value="${selectedStudent.departmentName}" default="—"/> / <c:out value="${selectedStudent.majorName}" default="—"/> |
                                    CTĐT / Lớp: <c:out value="${selectedStudent.curriculumCode}" default="—"/> / <c:out value="${selectedStudent.mainClassCode}" default="—"/>
                                    <c:if test="${not empty selectedSemester}">
                                        | Học kỳ: <strong><c:out value="${selectedSemester.code}"/> – <c:out value="${selectedSemester.name}"/></strong>
                                    </c:if>
                                </c:when>
                                <c:otherwise>Chọn sinh viên để tra cứu</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                    <c:if test="${not empty selectedStudent}">
                        <div class="activity-heading-actions">
                            <span class="activity-count">
                                Tham gia <c:out value="${selectedStudent.participationRatioText}"/> hoạt động
                            </span>
                        </div>
                    </c:if>
                </div>

                <%-- 1. Bộ chọn Học kỳ & Sinh viên --%>
                <form class="grade-selector-bar" action="${pageContext.request.contextPath}/student-grades" method="get">
                    <input type="hidden" name="action" value="studentDetail">
                    <div class="grade-selector-field">
                        <label for="semester-selector">Học kỳ</label>
                        <select id="semester-selector" name="semesterId" onchange="this.form.submit()">
                            <option value="0" ${selectedSemesterId == 0 ? 'selected' : ''}>Tất cả học kỳ</option>
                            <c:forEach var="sem" items="${semesters}">
                                <option value="${sem.id}" ${selectedSemesterId == sem.id ? 'selected' : ''}>
                                    <c:out value="${sem.code}"/> – <c:out value="${sem.name}"/><c:if test="${sem.id == activeSemesterId}"> (Đang diễn ra)</c:if>
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <c:if test="${not isStudent}">
                        <div class="grade-selector-field">
                            <label for="student-quick-filter">Tìm nhanh sinh viên (theo Mã hoặc Tên)</label>
                            <input id="student-quick-filter" type="search" placeholder="Gõ mã SV hoặc họ tên để lọc danh sách..." autocomplete="off">
                        </div>
                        <div class="grade-selector-field">
                            <label for="student-selector">Chọn sinh viên</label>
                            <select id="student-selector" name="studentId" onchange="this.form.submit()">
                                <c:forEach var="st" items="${allStudents}">
                                    <option value="${st.studentId}" ${selectedStudentId == st.studentId ? 'selected' : ''}>
                                        <c:out value="${st.studentCode}"/> – <c:out value="${st.studentName}"/><c:if test="${not empty st.mainClassCode}"> (<c:out value="${st.mainClassCode}"/>)</c:if>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                    </c:if>
                </form>

                <%-- 2. Thẻ Thông tin Tổng quan (Summary Cards) --%>
                <c:if test="${not empty selectedStudent}">
                    <div class="grade-summary-cards" aria-label="Thẻ thông tin tổng quan điểm sinh viên">
                        <div class="grade-summary-card">
                            <span class="grade-summary-card-label">Sinh viên</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.studentCode}" default="—"/></strong>
                            <span class="grade-summary-card-sub"><c:out value="${selectedStudent.studentName}" default="—"/> (<c:out value="${selectedStudent.participationRatioText}"/> HĐ)</span>
                        </div>
                        <div class="grade-summary-card">
                            <span class="grade-summary-card-label">Tổng điểm tham gia</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedTotalParticipationScore}"/></strong>
                            <span class="grade-summary-card-sub">ĐD: <c:out value="${selectedStudent.formattedTotalAttendanceScore}"/> | ĐG: <c:out value="${selectedStudent.formattedTotalParticipationEvalScore}"/></span>
                        </div>
                        <div class="grade-summary-card">
                            <span class="grade-summary-card-label">Tổng điểm thu hoạch</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedTotalSummaryScore}"/></strong>
                            <span class="grade-summary-card-sub">Tổng điểm TB: <c:out value="${selectedStudent.formattedTotalAverageScore}"/></span>
                        </div>
                        <div class="grade-summary-card is-plus">
                            <span class="grade-summary-card-label">Tổng số điểm cộng</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedPlusPoints}"/></strong>
                            <span class="grade-summary-card-sub">Từ các hoạt động có mặt</span>
                        </div>
                        <div class="grade-summary-card is-minus">
                            <span class="grade-summary-card-label">Tổng số điểm trừ</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedMinusPoints}"/></strong>
                            <span class="grade-summary-card-sub">Trừ 5đ / hoạt động vắng mặt</span>
                        </div>
                        <div class="grade-summary-card is-total">
                            <span class="grade-summary-card-label">Tổng điểm tích lũy</span>
                            <strong class="grade-summary-card-value"><c:out value="${selectedStudent.formattedAccumulatedPoints}"/></strong>
                            <span class="grade-summary-card-sub">Điểm cộng − Điểm trừ</span>
                        </div>
                    </div>
                </c:if>

                <%-- 3. Bảng Lịch sử tham gia hoạt động --%>
                <div class="table-wrap activity-table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th scope="col" class="col-code">Mã HĐ</th>
                            <th scope="col" class="col-object-name">Tên hoạt động</th>
                            <th scope="col" class="col-status">Trạng thái HĐ</th>
                            <th scope="col" class="col-status">Trạng thái điểm danh</th>
                            <th scope="col" style="text-align: center;">Điểm tham gia</th>
                            <th scope="col" style="text-align: center;">Điểm thu hoạch</th>
                            <th scope="col" style="text-align: center;">Điểm TB</th>
                            <th scope="col" style="text-align: center;">Điểm tích lũy</th>
                            <th scope="col" class="activity-action-column">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:choose>
                            <c:when test="${empty activityGrades}">
                                <tr>
                                    <td class="empty-state activity-empty-state" colspan="9">
                                        <span class="empty-state-mark" aria-hidden="true">✓</span>
                                        <strong>Sinh viên chưa tham gia hoạt động nào</strong>
                                        <span>Lịch sử tham gia và điểm tích lũy sẽ hiển thị tại đây khi sinh viên có tên trong hoạt động.</span>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="ag" items="${activityGrades}">
                                    <tr>
                                        <td class="col-code"><span class="activity-code"><c:out value="${ag.activityCode}"/></span></td>
                                        <td class="col-object-name" title="<c:out value='${ag.activityName}'/>">
                                            <strong class="activity-name"><c:out value="${ag.activityName}"/></strong>
                                            <div class="text-muted" style="font-size: .76rem;">
                                                <c:out value="${ag.organizationLevelName}" default="Cấp mặc định"/> (x<c:out value="${ag.formattedOrgLevelCoefficient}"/>)
                                                • <c:out value="${ag.registrationTypeLabel}"/>
                                            </div>
                                        </td>
                                        <td class="col-status">
                                            <span class="badge status-pill ${st:css(ag.activityStatus)}">${st:label(ag.activityStatus)}</span>
                                        </td>
                                        <td class="col-status">
                                            <span class="badge status-pill ${st:css(ag.attendanceStatus)}">${st:label(ag.attendanceStatus)}</span>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${ag.attendanceStatus == 'NOT_YET'}"><span class="text-muted">Chưa ĐD</span></c:when>
                                                <c:otherwise><strong><c:out value="${ag.formattedParticipationScore}"/></strong></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: center;">
                                            <span class="${ag.summaryRequired ? '' : 'text-muted'}"><c:out value="${ag.formattedSummaryScore}"/></span>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${ag.attendanceStatus == 'NOT_YET'}"><span class="text-muted">—</span></c:when>
                                                <c:otherwise><strong><c:out value="${ag.formattedAverageScore}"/></strong></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: center;">
                                            <c:choose>
                                                <c:when test="${ag.attendanceStatus == 'NOT_YET'}">
                                                    <span class="grade-pill grade-pill--neutral">0</span>
                                                </c:when>
                                                <c:when test="${ag.accumulatedScore < 0}">
                                                    <span class="grade-pill grade-pill--minus"><c:out value="${ag.formattedAccumulatedScore}"/></span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="grade-pill grade-pill--plus">+<c:out value="${ag.formattedAccumulatedScore}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="activity-action-column">
                                            <div class="actions activity-actions">
                                                <a class="activity-action-link action-btn action-btn--view"
                                                   href="${pageContext.request.contextPath}/student-grades?action=activityDetail&amp;studentId=${selectedStudent.studentId}&amp;activityId=${ag.activityId}"
                                                   title="Xem chi tiết điểm hoạt động">
                                                    <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></span>
                                                    Chi tiết
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                        </tbody>
                        <c:if test="${not empty activityGrades and not empty selectedStudent}">
                            <tfoot>
                            <tr style="background: #f8fafc; font-weight: 700;">
                                <td colspan="4" style="text-align: right;">Tổng các đầu điểm trong học kỳ:</td>
                                <td style="text-align: center;"><c:out value="${selectedStudent.formattedTotalParticipationScore}"/></td>
                                <td style="text-align: center;"><c:out value="${selectedStudent.formattedTotalSummaryScore}"/></td>
                                <td style="text-align: center;"><c:out value="${selectedStudent.formattedTotalAverageScore}"/></td>
                                <td style="text-align: center;">
                                    <span class="grade-pill ${selectedStudent.accumulatedPoints < 0 ? 'grade-pill--minus' : (selectedStudent.accumulatedPoints > 0 ? 'grade-pill--plus' : 'grade-pill--neutral')}">
                                        <c:out value="${selectedStudent.formattedAccumulatedPoints}"/>
                                    </span>
                                </td>
                                <td></td>
                            </tr>
                            </tfoot>
                        </c:if>
                    </table>
                </div>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script>
    (function () {
        var searchInput = document.getElementById('student-quick-filter');
        var selectEl = document.getElementById('student-selector');
        if (!searchInput || !selectEl) return;

        function normalize(str) {
            return (str || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
                .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();
        }

        searchInput.addEventListener('input', function () {
            var q = normalize(searchInput.value);
            var firstVisible = null;
            Array.from(selectEl.options).forEach(function (opt) {
                var match = !q || normalize(opt.textContent).indexOf(q) !== -1;
                opt.hidden = !match;
                if (match && !firstVisible) firstVisible = opt;
            });
        });
    })();
</script>
</body>
</html>
