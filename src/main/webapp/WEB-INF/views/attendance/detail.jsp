<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="attendance" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Chi tiết điểm danh sinh viên</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261109-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/attendance.css?v=20261110-1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> ĐIỂM DANH SINH VIÊN <span aria-hidden="true">/</span> CHI TIẾT</span>
                    <h1>Chi tiết điểm danh của sinh viên</h1>
                    <p>Theo dõi kết quả điểm danh của sinh viên trong tất cả các buổi của hoạt động và lịch sử cập nhật trạng thái điểm danh.</p>
                </div>
            </header>

            <div class="activity-page-back" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
                <a class="button button-back"
                   href="${pageContext.request.contextPath}/attendance?activityId=${selectedActivity.id}&amp;sessionNumber=${selectedSessionNumber}">
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                    Quay lại danh sách điểm danh
                </a>
                <a class="button button-outline"
                   href="${pageContext.request.contextPath}/student-grades?action=activityDetail&amp;studentId=${participant.studentId}&amp;activityId=${selectedActivity.id}">
                    Bảng điểm sinh viên
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
                </a>
            </div>

            <c:if test="${not empty sessionScope.successMessage}">
                <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
                <c:remove var="successMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>

            <section class="card activity-table-card" aria-labelledby="student-attendance-detail-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="student-attendance-detail-title">
                            <c:out value="${participant.studentCode}"/> – <c:out value="${participant.studentName}"/>
                        </h2>
                        <p>
                            Hoạt động: <strong><c:out value="${selectedActivity.code}"/> – <c:out value="${selectedActivity.name}"/></strong>
                        </p>
                    </div>
                    <div class="activity-heading-actions">
                        <span class="badge status-pill ${st:css(participant.attendanceStatus)}">
                            Tổng hợp: ${st:label(participant.attendanceStatus)} (<c:out value="${participant.attendedSessionCount}"/>/<c:out value="${totalSessions}"/> buổi có mặt)
                        </span>
                    </div>
                </div>

                <%-- Bộ chọn nhanh Hoạt động & Sinh viên --%>
                <details class="activity-filter-panel" open>
                    <summary class="activity-filter-toggle"><span>Chuyển nhanh Hoạt động / Sinh viên</span></summary>
                    <form class="activity-filter attendance-filter" action="${pageContext.request.contextPath}/attendance" method="get">
                        <input type="hidden" name="action" value="detail">
                        <input type="hidden" name="sessionNumber" value="${selectedSessionNumber}">
                        <div class="activity-filter-field attendance-filter-activity">
                            <label id="detail-activity-label">Hoạt động</label>
                            <div class="multi-select-combobox" data-multi-select data-single data-auto-submit
                                 data-item-label="hoạt động" data-input-name="activityId">
                                <button type="button" class="multi-select-trigger" aria-expanded="false"
                                        aria-controls="detail-activity-options" aria-labelledby="detail-activity-label">
                                    <span data-selection-label>-- Chọn hoạt động --</span>
                                    <span aria-hidden="true">▾</span>
                                </button>
                                <div class="multi-select-dropdown" id="detail-activity-options" hidden>
                                    <input class="multi-select-search" type="search"
                                           placeholder="Tìm mã hoặc tên hoạt động..." aria-label="Tìm hoạt động">
                                    <fieldset class="multi-select-options">
                                        <legend class="visually-hidden">Chọn hoạt động</legend>
                                        <c:forEach var="act" items="${activities}">
                                            <label class="multi-select-option">
                                                <input type="radio" name="activityId" value="${act.id}" ${selectedActivityId == act.id ? 'checked' : ''}>
                                                <span><c:out value="${act.code}"/> – <c:out value="${act.name}"/><c:if test="${not empty act.semesterName}"> (<c:out value="${act.semesterName}"/>)</c:if> [${act.attendanceSessionCount} buổi]</span>
                                            </label>
                                        </c:forEach>
                                    </fieldset>
                                    <div class="multi-select-empty" hidden>Không tìm thấy hoạt động phù hợp.</div>
                                </div>
                                <span class="multi-select-count" aria-live="polite">Chưa chọn hoạt động</span>
                            </div>
                        </div>
                        <div class="activity-filter-field attendance-filter-activity">
                            <label id="detail-participant-label">Sinh viên trong hoạt động</label>
                            <div class="multi-select-combobox" data-multi-select data-single data-auto-submit
                                 data-item-label="sinh viên" data-input-name="participantId">
                                <button type="button" class="multi-select-trigger" aria-expanded="false"
                                        aria-controls="detail-participant-options" aria-labelledby="detail-participant-label">
                                    <span data-selection-label>-- Chọn sinh viên --</span>
                                    <span aria-hidden="true">▾</span>
                                </button>
                                <div class="multi-select-dropdown" id="detail-participant-options" hidden>
                                    <input class="multi-select-search" type="search"
                                           placeholder="Tìm mã SV hoặc họ tên..." aria-label="Tìm sinh viên">
                                    <fieldset class="multi-select-options">
                                        <legend class="visually-hidden">Chọn sinh viên</legend>
                                        <c:forEach var="item" items="${allParticipants}">
                                            <label class="multi-select-option">
                                                <input type="radio" name="participantId" value="${item.participantId}" ${participant.participantId == item.participantId ? 'checked' : ''}>
                                                <span><c:out value="${item.studentCode}"/> – <c:out value="${item.studentName}"/><c:if test="${not empty item.mainClassCode}"> (<c:out value="${item.mainClassCode}"/>)</c:if></span>
                                            </label>
                                        </c:forEach>
                                    </fieldset>
                                    <div class="multi-select-empty" hidden>Không tìm thấy sinh viên phù hợp.</div>
                                </div>
                                <span class="multi-select-count" aria-live="polite">Chưa chọn sinh viên</span>
                            </div>
                        </div>
                    </form>
                </details>

                <%-- Thông tin tổng quan Sinh viên & Điểm danh --%>
                <div class="attendance-detail-grid">
                    <div class="attendance-detail-item">
                        <span>Mã sinh viên</span>
                        <strong><c:out value="${not empty participant.studentCode ? participant.studentCode : '—'}"/></strong>
                    </div>
                    <div class="attendance-detail-item">
                        <span>Họ và tên</span>
                        <strong><c:out value="${not empty participant.studentName ? participant.studentName : '—'}"/></strong>
                    </div>
                    <div class="attendance-detail-item">
                        <span>Khoa</span>
                        <strong><c:out value="${not empty participant.departmentName ? participant.departmentName : '—'}"/></strong>
                    </div>
                    <div class="attendance-detail-item">
                        <span>Ngành</span>
                        <strong><c:out value="${not empty participant.majorName ? participant.majorName : '—'}"/></strong>
                    </div>
                    <div class="attendance-detail-item">
                        <span>Chương trình đào tạo</span>
                        <strong><c:out value="${not empty participant.academicProgramName ? participant.academicProgramName : '—'}"/></strong>
                    </div>
                    <div class="attendance-detail-item">
                        <span>Lớp hành chính</span>
                        <strong><c:out value="${not empty participant.mainClassCode ? participant.mainClassCode : '—'}"/></strong>
                    </div>
                    <div class="attendance-detail-item">
                        <span>Tổng số buổi điểm danh</span>
                        <strong><c:out value="${totalSessions}"/> buổi</strong>
                    </div>
                    <div class="attendance-detail-item">
                        <span>Số buổi có mặt / Vắng mặt</span>
                        <strong>
                            Có mặt <c:out value="${participant.attendedSessionCount}"/>/<c:out value="${totalSessions}"/> buổi
                            • Vắng <c:out value="${participant.absentSessionCount}"/> buổi
                        </strong>
                    </div>
                    <div class="attendance-detail-item">
                        <span>Điểm chuyên cần trung bình</span>
                        <strong>
                            <span class="attendance-score-badge">
                                <c:out value="${participant.formattedAttendanceScoreOneDecimal}"/> / 5.0 điểm
                            </span>
                        </strong>
                    </div>
                </div>

                <%-- BẢNG 1: DỮ LIỆU ĐIỂM DANH CỦA SINH VIÊN TRONG TẤT CẢ CÁC BUỔI --%>
                <div class="attendance-detail-section-head">
                    <div>
                        <h3 id="all-sessions-heading">Dữ liệu điểm danh trong tất cả các buổi (<c:out value="${totalSessions}"/> buổi)</h3>
                        <p>Chi tiết trạng thái điểm danh và số điểm chuyên cần của sinh viên ở từng buổi học/hoạt động.</p>
                    </div>
                </div>
                <div class="table-wrap activity-table-wrap">
                    <table aria-labelledby="all-sessions-heading">
                        <thead>
                        <tr>
                            <th scope="col" style="width: 64px;">STT</th>
                            <th scope="col" style="width: 130px;">Buổi điểm danh</th>
                            <th scope="col" style="width: 210px;">Trạng thái điểm danh</th>
                            <th scope="col" style="width: 110px; text-align: center;">Điểm buổi</th>
                            <th scope="col">Người thực hiện</th>
                            <th scope="col">Đơn vị</th>
                            <th scope="col" style="width: 170px;">Thời gian cập nhật</th>
                            <th scope="col" class="activity-action-column" style="width: 140px;">Thao tác</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="sess" items="${sessionAttendances}" varStatus="sessLoop">
                            <tr>
                                <td><c:out value="${sessLoop.index + 1}"/></td>
                                <td>
                                    <strong>Buổi <c:out value="${sess.sessionNumber}"/></strong>
                                    <small class="text-muted"> / <c:out value="${totalSessions}"/></small>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${canTakeAttendance}">
                                            <form action="${pageContext.request.contextPath}/attendance" method="post"
                                                  style="display:inline-flex; align-items:center; gap:6px; margin:0;">
                                                <input type="hidden" name="action" value="saveStudentSession">
                                                <input type="hidden" name="activityId" value="${selectedActivity.id}">
                                                <input type="hidden" name="participantId" value="${participant.participantId}">
                                                <input type="hidden" name="sessionNumber" value="${selectedSessionNumber}">
                                                <input type="hidden" name="targetSessionNumber" value="${sess.sessionNumber}">
                                                <select name="attendanceStatusValue"
                                                        class="attendance-status-select ${st:css(sess.attendanceStatus)}"
                                                        onchange="this.form.submit()"
                                                        aria-label="Cập nhật điểm danh buổi ${sess.sessionNumber}">
                                                    <c:forEach var="stItem" items="${participantStatuses}">
                                                        <option value="${stItem}" class="${st:css(stItem)}" ${sess.attendanceStatus == stItem ? 'selected' : ''}>
                                                            ${st:label(stItem)}
                                                        </option>
                                                    </c:forEach>
                                                </select>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge status-pill ${st:css(sess.attendanceStatus)}">
                                                ${st:label(sess.attendanceStatus)}
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td style="text-align: center;">
                                    <strong class="attendance-score-badge">
                                        <c:out value="${sess.formattedSessionScore}"/>đ
                                    </strong>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty sess.updatedByName}">
                                            <strong><c:out value="${sess.updatedByName}"/></strong>
                                        </c:when>
                                        <c:when test="${sess.attendanceStatus != 'NOT_YET'}">
                                            Hệ thống / NV Điểm danh
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">—</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty sess.updatedByUnit}">
                                            <c:out value="${sess.updatedByUnit}"/>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">—</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty sess.updatedAt}">
                                            ${dt:format(sess.updatedAt)}
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted">—</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="activity-action-column">
                                    <a class="button button-sm button-outline action-btn"
                                       href="${pageContext.request.contextPath}/attendance?activityId=${selectedActivity.id}&amp;sessionNumber=${sess.sessionNumber}">
                                        <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg></span>
                                        Mở Buổi ${sess.sessionNumber}
                                    </a>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>

                <%-- BẢNG 2: LỊCH SỬ ĐIỂM DANH CỦA SINH VIÊN --%>
                <div class="attendance-detail-section-head">
                    <div>
                        <h3 id="history-heading">Lịch sử điểm danh của sinh viên</h3>
                        <p>Ghi nhận toàn bộ các lần thay đổi trạng thái điểm danh của sinh viên qua các buổi.</p>
                    </div>
                    <span class="activity-count"><c:out value="${attendanceHistory.size()}"/> lần cập nhật</span>
                </div>
                <div class="table-wrap activity-table-wrap">
                    <table aria-labelledby="history-heading">
                        <thead>
                        <tr>
                            <th scope="col" style="width: 64px;">STT</th>
                            <th scope="col" style="width: 170px;">Thời gian cập nhật</th>
                            <th scope="col" style="width: 130px;">Buổi điểm danh</th>
                            <th scope="col" style="width: 160px;">Trạng thái cũ</th>
                            <th scope="col" style="width: 160px;">Trạng thái mới</th>
                            <th scope="col">Người thực hiện</th>
                            <th scope="col" style="width: 140px;">Mã nhân viên</th>
                            <th scope="col">Đơn vị</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:choose>
                            <c:when test="${empty attendanceHistory}">
                                <tr>
                                    <td class="empty-state activity-empty-state" colspan="8">
                                        <span class="empty-state-mark" aria-hidden="true">✓</span>
                                        <strong>Chưa có lịch sử thay đổi điểm danh</strong>
                                        <span>Các lần cập nhật trạng thái điểm danh của sinh viên này sẽ được lưu lại tại đây.</span>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="h" items="${attendanceHistory}" varStatus="loop">
                                    <tr>
                                        <td><c:out value="${loop.index + 1}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty h.createdAt}">
                                                    ${dt:format(h.createdAt)}
                                                </c:when>
                                                <c:otherwise>—</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><strong>Buổi <c:out value="${h.sessionNumber}"/></strong></td>
                                        <td>
                                            <span class="badge status-pill ${st:css(h.oldStatus)}">
                                                ${st:label(h.oldStatus)}
                                            </span>
                                        </td>
                                        <td>
                                            <span class="badge status-pill ${st:css(h.newStatus)}">
                                                ${st:label(h.newStatus)}
                                            </span>
                                        </td>
                                        <td>
                                            <strong><c:out value="${not empty h.createdByName ? h.createdByName : 'Hệ thống'}"/></strong>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty h.createdByCode}">
                                                    <span class="activity-code"><c:out value="${h.createdByCode}"/></span>
                                                </c:when>
                                                <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty h.createdByUnit}">
                                                    <c:out value="${h.createdByUnit}"/>
                                                </c:when>
                                                <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                        </tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261109-4"></script>
<script src="${pageContext.request.contextPath}/assets/js/attendance.js?v=20261109-4"></script>
</body>
</html>
