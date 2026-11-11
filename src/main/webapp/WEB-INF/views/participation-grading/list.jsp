<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="participation-grading" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Chấm điểm tham gia</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261109-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/attendance.css?v=20261110-2">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> CHẤM ĐIỂM THAM GIA</span>
                    <h1>Chấm điểm tham gia hoạt động</h1>
                    <p>Đánh giá mức độ đóng góp, thái độ tham gia (0–5 điểm kèm nhận xét) cho sinh viên có mặt ít nhất 1 buổi điểm danh. Điểm tham gia = Điểm chuyên cần TB các buổi + Điểm đánh giá tham gia.</p>
                </div>
            </header>

            <div class="activity-page-back" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
                <a class="button button-back"
                    href="${pageContext.request.contextPath}/activities<c:if test='${not empty selectedActivity}'>?action=detail&amp;id=${selectedActivity.id}#tab-attendance</c:if>">
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>
                    Điểm danh sinh viên
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

    <section class="card activity-table-card" aria-labelledby="grading-table-title">
        <div class="activity-table-heading">
            <div>
                <h2 id="grading-table-title">Danh sách chấm điểm tham gia</h2>
                <p>
                    <c:choose>
                        <c:when test="${not empty selectedActivity}">
                            <c:out value="${selectedActivity.code}"/> – <c:out value="${selectedActivity.name}"/>
                        </c:when>
                        <c:otherwise>Chọn hoạt động ngoại khóa để chấm điểm tham gia</c:otherwise>
                    </c:choose>
                </p>
            </div>
            <c:if test="${not empty selectedActivity}">
                <div class="activity-heading-actions">
                    <span class="activity-count">
                        <c:choose>
                            <c:when test="${filtered}"><c:out value="${filteredParticipantCount}"/> / <c:out value="${totalParticipantCount}"/> sinh viên</c:when>
                            <c:otherwise><c:out value="${totalParticipantCount}"/> sinh viên</c:otherwise>
                        </c:choose>
                    </span>
                </div>
            </c:if>
        </div>

        <details class="activity-filter-panel" open>
            <summary class="activity-filter-toggle"><span>Chọn hoạt động</span></summary>
            <form class="activity-filter attendance-filter attendance-filter--single" action="${pageContext.request.contextPath}/participation-grading" method="get" data-attendance-activity-form>
                <div class="activity-filter-field attendance-filter-activity">
                    <label id="grading-activity-label">Hoạt động</label>
                    <div class="multi-select-combobox" data-multi-select data-single data-auto-submit
                         data-item-label="hoạt động" data-input-name="activityId">
                        <button type="button" class="multi-select-trigger" aria-expanded="false"
                                aria-controls="grading-activity-options" aria-labelledby="grading-activity-label">
                            <span data-selection-label>-- Chọn hoạt động --</span>
                            <span aria-hidden="true">▾</span>
                        </button>
                        <div class="multi-select-dropdown" id="grading-activity-options" hidden>
                            <input class="multi-select-search" type="search"
                                   placeholder="Tìm mã hoặc tên hoạt động..." aria-label="Tìm hoạt động">
                            <fieldset class="multi-select-options">
                                <legend class="visually-hidden">Chọn hoạt động</legend>
                                <c:forEach var="activity" items="${activities}">
                                    <label class="multi-select-option">
                                        <input type="radio" name="activityId" value="${activity.id}" ${selectedActivityId == activity.id ? 'checked' : ''}>
                                        <span><c:out value="${activity.code}"/> – <c:out value="${activity.name}"/><c:if test="${not empty activity.semesterName}"> (<c:out value="${activity.semesterName}"/>)</c:if> [${activity.attendanceSessionCount} buổi]</span>
                                    </label>
                                </c:forEach>
                            </fieldset>
                            <div class="multi-select-empty" hidden>Không tìm thấy hoạt động phù hợp.</div>
                        </div>
                        <span class="multi-select-count" aria-live="polite">Chưa chọn hoạt động</span>
                    </div>
                </div>
                <noscript>
                    <div class="activity-filter-buttons">
                        <button type="submit" class="button button-primary button-sm">Xem danh sách</button>
                    </div>
                </noscript>
            </form>
        </details>

        <c:if test="${not empty selectedActivity}">
            <div class="attendance-summary">
                <div class="attendance-summary-item">
                    <span class="attendance-summary-label">Thời gian hoạt động</span>
                    <span class="activity-time">
                        <c:choose>
                            <c:when test="${not empty selectedActivity.startTime}">
                                ${dt:format(selectedActivity.startTime)}
                            </c:when>
                            <c:otherwise>Chưa xác định</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <div class="attendance-summary-item">
                    <span class="attendance-summary-label">Số buổi điểm danh</span>
                    <span class="activity-time">
                        <strong><c:out value="${selectedActivity.attendanceSessionCount}"/> buổi</strong>
                    </span>
                </div>
                <div class="attendance-summary-item">
                    <span class="attendance-summary-label">Cấp tổ chức (Hệ số)</span>
                    <span class="activity-time">
                        <c:out value="${selectedActivity.organizationLevelName}" default="Mặc định"/>
                        (x<c:out value="${not empty selectedActivity.formattedOrganizationLevelCoefficient ? selectedActivity.formattedOrganizationLevelCoefficient : '1.00'}"/>)
                    </span>
                </div>
                <div class="attendance-summary-item">
                    <span class="attendance-summary-label">Trạng thái hoạt động</span>
                    <span class="badge status-pill ${st:css(selectedActivity.activityStatus)}">${st:label(selectedActivity.activityStatus)}</span>
                </div>
            </div>

            <c:set var="filterHiddenFields">
                <input type="hidden" name="activityId" value="${selectedActivity.id}">
                <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                <input type="hidden" name="department" value="<c:out value='${selectedDepartment}'/>">
                <input type="hidden" name="academicProgram" value="<c:out value='${selectedAcademicProgram}'/>">
                <input type="hidden" name="major" value="<c:out value='${selectedMajor}'/>">
                <input type="hidden" name="mainClass" value="<c:out value='${selectedMainClass}'/>">
                <input type="hidden" name="gradingStatus" value="<c:out value='${selectedGradingStatus}'/>">
                <input type="hidden" name="page" value="${currentPage}">
                <input type="hidden" name="pageSize" value="${pageSize}">
            </c:set>

            <details class="activity-filter-panel" ${filtered ? 'open' : ''}>
                <summary class="activity-filter-toggle">
                    <span>Tìm kiếm &amp; Bộ lọc</span>
                    <c:if test="${activeFilterCount > 0}">
                        <span class="activity-filter-badge"><c:out value="${activeFilterCount}"/></span>
                    </c:if>
                </summary>
                <form class="activity-filter" action="${pageContext.request.contextPath}/participation-grading" method="get">
                    <input type="hidden" name="activityId" value="${selectedActivity.id}">
                    <input type="hidden" name="pageSize" value="${pageSize}">
                    <div class="activity-filter-field">
                        <label for="filter-keyword">Tìm kiếm</label>
                        <input id="filter-keyword" type="search" name="keyword" value="<c:out value='${keyword}'/>"
                               placeholder="Mã SV hoặc họ tên">
                    </div>
                    <div class="activity-filter-field">
                        <label for="filter-department">Khoa</label>
                        <select id="filter-department" name="department">
                            <option value="">Tất cả</option>
                            <c:forEach var="item" items="${departments}">
                                <option value="<c:out value='${item}'/>" ${selectedDepartment == item ? 'selected' : ''}><c:out value="${item}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="activity-filter-field">
                        <label for="filter-program">Chương trình đào tạo</label>
                        <select id="filter-program" name="academicProgram">
                            <option value="">Tất cả</option>
                            <c:forEach var="item" items="${academicPrograms}">
                                <option value="<c:out value='${item}'/>" ${selectedAcademicProgram == item ? 'selected' : ''}><c:out value="${item}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="activity-filter-field">
                        <label for="filter-major">Ngành</label>
                        <select id="filter-major" name="major">
                            <option value="">Tất cả</option>
                            <c:forEach var="item" items="${majors}">
                                <option value="<c:out value='${item}'/>" ${selectedMajor == item ? 'selected' : ''}><c:out value="${item}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="activity-filter-field">
                        <label for="filter-class">Lớp</label>
                        <select id="filter-class" name="mainClass">
                            <option value="">Tất cả</option>
                            <c:forEach var="item" items="${mainClasses}">
                                <option value="<c:out value='${item}'/>" ${selectedMainClass == item ? 'selected' : ''}><c:out value="${item}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="activity-filter-field">
                        <label for="filter-grading-status">Trạng thái chấm điểm</label>
                        <select id="filter-grading-status" name="gradingStatus">
                            <option value="">Tất cả</option>
                            <option value="ELIGIBLE" ${selectedGradingStatus == 'ELIGIBLE' ? 'selected' : ''}>Đủ điều kiện chấm (Có mặt ≥ 1 buổi)</option>
                            <option value="GRADED" ${selectedGradingStatus == 'GRADED' ? 'selected' : ''}>Đã chấm điểm</option>
                            <option value="UNGRADED" ${selectedGradingStatus == 'UNGRADED' ? 'selected' : ''}>Chưa chấm điểm</option>
                            <option value="INELIGIBLE" ${selectedGradingStatus == 'INELIGIBLE' ? 'selected' : ''}>Chưa đủ điều kiện (Vắng / Chưa điểm danh)</option>
                        </select>
                    </div>
                    <div class="activity-filter-buttons">
                        <button type="submit" class="button button-primary button-sm">Tìm kiếm &amp; Lọc</button>
                        <c:if test="${filtered}">
                            <a class="button button-sm" href="${pageContext.request.contextPath}/participation-grading?activityId=${selectedActivity.id}&amp;pageSize=${pageSize}">Xóa Bộ lọc</a>
                        </c:if>
                    </div>
                </form>
            </details>

            <ul class="attendance-counts" aria-label="Thống kê chấm điểm tham gia">
                <li class="badge status-pill status-green">Đủ điều kiện chấm: <strong><c:out value="${eligibleCount}"/></strong></li>
                <li class="badge status-pill status-green">Đã chấm điểm: <strong data-grading-count="GRADED"><c:out value="${gradedCount}"/></strong></li>
                <li class="badge status-pill status-yellow">Chưa chấm điểm: <strong data-grading-count="UNGRADED"><c:out value="${ungradedCount}"/></strong></li>
                <li class="badge status-pill status-gray">Vắng / Chưa điểm danh: <strong><c:out value="${ineligibleCount}"/></strong></li>
            </ul>

            <div id="attendance-ajax-alert" class="alert alert-error activity-alert attendance-ajax-alert" role="alert" hidden></div>
            <div id="attendance-ajax-success" class="alert alert-success activity-alert attendance-ajax-alert" role="status" hidden></div>

            <c:choose>
                <c:when test="${canTakeAttendance && not empty participants}">
                    <form id="attendance-form" class="activity-bulk-toolbar" action="${pageContext.request.contextPath}/participation-grading" method="post"
                          data-activity-id="${selectedActivity.id}">
                        <input type="hidden" name="action" value="batchGrade">
                        ${filterHiddenFields}
                        <span class="activity-bulk-selection" data-attendance-selection-hint>
                            Tick chọn sinh viên có mặt (≥ 1 buổi) để chấm điểm tham gia hàng loạt<c:if test="${filtered || totalPages > 1}"> (đang hiển thị <c:out value="${participants.size()}"/> sinh viên<c:if test="${filtered}"> theo bộ lọc</c:if><c:if test="${totalPages > 1}"> ở trang <c:out value="${currentPage}"/></c:if>)</c:if>.
                        </span>
                        <div class="activity-bulk-actions">
                            <button type="button" class="button button-sm button-primary action-btn" id="btn-toggle-batch-grade"
                                    data-batch-grade-trigger disabled hidden style="display: none;">
                                <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg></span>
                                Chấm điểm hàng loạt (<span data-batch-grade-count>0</span>)
                            </button>
                        </div>
                    </form>

                    <form id="batch-grade-form" class="attendance-batch-grade-panel"
                          action="${pageContext.request.contextPath}/participation-grading" method="post" hidden>
                        <input type="hidden" name="action" value="batchGrade">
                        ${filterHiddenFields}
                        <div data-batch-grade-inputs hidden></div>
                        <div class="attendance-batch-grade-head">
                            <div>
                                <h3>Chấm điểm đánh giá tham gia hàng loạt</h3>
                                <p>Áp dụng chung điểm đánh giá thái độ/mức độ tham gia (tối đa 5.0 điểm) và nhận xét cho <strong data-batch-grade-target-count>0</strong> sinh viên có mặt đã chọn.</p>
                            </div>
                            <button type="button" class="button button-sm button-outline" data-batch-grade-close>Đóng</button>
                        </div>
                        <div class="attendance-batch-grade-fields">
                            <div class="activity-filter-field attendance-batch-score-field">
                                <label for="batch-eval-score">Điểm đánh giá tham gia (0 – 5)</label>
                                <input id="batch-eval-score" type="number" name="evalScore" min="0" max="5" step="0.1" value="5" required>
                            </div>
                            <div class="activity-filter-field attendance-batch-feedback-field">
                                <label for="batch-eval-feedback">Nhận xét / Feedback chung</label>
                                <input id="batch-eval-feedback" type="text" name="feedback" maxlength="1000"
                                       placeholder="Ví dụ: Tích cực, hăng hái tham gia hoạt động">
                            </div>
                            <div class="attendance-batch-grade-actions">
                                <button type="submit" class="button button-primary button-sm" data-batch-grade-submit>
                                    Lưu điểm hàng loạt
                                </button>
                            </div>
                        </div>
                    </form>
                </c:when>
                <c:when test="${not canTakeAttendance}">
                    <p class="attendance-note">Chỉ chấm điểm tham gia khi hoạt động đang diễn ra hoặc đã hoàn thành.</p>
                </c:when>
            </c:choose>
        </c:if>

        <c:set var="showSelectColumn" value="${canTakeAttendance}"/>
        <c:set var="columnCount" value="${showSelectColumn ? 10 : 9}"/>
        <div class="table-wrap activity-table-wrap">
            <table>
                <thead>
                <tr>
                    <c:if test="${showSelectColumn}">
                        <th scope="col" class="activity-select-column">
                            <input type="checkbox" class="activity-select-checkbox" data-select-all-participants
                                   aria-label="Chọn tất cả sinh viên đang hiển thị" ${empty participants ? 'disabled' : ''}>
                        </th>
                    </c:if>
                    <th scope="col" class="col-code">Mã SV</th>
                    <th scope="col" class="col-object-name">Họ tên</th>
                    <th scope="col" class="col-academic">Khoa / Ngành</th>
                    <th scope="col" class="col-academic">CTĐT / Lớp</th>
                    <th scope="col" class="col-status">Chuyên cần</th>
                    <th scope="col" class="col-eval-score">Đánh giá</th>
                    <th scope="col" class="col-eval-feedback">Nhận xét</th>
                    <th scope="col" class="col-score">Điểm tham gia</th>
                    <th scope="col" class="activity-action-column">Chi tiết</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty selectedActivity}">
                        <tr>
                            <td class="empty-state activity-empty-state" colspan="${columnCount}">
                                <span class="empty-state-mark" aria-hidden="true">★</span>
                                <strong>Chọn hoạt động để xem danh sách chấm điểm tham gia</strong>
                                <span>Danh sách sinh viên tham gia và điểm đánh giá sẽ hiển thị tại đây.</span>
                            </td>
                        </tr>
                    </c:when>
                    <c:when test="${empty participants && filtered}">
                        <tr>
                            <td class="empty-state activity-empty-state" colspan="${columnCount}">
                                <span class="empty-state-mark" aria-hidden="true">+</span>
                                <strong>Không tìm thấy sinh viên phù hợp</strong>
                                <span>Hãy thử thay đổi từ khóa hoặc bộ lọc.</span>
                            </td>
                        </tr>
                    </c:when>
                    <c:when test="${empty participants}">
                        <tr>
                            <td class="empty-state activity-empty-state" colspan="${columnCount}">
                                <span class="empty-state-mark" aria-hidden="true">+</span>
                                <strong>Chưa có sinh viên tham gia</strong>
                                <span>Hoạt động này chưa có sinh viên nào trong danh sách tham gia.</span>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="p" items="${participants}" varStatus="loop">
                            <tr data-participant-row="${p.participantId}">
                                <c:if test="${showSelectColumn}">
                                    <td class="activity-select-column">
                                        <input type="checkbox" class="activity-select-checkbox"
                                               value="${p.participantId}" data-participant-select
                                               data-participant-id="${p.participantId}"
                                               data-present="${p.presentStatus ? 'true' : 'false'}"
                                               aria-label="Chọn <c:out value='${p.studentName}'/>"
                                               title="${p.presentStatus ? 'Chọn sinh viên để chấm điểm tham gia' : 'Sinh viên chưa có mặt buổi nào, không thể chấm điểm tham gia'}">
                                    </td>
                                </c:if>
                                <td class="col-code"><span class="activity-code"><c:out value="${p.studentCode}" default="—"/></span></td>
                                <td class="col-object-name" title="<c:out value='${p.studentName}'/>">
                                    <strong class="activity-name" title="<c:out value='${p.studentName}'/>"><c:out value="${p.studentName}" default="—"/></strong>
                                </td>
                                <td class="col-academic" title="<c:out value='${p.departmentName} - ${p.majorName}'/>">
                                    <c:out value="${p.departmentName}" default="—"/>
                                    <c:if test="${not empty p.majorName}"><br><small class="text-muted"><c:out value="${p.majorName}"/></small></c:if>
                                </td>
                                <td class="col-academic" title="<c:out value='${p.academicProgramName} - ${p.mainClassCode}'/>">
                                    <c:out value="${p.academicProgramName}" default="—"/>
                                    <c:if test="${not empty p.mainClassCode}"><br><small class="text-muted"><c:out value="${p.mainClassCode}"/></small></c:if>
                                </td>
                                <td class="col-status">
                                    <div style="display:flex; flex-direction:column; gap:3px;">
                                        <span class="badge status-pill ${st:css(p.attendanceStatus)}">
                                            ${st:label(p.attendanceStatus)} (<c:out value="${p.attendedSessionCount}"/>/<c:out value="${p.totalSessions}"/> buổi)
                                        </span>
                                        <small class="text-muted">
                                            Chuyên cần: <strong data-attendance-score-for="${p.participantId}">(<c:out value="${p.formattedAttendanceScore}"/>đ)</strong>
                                        </small>
                                    </div>
                                </td>
                                <td class="col-eval-score">
                                    <div class="attendance-eval-score-cell"
                                         data-eval-score-state="${p.participantId}"
                                         data-present="${p.presentStatus ? 'true' : 'false'}"
                                         data-is-graded="${p.presentStatus and not empty p.participationEvalScore ? 'true' : 'false'}">
                                        <span class="attendance-score-badge${p.presentStatus and not empty p.participationEvalScore ? ' is-graded' : ' is-empty'}"
                                              data-eval-score-display="${p.participantId}">
                                            <c:choose>
                                                <c:when test="${p.presentStatus and not empty p.participationEvalScore}">
                                                    <c:out value="${p.formattedEvalScore}"/> / 5
                                                </c:when>
                                                <c:otherwise>—</c:otherwise>
                                            </c:choose>
                                        </span>
                                    </div>
                                </td>
                                <td class="col-eval-feedback">
                                    <div class="attendance-feedback-wrap">
                                        <div class="attendance-feedback-row">
                                            <div class="attendance-feedback-cell ${canTakeAttendance and p.presentStatus ? 'is-editable' : ''}"
                                                 <c:if test="${canTakeAttendance}">data-trigger-grade-modal="${p.participantId}"</c:if>>
                                                <c:choose>
                                                    <c:when test="${not empty p.participationFeedback}">
                                                        <div class="attendance-feedback-text" data-eval-feedback-display="${p.participantId}"><c:out value="${p.participationFeedback}"/></div>
                                                    </c:when>
                                                    <c:when test="${not p.presentStatus}">
                                                        <div class="attendance-feedback-text is-placeholder" data-eval-feedback-display="${p.participantId}">Chỉ chấm khi có mặt ít nhất 1 buổi</div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <div class="attendance-feedback-text is-placeholder" data-eval-feedback-display="${p.participantId}">${canTakeAttendance ? 'Nhấn "Chấm" hoặc kích vào đây để nhập nhận xét...' : '—'}</div>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                            <c:if test="${canTakeAttendance}">
                                                <button type="button"
                                                        class="button button-sm button-outline action-btn attendance-grade-modal-btn"
                                                        data-open-grade-modal="${p.participantId}"
                                                        data-student-code="<c:out value='${p.studentCode}'/>"
                                                        data-student-name="<c:out value='${p.studentName}'/>"
                                                        data-eval-score="<c:out value='${p.formattedEvalScore}'/>"
                                                        data-feedback="<c:out value='${p.participationFeedback}'/>"
                                                        ${p.presentStatus ? '' : 'disabled'}
                                                        title="${p.presentStatus ? 'Nhập hoặc chỉnh sửa điểm đánh giá và nhận xét' : 'Chỉ chấm khi sinh viên có mặt ít nhất 1 buổi'}">
                                                    <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg></span>
                                                    <span data-grade-btn-label="${p.participantId}">${not empty p.participationEvalScore ? 'Sửa' : 'Chấm'}</span>
                                                </button>
                                            </c:if>
                                        </div>
                                        <small class="attendance-graded-by"
                                               data-graded-by-for="${p.participantId}"
                                               ${empty p.participationGradedBy ? 'hidden' : ''}>
                                            <c:if test="${not empty p.participationGradedBy}">
                                                Chấm bởi: <c:out value="${p.participationGradedBy}"/>
                                            </c:if>
                                        </small>
                                    </div>
                                </td>
                                <td class="col-score">
                                    <strong class="attendance-score-badge" data-participation-score-for="${p.participantId}">
                                        <c:out value="${p.formattedParticipationScore}"/>
                                    </strong>
                                </td>
                                <td class="activity-action-column">
                                    <div class="actions activity-actions">
                                        <a class="activity-action-link action-btn action-btn--view"
                                           href="${pageContext.request.contextPath}/student-grades?action=activityDetail&amp;studentId=${p.studentId}&amp;activityId=${selectedActivity.id}"
                                           title="Xem chi tiết các đầu điểm của sinh viên trong hoạt động này">
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
            </table>
        </div>
        <c:if test="${not empty selectedActivity && filteredParticipantCount > 0}">
            <c:set var="pageBaseUrl" value="${pageContext.request.contextPath}/participation-grading?activityId=${selectedActivity.id}&pageSize=${pageSize}${filterQuery}"/>
            <div class="activity-pagination">
                <form class="activity-page-size" action="${pageContext.request.contextPath}/participation-grading" method="get">
                    <input type="hidden" name="activityId" value="${selectedActivity.id}">
                    <input type="hidden" name="page" value="1">
                    <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                    <input type="hidden" name="department" value="<c:out value='${selectedDepartment}'/>">
                    <input type="hidden" name="academicProgram" value="<c:out value='${selectedAcademicProgram}'/>">
                    <input type="hidden" name="major" value="<c:out value='${selectedMajor}'/>">
                    <input type="hidden" name="mainClass" value="<c:out value='${selectedMainClass}'/>">
                    <input type="hidden" name="gradingStatus" value="<c:out value='${selectedGradingStatus}'/>">
                    <label for="grading-page-size">Hiển thị</label>
                    <select id="grading-page-size" name="pageSize" onchange="this.form.submit()">
                        <c:forEach var="size" items="${pageSizes}">
                            <option value="${size}" ${pageSize == size ? 'selected' : ''}>${size}</option>
                        </c:forEach>
                    </select>
                    <span>bản ghi mỗi trang</span>
                </form>
                <span class="activity-pagination-summary">
                    Hiển thị <c:out value="${firstParticipantNumber}"/>–<c:out value="${lastParticipantNumber}"/>
                    trong <c:out value="${filteredParticipantCount}"/> sinh viên
                </span>
                <c:if test="${totalPages > 1}">
                    <nav class="activity-pagination-nav" aria-label="Phân trang danh sách chấm điểm tham gia">
                        <c:choose>
                            <c:when test="${currentPage > 1}">
                                <a class="activity-page-link" href="<c:out value='${pageBaseUrl}&page=${currentPage - 1}'/>"
                                   aria-label="Trang trước">‹</a>
                            </c:when>
                            <c:otherwise><span class="activity-page-link is-disabled" aria-hidden="true">‹</span></c:otherwise>
                        </c:choose>
                        <c:forEach var="pageNumber" begin="1" end="${totalPages}">
                            <a class="activity-page-link${pageNumber == currentPage ? ' is-active' : ''}"
                               href="<c:out value='${pageBaseUrl}&page=${pageNumber}'/>"
                               aria-current="${pageNumber == currentPage ? 'page' : ''}"><c:out value="${pageNumber}"/></a>
                        </c:forEach>
                        <c:choose>
                            <c:when test="${currentPage < totalPages}">
                                <a class="activity-page-link" href="<c:out value='${pageBaseUrl}&page=${currentPage + 1}'/>"
                                   aria-label="Trang tiếp theo">›</a>
                            </c:when>
                            <c:otherwise><span class="activity-page-link is-disabled" aria-hidden="true">›</span></c:otherwise>
                        </c:choose>
                    </nav>
                </c:if>
            </div>
        </c:if>
    </section>

    <c:if test="${canTakeAttendance}">
        <dialog id="single-grade-modal" class="attendance-grade-modal" aria-labelledby="single-grade-modal-title">
            <form id="single-grade-modal-form" class="attendance-grade-modal__form" method="dialog" novalidate>
                <input type="hidden" id="single-grade-participant-id" name="participantId" value="">
                <div class="attendance-grade-modal__header">
                    <div>
                        <span class="attendance-grade-modal__eyebrow">ĐÁNH GIÁ THAM GIA</span>
                        <h3 id="single-grade-modal-title">Chấm điểm &amp; Nhận xét</h3>
                        <p class="attendance-grade-modal__student" id="single-grade-student-info">—</p>
                    </div>
                    <button type="button" class="attendance-grade-modal__close" data-close-grade-modal aria-label="Đóng hộp thoại">×</button>
                </div>
                <div class="attendance-grade-modal__body">
                    <div id="single-grade-modal-error" class="alert alert-error" role="alert" hidden></div>
                    <div class="activity-filter-field">
                        <label for="single-grade-eval-score">Điểm đánh giá tham gia (0 – 5 điểm) <span class="required-mark">*</span></label>
                        <input id="single-grade-eval-score" type="number" name="evalScore" min="0" max="5" step="0.1" placeholder="Nhập điểm từ 0 đến 5 (ví dụ: 4.8)" required>
                        <small class="text-muted">Điểm tham gia = Điểm chuyên cần trung bình + Điểm đánh giá tham gia.</small>
                    </div>
                    <div class="activity-filter-field">
                        <label for="single-grade-eval-feedback">Nhận xét / Đánh giá chi tiết</label>
                        <textarea id="single-grade-eval-feedback" name="feedback" rows="4" maxlength="1000"
                                  placeholder="Nhập nhận xét về thái độ, mức độ đóng góp của sinh viên trong hoạt động..."></textarea>
                    </div>
                </div>
                <div class="attendance-grade-modal__footer">
                    <button type="button" class="button button-sm button-outline" data-close-grade-modal>Hủy</button>
                    <button type="submit" class="button button-sm button-primary" id="single-grade-submit-btn">Lưu đánh giá</button>
                </div>
            </form>
        </dialog>
    </c:if>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261109-4"></script>
<script src="${pageContext.request.contextPath}/assets/js/attendance.js?v=20261110-1"></script>
</body>
</html>
