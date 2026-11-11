<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<% request.setAttribute("activeNav", "checklist"); %>
<c:url var="checklistUrl" value="/checklist">
    <c:if test="${formActivityId > 0}">
        <c:param name="activityId" value="${formActivityId}"/>
    </c:if>
</c:url>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${isEdit ? 'Chỉnh sửa' : 'Tạo'}"/> công việc checklist | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/checklist.css?v=20261106-3">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <div class="activity-app-main">
            <main class="page checklist-page">
                <header class="checklist-header">
                    <div>
                        <p class="checklist-eyebrow">QUẢN LÝ CÔNG VIỆC</p>
                        <h1><c:choose><c:when test="${isEdit}">Chỉnh sửa công việc checklist</c:when>
                            <c:otherwise>Tạo công việc checklist</c:otherwise></c:choose></h1>
                        <p class="subtitle">
                            <c:choose>
                                <c:when test="${isChecklistAdmin}">Nhập thông tin và chọn nhân sự được phân công.</c:when>
                                <c:otherwise>Công việc mới sẽ được tự động giao cho bạn.</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                    <a class="checklist-back-link" href="${checklistUrl}">Quay lại checklist</a>
                </header>

                <c:if test="${not empty errorMessage}">
                    <div class="checklist-error" role="alert"><c:out value="${errorMessage}"/></div>
                </c:if>

                <form class="checklist-form" method="post" action="${pageContext.request.contextPath}/checklist">
                    <input type="hidden" name="action" value="${isEdit ? 'update' : 'create'}">
                    <c:if test="${isEdit}">
                        <input type="hidden" name="id" value="${taskId}">
                    </c:if>
                    <section class="checklist-form-section">
                        <h2>Thông tin công việc</h2>
                        <div class="checklist-form-grid">
                            <div class="checklist-field">
                                <label for="activityId">Hoạt động <span>*</span></label>
                                <select id="activityId" name="activityId" required>
                                    <option value="">-- Chọn hoạt động --</option>
                                    <c:forEach var="activity" items="${activityOptions}">
                                        <option value="${activity.id}"
                                            ${formActivityId == activity.id ? 'selected' : ''}>
                                            <c:out value="${activity.code}"/> - <c:out value="${activity.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="checklist-field">
                                <label for="phase">Giai đoạn <span>*</span></label>
                                <select id="phase" name="phase" required>
                                    <option value="">-- Chọn giai đoạn --</option>
                                    <option value="BEFORE_EVENT" ${formPhase == 'BEFORE_EVENT' ? 'selected' : ''}>
                                        Trước hoạt động
                                    </option>
                                    <option value="DURING_EVENT" ${formPhase == 'DURING_EVENT' ? 'selected' : ''}>
                                        Trong hoạt động
                                    </option>
                                    <option value="AFTER_EVENT" ${formPhase == 'AFTER_EVENT' ? 'selected' : ''}>
                                        Sau hoạt động
                                    </option>
                                </select>
                            </div>
                            <div class="checklist-field">
                                <label for="priority">Mức độ ưu tiên <span>*</span></label>
                                <select id="priority" name="priority" required>
                                    <option value="LOW" ${formPriority == 'LOW' ? 'selected' : ''}>Thấp</option>
                                    <option value="MEDIUM" ${formPriority == 'MEDIUM' ? 'selected' : ''}>Trung bình</option>
                                    <option value="HIGH" ${formPriority == 'HIGH' ? 'selected' : ''}>Cao</option>
                                </select>
                            </div>
                            <div class="checklist-field">
                                <label for="code">Mã công việc <span>*</span></label>
                                <input id="code" name="code" type="text" maxlength="50" required
                                       value="<c:out value='${formCode}'/>" placeholder="Ví dụ: TASK-001">
                            </div>
                            <div class="checklist-field">
                                <label for="name">Tên công việc <span>*</span></label>
                                <input id="name" name="name" type="text" maxlength="255" required
                                       value="<c:out value='${formName}'/>" placeholder="Nhập tên công việc">
                            </div>
                            <div class="checklist-field">
                                <label for="deadline">Deadline <span>*</span></label>
                                <input id="deadline" name="deadline" type="datetime-local" required
                                       value="<c:out value='${formDeadline}'/>">
                            </div>
                            <div class="checklist-field checklist-field-wide">
                                <span class="checklist-prerequisite-label">Công việc tiên quyết</span>
                                <button class="checklist-prerequisite-button" type="button"
                                        data-open-prerequisites>Chọn công việc tiên quyết</button>
                                <p class="checklist-prerequisite-selection" data-prerequisite-selection>
                                    Chưa chọn công việc tiên quyết.
                                </p>
                                <small data-no-prerequisite-options hidden>
                                    Hoạt động này chưa có công việc nào khác để chọn làm tiên quyết.
                                </small>
                                <dialog class="checklist-prerequisite-dialog" data-prerequisite-dialog>
                                    <div class="checklist-prerequisite-dialog-content">
                                        <div class="checklist-prerequisite-dialog-header">
                                            <div>
                                                <h2>Chọn công việc tiên quyết</h2>
                                                <p>Chỉ có thể chọn công việc thuộc cùng hoạt động.</p>
                                            </div>
                                            <button type="button" class="checklist-quick-assign-close"
                                                    aria-label="Đóng" data-close-prerequisites>&times;</button>
                                        </div>
                                        <label class="checklist-prerequisite-search-label" for="prerequisiteSearch">
                                            Tìm công việc
                                        </label>
                                        <input id="prerequisiteSearch" class="checklist-assignee-search"
                                               type="search" data-prerequisite-search
                                               placeholder="Nhập mã hoặc tên công việc">
                                        <div class="checklist-prerequisite-options" data-prerequisite-options>
                                            <c:forEach var="candidateTask" items="${taskOptions}">
                                                <label class="checklist-prerequisite-option"
                                                       data-prerequisite-option>
                                                    <input type="checkbox" name="prerequisiteTaskIds"
                                                           value="${candidateTask.id}"
                                                           data-task-activity="${candidateTask.activityId}"
                                                           data-task-id="${candidateTask.id}"
                                                           data-task-status="${candidateTask.statusValue}"
                                                        ${selectedPrerequisiteTaskIds.contains(candidateTask.id)
                                                            ? 'checked' : ''}>
                                                    <span>
                                                        <strong><c:out value="${candidateTask.code}"/></strong>
                                                        <small><c:out value="${candidateTask.name}"/></small>
                                                    </span>
                                                </label>
                                            </c:forEach>
                                        </div>
                                        <p class="checklist-no-search-results"
                                           data-no-prerequisite-results hidden>
                                            Không tìm thấy công việc phù hợp.
                                        </p>
                                        <div class="checklist-prerequisite-dialog-actions">
                                            <button type="button" class="checklist-create-button"
                                                    data-close-prerequisites>Hoàn tất chọn</button>
                                        </div>
                                    </div>
                                </dialog>
                            </div>
                            <div class="checklist-field">
                                <c:choose>
                                    <c:when test="${isChecklistAdmin}">
                                        <label for="status">Trạng thái <span>*</span></label>
                                        <select id="status" name="status" required>
                                            <option value="Chưa hoàn thành" ${formStatus == 'Chưa hoàn thành' ? 'selected' : ''}>Chưa hoàn thành</option>
                                            <option value="Đang hoàn thành" data-advanced-status ${formStatus == 'Đang hoàn thành' ? 'selected' : ''}>Đang hoàn thành</option>
                                            <option value="Đã hoàn thành" data-advanced-status ${formStatus == 'Đã hoàn thành' ? 'selected' : ''}>Đã hoàn thành</option>
                                        </select>
                                        <small data-prerequisite-status-help hidden>
                                            Chỉ có thể bắt đầu công việc sau khi tất cả công việc tiên quyết hoàn thành.
                                        </small>
                                    </c:when>
                                    <c:otherwise>
                                        <label>Trạng thái</label>
                                        <p>Chưa hoàn thành</p>
                                        <input type="hidden" name="status" value="Chưa hoàn thành">
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </section>

                    <c:if test="${isChecklistAdmin}">
                        <section class="checklist-form-section">
                            <div class="checklist-assignee-heading">
                                <div>
                                    <h2>Phân công nhân sự</h2>
                                    <p>Có thể chọn nhiều nhân viên và đối tác cho cùng một công việc.</p>
                                </div>
                            </div>
                            <div class="checklist-assignee-grid">
                            <fieldset class="checklist-assignee-group">
                                <legend>Nhân viên nội bộ</legend>
                                <c:choose>
                                    <c:when test="${empty staffOptions}">
                                        <p class="checklist-no-assignees">Không có nhân viên đang hoạt động.</p>
                                    </c:when>
                                    <c:otherwise>
                                        <label class="checklist-assignee-search-label" for="staffSearch">Tìm nhân viên</label>
                                        <input class="checklist-assignee-search" id="staffSearch" type="search"
                                               data-assignee-search="staff" placeholder="Nhập tên hoặc mã nhân viên">
                                        <div class="checklist-table-wrap checklist-assignee-table-wrap"
                                             data-assignee-list="staff">
                                            <table class="checklist-table checklist-assignee-table">
                                                <thead><tr>
                                                    <th>Chọn</th><th>STT</th><th>Mã nhân sự</th><th>Họ và tên</th>
                                                    <th>Trạng thái nhân sự</th><th>Chức vụ</th><th>Thời gian phân công</th>
                                                </tr></thead>
                                                <tbody>
                                                <c:forEach var="staff" items="${staffOptions}" varStatus="row">
                                                    <tr data-assignee-option>
                                                        <td><input aria-label="Chọn nhân sự"
                                                                   type="checkbox" name="staffIds" value="${staff.id}"
                                                            <c:if test="${selectedStaffIds.contains(staff.id)}">checked</c:if>></td>
                                                        <td><c:out value="${row.index + 1}"/></td>
                                                        <td><c:out value="${staff.code}"/></td>
                                                        <td><c:out value="${staff.name}"/></td>
                                                        <td><c:out value="${staff.statusDisplay}"/></td>
                                                        <td><c:out value="${empty staff.position ? 'Chưa cập nhật' : staff.position}"/></td>
                                                        <td><c:out value="${staff.assignedAtDisplay}"/></td>
                                                    </tr>
                                                </c:forEach>
                                                </tbody>
                                            </table>
                                        </div>
                                        <p class="checklist-no-search-results" data-no-assignee-results="staff" hidden>
                                            Không tìm thấy nhân viên phù hợp.
                                        </p>
                                    </c:otherwise>
                                </c:choose>
                            </fieldset>

                            <fieldset class="checklist-assignee-group">
                                <legend>Nhân viên đối tác</legend>
                                <c:choose>
                                    <c:when test="${empty partnerStaffOptions}">
                                        <p class="checklist-no-assignees">Không có nhân viên đối tác đang hoạt động.</p>
                                    </c:when>
                                    <c:otherwise>
                                        <label class="checklist-assignee-search-label" for="partnerStaffSearch">Tìm nhân viên đối tác</label>
                                        <input class="checklist-assignee-search" id="partnerStaffSearch" type="search"
                                               data-assignee-search="partner" placeholder="Nhập tên, mã hoặc công ty">
                                        <div class="checklist-table-wrap checklist-assignee-table-wrap"
                                             data-assignee-list="partner">
                                            <table class="checklist-table checklist-assignee-table">
                                                <thead><tr>
                                                    <th>Chọn</th><th>STT</th><th>Mã đối tác</th><th>Họ và tên</th>
                                                    <th>Công ty</th><th>Trạng thái nhân sự</th><th>Chức vụ</th>
                                                    <th>Thời gian phân công</th>
                                                </tr></thead>
                                                <tbody>
                                                <c:forEach var="partnerStaff" items="${partnerStaffOptions}" varStatus="row">
                                                    <tr data-assignee-option>
                                                        <td><input aria-label="Chọn nhân sự đối tác"
                                                                   type="checkbox" name="partnerStaffIds" value="${partnerStaff.id}"
                                                            <c:if test="${selectedPartnerStaffIds.contains(partnerStaff.id)}">checked</c:if>></td>
                                                        <td><c:out value="${row.index + 1}"/></td>
                                                        <td><c:out value="${partnerStaff.code}"/></td>
                                                        <td><c:out value="${partnerStaff.name}"/></td>
                                                        <td><c:out value="${empty partnerStaff.companyName ? 'Chưa cập nhật' : partnerStaff.companyName}"/></td>
                                                        <td><c:out value="${partnerStaff.statusDisplay}"/></td>
                                                        <td><c:out value="${empty partnerStaff.position ? 'Chưa cập nhật' : partnerStaff.position}"/></td>
                                                        <td><c:out value="${partnerStaff.assignedAtDisplay}"/></td>
                                                    </tr>
                                                </c:forEach>
                                                </tbody>
                                            </table>
                                        </div>
                                        <p class="checklist-no-search-results" data-no-assignee-results="partner" hidden>
                                            Không tìm thấy nhân viên đối tác phù hợp.
                                        </p>
                                    </c:otherwise>
                                </c:choose>
                            </fieldset>
                            </div>
                        </section>
                    </c:if>

                    <div class="checklist-form-actions">
                        <a class="checklist-cancel-button" href="${checklistUrl}">Hủy</a>
                        <button class="checklist-create-button" type="submit">
                            <c:choose><c:when test="${isEdit}">Lưu thay đổi</c:when>
                                <c:otherwise>Tạo công việc</c:otherwise></c:choose>
                        </button>
                    </div>
                </form>
            </main>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/checklist-form.js?v=20261111-2" defer></script>
</body>
</html>
