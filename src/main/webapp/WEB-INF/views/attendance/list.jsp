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
    <title>EAMS | Điểm danh sinh viên</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/attendance.css?v=9">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> ĐIỂM DANH SINH VIÊN</span>
                    <h1>Điểm danh sinh viên</h1>
                    <p>Chọn hoạt động để xem danh sách sinh viên tham gia và cập nhật trạng thái điểm danh.</p>
                </div>
            </header>

    <c:if test="${not empty sessionScope.successMessage}">
        <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
        <c:remove var="successMessage" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.errorMessage}">
        <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
        <c:remove var="errorMessage" scope="session"/>
    </c:if>

    <section class="card activity-table-card" aria-labelledby="attendance-table-title">
        <div class="activity-table-heading">
            <div>
                <h2 id="attendance-table-title">Danh sách điểm danh</h2>
                <p>
                    <c:choose>
                        <c:when test="${not empty selectedActivity}">
                            <c:out value="${selectedActivity.code}"/> – <c:out value="${selectedActivity.name}"/>
                        </c:when>
                        <c:otherwise>Sinh viên tham gia hoạt động ngoại khóa</c:otherwise>
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
            <form class="activity-filter attendance-filter" action="${pageContext.request.contextPath}/attendance" method="get">
                <div class="activity-filter-field attendance-filter-activity">
                    <label for="attendance-activity">Hoạt động</label>
                    <select id="attendance-activity" name="activityId" onchange="this.form.submit()">
                        <option value="">-- Chọn hoạt động --</option>
                        <c:forEach var="activity" items="${activities}">
                            <option value="${activity.id}" ${selectedActivityId == activity.id ? 'selected' : ''}>
                                <c:out value="${activity.code}"/> – <c:out value="${activity.name}"/><c:if test="${not empty activity.semesterName}"> (<c:out value="${activity.semesterName}"/>)</c:if>
                            </option>
                        </c:forEach>
                    </select>
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
                    <span class="attendance-summary-label">Thời gian bắt đầu</span>
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
                    <span class="attendance-summary-label">Thời gian kết thúc</span>
                    <span class="activity-time">
                        <c:choose>
                            <c:when test="${not empty selectedActivity.endTime}">
                                ${dt:format(selectedActivity.endTime)}
                            </c:when>
                            <c:otherwise>Chưa xác định</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <div class="attendance-summary-item">
                    <span class="attendance-summary-label">Trạng thái hoạt động</span>
                    <span class="badge status-pill ${st:css(selectedActivity.activityStatus)}">${st:label(selectedActivity.activityStatus)}</span>
                </div>
                <ul class="attendance-counts" aria-label="Thống kê điểm danh">
                    <c:forEach var="status" items="${participantStatuses}">
                        <li class="badge status-pill ${st:css(status)}">${st:label(status)}: <strong data-status-count="${status}"><c:out value="${statusCounts[status]}"/></strong></li>
                    </c:forEach>
                </ul>
            </div>

            <%-- Hidden filter fields shared by every POST form, so the redirect keeps the current filters. --%>
            <c:set var="filterHiddenFields">
                <input type="hidden" name="activityId" value="${selectedActivity.id}">
                <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                <input type="hidden" name="department" value="<c:out value='${selectedDepartment}'/>">
                <input type="hidden" name="academicProgram" value="<c:out value='${selectedAcademicProgram}'/>">
                <input type="hidden" name="major" value="<c:out value='${selectedMajor}'/>">
                <input type="hidden" name="mainClass" value="<c:out value='${selectedMainClass}'/>">
                <input type="hidden" name="attendanceStatus" value="<c:out value='${selectedAttendanceStatus}'/>">
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
                <form class="activity-filter" action="${pageContext.request.contextPath}/attendance" method="get">
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
                        <label for="filter-attendance-status">Trạng thái điểm danh</label>
                        <select id="filter-attendance-status" name="attendanceStatus">
                            <option value="">Tất cả</option>
                            <c:forEach var="status" items="${participantStatuses}">
                                <option value="${status}" ${selectedAttendanceStatus == status.name() ? 'selected' : ''}>${st:label(status)}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="activity-filter-buttons">
                        <button type="submit" class="button button-primary button-sm">Tìm kiếm &amp; Lọc</button>
                        <c:if test="${filtered}">
                            <a class="button button-sm" href="${pageContext.request.contextPath}/attendance?activityId=${selectedActivity.id}&amp;pageSize=${pageSize}">Xóa Bộ lọc</a>
                        </c:if>
                    </div>
                </form>
            </details>

            <div id="attendance-ajax-alert" class="alert alert-error activity-alert attendance-ajax-alert" role="alert" hidden></div>

            <c:choose>
                <c:when test="${canTakeAttendance && not empty participants}">
                    <form id="attendance-form" class="activity-bulk-toolbar" action="${pageContext.request.contextPath}/attendance" method="post"
                          data-attendance-autosave data-activity-id="${selectedActivity.id}">
                        <input type="hidden" name="action" value="save">
                        ${filterHiddenFields}
                        <span class="activity-bulk-selection" data-attendance-selection-hint>
                            Tick chọn sinh viên để thao tác hàng loạt<c:if test="${filtered || totalPages > 1}"> (đang hiển thị <c:out value="${participants.size()}"/> sinh viên<c:if test="${filtered}"> theo bộ lọc</c:if><c:if test="${totalPages > 1}"> ở trang <c:out value="${currentPage}"/></c:if>)</c:if>.
                        </span>
                        <div class="activity-bulk-actions">
                            <button type="button" class="button button-sm action-btn action-btn--bulk-approve" data-bulk-status
                                    data-to="ATTENDED" data-label="Có mặt" disabled hidden style="display: none;">
                                <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></svg></span>
                                Có mặt (<span data-bulk-count>0</span>)
                            </button>
                            <button type="button" class="button button-sm action-btn button-revoke" data-bulk-status
                                    data-to="NOT_YET" data-from="ATTENDED,IN_LATE,OUT_EARLY" data-label="Hủy có mặt" disabled hidden style="display: none;">
                                <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 7v6h6"/><path d="M5.5 13a8 8 0 1 0 2-7L3 11"/></svg></span>
                                Hủy có mặt (<span data-bulk-count>0</span>)
                            </button>
                            <button type="button" class="button button-sm action-btn action-btn--bulk-reject" data-bulk-status
                                    data-to="ABSENT" data-label="Vắng" disabled hidden style="display: none;">
                                <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg></span>
                                Vắng mặt (<span data-bulk-count>0</span>)
                            </button>
                            <button type="button" class="button button-sm action-btn button-revoke" data-bulk-status
                                    data-to="NOT_YET" data-from="ABSENT" data-label="Hủy vắng" disabled hidden style="display: none;">
                                <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 7v6h6"/><path d="M5.5 13a8 8 0 1 0 2-7L3 11"/></svg></span>
                                Hủy vắng (<span data-bulk-count>0</span>)
                            </button>
                            <details class="action-menu">
                                <summary aria-label="Thao tác cho tất cả sinh viên đang hiển thị"><span aria-hidden="true">⋯</span></summary>
                                <div class="action-menu__items">
                                    <button type="button" class="action-menu-item action-menu-item--approve" data-mark-all-status="ATTENDED"
                                            data-label="Đánh dấu tất cả có mặt">
                                        <span class="action-menu-item__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m5 12 4 4L19 6"/></svg></span>
                                        Đánh dấu tất cả có mặt
                                    </button>
                                    <button type="button" class="action-menu-item action-menu-item--reject" data-mark-all-status="ABSENT"
                                            data-label="Đánh dấu tất cả vắng mặt">
                                        <span class="action-menu-item__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m18 6-12 12"/><path d="m6 6 12 12"/></svg></span>
                                        Đánh dấu tất cả vắng mặt
                                    </button>
                                </div>
                            </details>
                        </div>
                    </form>
                </c:when>
                <c:when test="${not canTakeAttendance}">
                    <p class="attendance-note">Chỉ điểm danh khi hoạt động đang diễn ra hoặc đã hoàn thành.</p>
                </c:when>
            </c:choose>

            <c:if test="${canManageParticipants}">
                <form id="remove-participants-form" class="activity-bulk-toolbar" action="${pageContext.request.contextPath}/attendance" method="post">
                    <input type="hidden" name="action" value="remove">
                    ${filterHiddenFields}
                    <div data-remove-inputs hidden></div>
                    <span class="activity-bulk-selection" data-remove-count>
                        <c:choose>
                            <c:when test="${not empty participants}">
                                Chọn sinh viên chưa điểm danh để xóa. Muốn xóa theo khoa, ngành, chương trình đào tạo hoặc lớp: lọc trước rồi chọn tất cả.
                            </c:when>
                            <c:otherwise>Thêm từng sinh viên hoặc thêm cả nhóm theo khoa, ngành, chương trình đào tạo, lớp.</c:otherwise>
                        </c:choose>
                    </span>
                    <div class="activity-bulk-actions">
                        <a class="button button-primary button-sm action-btn" data-add-toggle
                           aria-controls="attendance-add-panel" aria-expanded="${openAddPanel ? 'true' : 'false'}"
                           href="${pageContext.request.contextPath}/attendance?action=add&amp;activityId=${selectedActivity.id}<c:out value='${filterQuery}${pagingQuery}'/>">
                            <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M12 5v14"/><path d="M5 12h14"/></svg></span>
                            Thêm sinh viên
                        </a>
                        <c:if test="${not empty participants}">
                            <button type="submit" class="button button-sm action-btn action-btn--bulk-reject" data-remove-submit
                                    data-confirm="Xóa các sinh viên chưa điểm danh đã chọn khỏi danh sách?" disabled>
                                <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg></span>
                                Xóa (<span data-remove-submit-count>0</span>)
                            </button>
                        </c:if>
                    </div>
                </form>

                <div id="attendance-add-panel" class="attendance-add-panel" ${openAddPanel ? '' : 'hidden'}>
                    <div class="attendance-add-panel-heading">
                        <h3>Thêm sinh viên vào danh sách điểm danh</h3>
                        <span class="activity-count">
                            <c:out value="${totalParticipantCount}"/><c:if test="${not empty selectedActivity.maxParticipants}"> / <c:out value="${selectedActivity.maxParticipants}"/></c:if> sinh viên
                        </span>
                        <button type="button" class="button button-sm button-outline" data-add-close>Đóng</button>
                    </div>
                    <c:choose>
                        <c:when test="${empty candidates}">
                            <p class="attendance-add-empty">Tất cả sinh viên đang hoạt động đã có trong danh sách.</p>
                        </c:when>
                        <c:otherwise>
                            <form class="attendance-add-form" action="${pageContext.request.contextPath}/attendance" method="post">
                                <input type="hidden" name="action" value="addStudents">
                                ${filterHiddenFields}
                                <h3>Thêm từng sinh viên</h3>
                                <p class="attendance-add-hint">Tìm theo mã hoặc họ tên, có thể chọn nhiều sinh viên.</p>
                                <span id="add-student-label" class="visually-hidden">Sinh viên cần thêm</span>
                                <div class="multi-select-combobox" data-multi-select data-required
                                     data-item-label="sinh viên" data-input-name="studentId">
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="add-student-options" aria-labelledby="add-student-label">
                                        <span data-selection-label>Chọn sinh viên</span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="add-student-options" hidden>
                                        <input class="multi-select-search" type="search" placeholder="Tìm sinh viên..." aria-label="Tìm sinh viên">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn sinh viên</legend>
                                            <c:forEach var="candidate" items="${candidates}">
                                                <label class="multi-select-option">
                                                    <input type="checkbox" name="studentId" value="${candidate.studentId}">
                                                    <span><c:out value="${candidate.studentCode}"/> – <c:out value="${candidate.studentName}"/><c:if test="${not empty candidate.mainClassCode}"> (<c:out value="${candidate.mainClassCode}"/>)</c:if></span>
                                                </label>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy sinh viên phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Chưa chọn sinh viên</span>
                                </div>
                                <div class="attendance-add-actions">
                                    <button type="submit" class="button button-primary button-sm">Thêm đã chọn</button>
                                </div>
                            </form>

                            <form class="attendance-add-form" action="${pageContext.request.contextPath}/attendance" method="post" data-group-form>
                                <input type="hidden" name="action" value="addGroup">
                                ${filterHiddenFields}
                                <h3>Thêm theo nhóm</h3>
                                <p class="attendance-add-hint">Thêm tất cả sinh viên chưa có trong danh sách thuộc nhóm đã chọn.</p>
                                <div class="attendance-group-fields">
                                    <div class="activity-filter-field">
                                        <label for="group-department">Khoa</label>
                                        <select id="group-department" name="groupDepartmentId" data-group-level="department">
                                            <option value="">Tất cả</option>
                                            <c:forEach var="item" items="${groupDepartments}">
                                                <option value="${item.id}" data-count="${item.count}"><c:out value="${item.name}"/> (${item.count})</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                    <div class="activity-filter-field">
                                        <label for="group-major">Ngành</label>
                                        <select id="group-major" name="groupMajorId" data-group-level="major">
                                            <option value="">Tất cả</option>
                                            <c:forEach var="item" items="${groupMajors}">
                                                <option value="${item.id}" data-count="${item.count}" data-department="${item.departmentId}"><c:out value="${item.name}"/> (${item.count})</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                    <div class="activity-filter-field">
                                        <label for="group-program">Chương trình đào tạo</label>
                                        <select id="group-program" name="groupProgramId" data-group-level="program">
                                            <option value="">Tất cả</option>
                                            <c:forEach var="item" items="${groupPrograms}">
                                                <option value="${item.id}" data-count="${item.count}" data-department="${item.departmentId}"
                                                        data-major="${item.majorId}"><c:out value="${item.name}"/> (${item.count})</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                    <div class="activity-filter-field">
                                        <label for="group-class">Lớp</label>
                                        <select id="group-class" name="groupClassId" data-group-level="class">
                                            <option value="">Tất cả</option>
                                            <c:forEach var="item" items="${groupClasses}">
                                                <option value="${item.id}" data-count="${item.count}" data-department="${item.departmentId}"
                                                        data-major="${item.majorId}" data-program="${item.programId}"><c:out value="${item.name}"/> (${item.count})</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </div>
                                <div class="attendance-add-actions">
                                    <span class="attendance-group-count" data-group-count aria-live="polite">Chọn khoa, ngành, chương trình đào tạo hoặc lớp</span>
                                    <button type="submit" class="button button-primary button-sm" data-group-submit
                                            data-confirm="Thêm sinh viên theo nhóm đã chọn?" disabled>Thêm theo nhóm</button>
                                </div>
                            </form>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:if>
        </c:if>

        <c:set var="showSelectColumn" value="${canManageParticipants or canTakeAttendance}"/>
        <c:set var="columnCount" value="${showSelectColumn ? 8 : 7}"/>
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
                    <th scope="col">Mã SV</th>
                    <th scope="col">Họ tên</th>
                    <th scope="col">Khoa</th>
                    <th scope="col">Ngành</th>
                    <th scope="col">Chương trình đào tạo</th>
                    <th scope="col">Lớp</th>
                    <th scope="col">Trạng thái điểm danh</th>
                </tr>
                </thead>
                <tbody>
                <c:choose>
                    <c:when test="${empty selectedActivity}">
                        <tr>
                            <td class="empty-state activity-empty-state" colspan="${columnCount}">
                                <span class="empty-state-mark" aria-hidden="true">✓</span>
                                <strong>Chọn hoạt động để xem danh sách điểm danh</strong>
                                <span>Danh sách sinh viên tham gia sẽ hiển thị tại đây.</span>
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
                                               data-removable="${p.removable ? 'true' : 'false'}"
                                               aria-label="Chọn <c:out value='${p.studentName}'/>"
                                               title="${p.removable ? 'Chọn sinh viên' : 'Sinh viên đã được điểm danh (chỉ có thể thao tác điểm danh, không thể xóa khỏi danh sách)'}">
                                    </td>
                                </c:if>
                                <td><span class="activity-code"><c:out value="${p.studentCode}" default="—"/></span></td>
                                <td><strong class="activity-name"><c:out value="${p.studentName}" default="—"/></strong></td>
                                <td><c:out value="${p.departmentName}" default="—"/></td>
                                <td><c:out value="${p.majorName}" default="—"/></td>
                                <td><c:out value="${p.academicProgramName}" default="—"/></td>
                                <td><c:out value="${p.mainClassCode}" default="—"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${canTakeAttendance}">
                                            <div class="attendance-status-cell">
                                                <select class="attendance-status-select ${st:css(p.attendanceStatus)}" name="status_${p.participantId}"
                                                        form="attendance-form" data-attendance-select
                                                        data-participant-id="${p.participantId}"
                                                        data-saved-value="${p.attendanceStatus}"
                                                        aria-label="Trạng thái điểm danh của <c:out value='${p.studentName}'/>">
                                                    <c:forEach var="status" items="${participantStatuses}">
                                                        <option value="${status}" class="${st:css(status)}" ${p.attendanceStatus == status ? 'selected' : ''}>${st:label(status)}</option>
                                                    </c:forEach>
                                                </select>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge status-pill ${st:css(p.attendanceStatus)}">${st:label(p.attendanceStatus)}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
                </tbody>
            </table>
        </div>
        <c:if test="${not empty selectedActivity && filteredParticipantCount > 0}">
            <c:set var="pageBaseUrl" value="${pageContext.request.contextPath}/attendance?activityId=${selectedActivity.id}&pageSize=${pageSize}${filterQuery}"/>
            <div class="activity-pagination">
                <form class="activity-page-size" action="${pageContext.request.contextPath}/attendance" method="get">
                    <input type="hidden" name="activityId" value="${selectedActivity.id}">
                    <input type="hidden" name="page" value="1">
                    <input type="hidden" name="keyword" value="<c:out value='${keyword}'/>">
                    <input type="hidden" name="department" value="<c:out value='${selectedDepartment}'/>">
                    <input type="hidden" name="academicProgram" value="<c:out value='${selectedAcademicProgram}'/>">
                    <input type="hidden" name="major" value="<c:out value='${selectedMajor}'/>">
                    <input type="hidden" name="mainClass" value="<c:out value='${selectedMainClass}'/>">
                    <input type="hidden" name="attendanceStatus" value="<c:out value='${selectedAttendanceStatus}'/>">
                    <label for="attendance-page-size">Hiển thị</label>
                    <select id="attendance-page-size" name="pageSize" onchange="this.form.submit()">
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
                    <nav class="activity-pagination-nav" aria-label="Phân trang danh sách điểm danh">
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
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261020-2"></script>
<script src="${pageContext.request.contextPath}/assets/js/attendance.js?v=8"></script>
</body>
</html>
