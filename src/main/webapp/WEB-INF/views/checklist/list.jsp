<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<% request.setAttribute("activeNav", "checklist"); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Checklist công việc | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/checklist.css?v=20261105-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css">
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
                        <p class="checklist-eyebrow">CÔNG VIỆC ĐƯỢC GIAO</p>
                        <h1>Checklist công việc</h1>
                        <p class="subtitle">Chọn hoạt động để xem danh sách checklist tương ứng.</p>
                    </div>
                    <div class="checklist-header-actions">
                        <c:choose>
                            <c:when test="${selectedActivityId > 0}">
                                <span class="checklist-count"><c:out value="${fn:length(checklistTasks)}"/> công việc</span>
                            </c:when>
                            <c:otherwise><span class="checklist-count">Chưa chọn hoạt động</span></c:otherwise>
                        </c:choose>
                        <a class="checklist-create-button"
                           href="${pageContext.request.contextPath}/checklist?action=create&amp;activityId=${selectedActivityId}">
                            <span aria-hidden="true">+</span> Tạo công việc
                        </a>
                    </div>
                </header>

                <c:if test="${param.created == '1'}">
                    <div class="checklist-success" role="status">Đã tạo công việc checklist thành công.</div>
                </c:if>
                <c:if test="${param.updated == '1'}">
                    <div class="checklist-success" role="status">Đã cập nhật công việc checklist.</div>
                </c:if>
                <c:if test="${param.deleted == '1'}">
                    <div class="checklist-success" role="status">Đã xóa công việc checklist.</div>
                </c:if>
                <c:if test="${param.notFound == '1'}">
                    <div class="checklist-error" role="alert">Công việc không tồn tại hoặc đã được xóa.</div>
                </c:if>
                <c:if test="${param.statusUpdated == '1'}">
                    <div class="checklist-success" role="status">Đã cập nhật trạng thái công việc.</div>
                </c:if>
                <c:if test="${param.assigned == '1'}">
                    <div class="checklist-success" role="status">Đã phân công nhanh nhân sự cho công việc.</div>
                </c:if>
                <c:if test="${param.quickAssignError == '1'}">
                    <div class="checklist-error" role="alert">
                        <c:choose>
                            <c:when test="${param.reason == 'empty'}">Chọn ít nhất một nhân viên hoặc nhân viên đối tác.</c:when>
                            <c:when test="${param.reason == 'assignee'}">Nhân sự không hợp lệ hoặc không còn hoạt động.</c:when>
                            <c:otherwise>Không thể lưu phân công nhanh. Vui lòng kiểm tra công việc và thử lại.</c:otherwise>
                        </c:choose>
                    </div>
                </c:if>

                <form class="checklist-activity-filter" method="get"
                      action="${pageContext.request.contextPath}/checklist">
                    <label for="activityId">Chọn hoạt động</label>
                    <select id="activityId" name="activityId" required
                            onchange="if (this.value) this.form.submit()">
                        <option value="">-- Chọn hoạt động --</option>
                        <c:forEach var="activity" items="${activities}">
                            <option value="${activity.id}"
                                ${selectedActivityId == activity.id ? 'selected' : ''}>
                                <c:out value="${activity.code}"/> - <c:out value="${activity.name}"/>
                            </option>
                        </c:forEach>
                    </select>
                    <c:if test="${selectedActivityId > 0}">
                        <button type="button" data-open-quick-assign>Phân công nhanh</button>
                    </c:if>
                </form>

                <c:if test="${selectedActivityId > 0}">
                    <dialog class="checklist-quick-assign-dialog" data-quick-assign-dialog>
                        <form class="checklist-quick-assign-form" method="post"
                              action="${pageContext.request.contextPath}/checklist">
                            <input type="hidden" name="action" value="quickAssign">
                            <input type="hidden" name="activityId" value="${selectedActivityId}">
                            <div class="checklist-quick-assign-header">
                                <div>
                                    <p class="checklist-eyebrow">CHECKLIST</p>
                                    <h2>Phân công nhanh</h2>
                                    <p>Chọn công việc và thêm nhân sự. Phân công hiện tại sẽ được giữ nguyên.</p>
                                </div>
                                <button type="button" class="checklist-quick-assign-close"
                                        aria-label="Đóng" data-close-quick-assign>&times;</button>
                            </div>
                            <label class="checklist-quick-assign-field" for="quickAssignTask">
                                Công việc <span>*</span>
                                <select id="quickAssignTask" name="taskId" required>
                                    <option value="">-- Chọn công việc --</option>
                                    <c:forEach var="taskOption" items="${quickAssignTaskOptions}">
                                        <option value="${taskOption.id}">
                                            <c:out value="${taskOption.code}"/> - <c:out value="${taskOption.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </label>
                            <div class="checklist-quick-assign-pickers">
                                <section class="checklist-quick-assign-group">
                                    <div class="checklist-quick-assign-group-heading">
                                        <h3>Nhân viên nội bộ</h3>
                                        <label><input type="checkbox" data-select-all="staff"> Chọn tất cả</label>
                                    </div>
                                    <div class="checklist-quick-assign-options">
                                        <c:forEach var="staff" items="${quickAssignStaffOptions}">
                                            <label class="checklist-quick-assign-option">
                                                <input type="checkbox" name="staffIds" value="${staff.id}"
                                                       data-quick-assignee="staff">
                                                <span><strong><c:out value="${staff.name}"/></strong>
                                                    <small><c:out value="${staff.code}"/></small></span>
                                            </label>
                                        </c:forEach>
                                        <c:if test="${empty quickAssignStaffOptions}">
                                            <p class="checklist-quick-assign-empty">Không có nhân viên đang hoạt động.</p>
                                        </c:if>
                                    </div>
                                </section>
                                <section class="checklist-quick-assign-group">
                                    <div class="checklist-quick-assign-group-heading">
                                        <h3>Nhân viên đối tác</h3>
                                        <label><input type="checkbox" data-select-all="partner"> Chọn tất cả</label>
                                    </div>
                                    <div class="checklist-quick-assign-options">
                                        <c:forEach var="partner" items="${quickAssignPartnerOptions}">
                                            <label class="checklist-quick-assign-option">
                                                <input type="checkbox" name="partnerStaffIds" value="${partner.id}"
                                                       data-quick-assignee="partner">
                                                <span><strong><c:out value="${partner.name}"/></strong>
                                                    <small><c:out value="${partner.code}"/>
                                                        <c:if test="${not empty partner.companyName}">
                                                            · <c:out value="${partner.companyName}"/>
                                                        </c:if>
                                                    </small></span>
                                            </label>
                                        </c:forEach>
                                        <c:if test="${empty quickAssignPartnerOptions}">
                                            <p class="checklist-quick-assign-empty">Không có nhân viên đối tác đang hoạt động.</p>
                                        </c:if>
                                    </div>
                                </section>
                            </div>
                            <p class="checklist-quick-assign-hint">Tích vào ô cạnh tên để thêm người được phân công; có thể chọn nhiều người.</p>
                            <div class="checklist-quick-assign-actions">
                                <button type="button" class="checklist-cancel-button" data-close-quick-assign>Hủy</button>
                                <button type="submit" class="checklist-create-button">Lưu phân công</button>
                            </div>
                        </form>
                    </dialog>
                </c:if>

                <c:if test="${selectedActivityId > 0}">
                    <section class="checklist-completion-summary" aria-label="Tiến độ hoàn thành checklist">
                        <div class="checklist-completion-heading">
                            <strong>Tiến độ hoàn thành</strong>
                            <span>
                                <c:out value="${completedTaskCount}"/> / <c:out value="${totalTaskCount}"/> công việc
                                (<c:out value="${completionPercent}"/>%)
                            </span>
                        </div>
                        <progress max="100" value="${completionPercent}"
                                  aria-label="Đã hoàn thành ${completedTaskCount} trên ${totalTaskCount} công việc"></progress>
                    </section>
                </c:if>

                <c:choose>
                    <c:when test="${selectedActivityId == 0}">
                        <section class="checklist-empty">
                            <p>Chọn một hoạt động để xem checklist tương ứng.</p>
                        </section>
                    </c:when>
                    <c:when test="${empty checklistTasks}">
                        <section class="checklist-empty">
                            <p>Hoạt động này chưa có công việc checklist.</p>
                        </section>
                    </c:when>
                    <c:otherwise>
                        <section class="checklist-filter-panel" aria-label="Tìm kiếm và lọc checklist">
                            <div class="checklist-filter-heading">
                                <h2>Tìm kiếm và bộ lọc</h2>
                                <span data-filter-count><c:out value="${fn:length(checklistTasks)}"/> công việc</span>
                            </div>
                            <div class="checklist-filter-grid" data-checklist-filters>
                                <div class="checklist-filter-field checklist-filter-wide">
                                    <label for="taskSearch">Tìm mã, tên, nội dung hoặc người phụ trách</label>
                                    <input id="taskSearch" type="search" data-filter="search"
                                           placeholder="Nhập mã, tên, nội dung công việc...">
                                </div>
                                <div class="checklist-filter-field">
                                    <label for="staffFilter">Nhân viên phụ trách</label>
                                    <input id="staffFilter" type="search" data-filter="staff"
                                           placeholder="Tên nhân viên nội bộ">
                                </div>
                                <div class="checklist-filter-field">
                                    <label for="partnerFilter">Đối tác phụ trách</label>
                                    <input id="partnerFilter" type="search" data-filter="partner"
                                           placeholder="Tên nhân viên đối tác">
                                </div>
                                <div class="checklist-filter-field">
                                    <label for="phaseFilter">Giai đoạn</label>
                                    <select id="phaseFilter" data-filter="phase">
                                        <option value="">Tất cả giai đoạn</option>
                                        <option value="BEFORE_EVENT">Trước hoạt động</option>
                                        <option value="DURING_EVENT">Trong hoạt động</option>
                                        <option value="AFTER_EVENT">Sau hoạt động</option>
                                    </select>
                                </div>
                                <div class="checklist-filter-field">
                                    <label for="statusFilter">Trạng thái</label>
                                    <select id="statusFilter" data-filter="status">
                                        <option value="">Tất cả trạng thái</option>
                                        <option value="Chưa hoàn thành">Chưa hoàn thành</option>
                                        <option value="Đang hoàn thành">Đang hoàn thành</option>
                                        <option value="Đã hoàn thành">Đã hoàn thành</option>
                                    </select>
                                </div>
                                <div class="checklist-filter-field">
                                    <label for="deadlineFrom">Deadline từ ngày</label>
                                    <input id="deadlineFrom" type="date" data-filter="deadline-from">
                                </div>
                                <div class="checklist-filter-field">
                                    <label for="deadlineTo">Deadline đến ngày</label>
                                    <input id="deadlineTo" type="date" data-filter="deadline-to">
                                </div>
                                <div class="checklist-filter-actions">
                                    <button type="button" data-clear-filters>Xóa bộ lọc</button>
                                </div>
                            </div>
                        </section>
                        <section class="checklist-table-wrap" aria-label="Danh sách công việc">
                            <table class="checklist-table">
                                <thead>
                                <tr data-checklist-row
                                    data-code="<c:out value='${task.code}'/>"
                                    data-name="<c:out value='${task.name}'/>"
                                    data-description="<c:out value='${task.description}'/>"
                                    data-assignees="<c:out value='${task.assignees}'/>"
                                    data-staff="<c:out value='${task.staffAssignees}'/>"
                                    data-partner="<c:out value='${task.partnerStaffAssignees}'/>"
                                    data-phase="<c:out value='${task.phase}'/>"
                                    data-status="<c:out value='${task.statusValue}'/>"
                                    data-deadline="<c:out value='${task.deadlineFilterValue}'/>">
                                    <th scope="col">Mã công việc</th>
                                    <th scope="col">Tên công việc</th>
                                    <th scope="col">Người phụ trách</th>
                                    <th scope="col">Công việc tiên quyết</th>
                                    <th scope="col">Giai đoạn</th>
                                    <th scope="col">Ưu tiên</th>
                                    <th scope="col">Deadline</th>
                                    <th scope="col">Trạng thái</th>
                                    <th scope="col">Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="task" items="${checklistTasks}">
                                    <tr>
                                        <td class="checklist-table-code"><c:out value="${task.code}"/></td>
                                        <td class="checklist-table-name"><c:out value="${task.name}"/></td>
                                        <td class="checklist-table-description">
                                            <c:choose>
                                                <c:when test="${not empty task.assignees}">
                                                    <c:out value="${task.assignees}"/>
                                                </c:when>
                                                <c:otherwise><span class="checklist-muted">Chưa phân công</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="checklist-table-description">
                                            <c:choose>
                                                <c:when test="${not empty task.prerequisites}">
                                                    <c:out value="${task.prerequisites}"/>
                                                </c:when>
                                                <c:otherwise><span class="checklist-muted">Không có</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><c:out value="${task.phaseDisplay}"/></td>
                                        <td><c:out value="${task.priorityDisplay}"/></td>
                                        <td><c:out value="${task.deadlineDisplay}"/></td>
                                        <td>
                                            <form class="checklist-inline-status" method="post"
                                                  action="${pageContext.request.contextPath}/checklist">
                                                <input type="hidden" name="action" value="updateStatus">
                                                <input type="hidden" name="id" value="${task.id}">
                                                <input type="hidden" name="activityId" value="${selectedActivityId}">
                                                <select name="status" aria-label="Trạng thái công việc"
                                                        onchange="this.form.requestSubmit()">
                                                    <option value="Chưa hoàn thành" ${task.statusValue == 'Chưa hoàn thành' ? 'selected' : ''}>Chưa hoàn thành</option>
                                                    <option value="Đang hoàn thành" ${task.statusValue == 'Đang hoàn thành' ? 'selected' : ''}>Đang hoàn thành</option>
                                                    <option value="Đã hoàn thành" ${task.statusValue == 'Đã hoàn thành' ? 'selected' : ''}>Đã hoàn thành</option>
                                                </select>
                                            </form>
                                        </td>
                                        <td class="checklist-actions-cell">
                                            <div class="checklist-actions">
                                                <a class="checklist-detail-link"
                                                   href="${pageContext.request.contextPath}/checklist?action=detail&amp;id=${task.id}&amp;activityId=${selectedActivityId}">Chi tiết</a>
                                                <details class="action-menu">
                                                    <summary class="action-menu__summary">Thao tác</summary>
                                                    <div class="action-menu__items">
                                                        <a class="action-menu-item action-menu-item--edit"
                                                           href="${pageContext.request.contextPath}/checklist?action=edit&amp;id=${task.id}&amp;activityId=${selectedActivityId}">
                                                            Chỉnh sửa
                                                        </a>
                                                        <form method="post" action="${pageContext.request.contextPath}/checklist"
                                                              onsubmit="return confirm('Bạn có chắc muốn xóa công việc này không?');">
                                                            <input type="hidden" name="action" value="delete">
                                                            <input type="hidden" name="id" value="${task.id}">
                                                            <input type="hidden" name="activityId" value="${selectedActivityId}">
                                                            <button class="action-menu-item action-menu-item--delete" type="submit">Xóa</button>
                                                        </form>
                                                    </div>
                                                </details>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <tr data-filter-empty hidden>
                                    <td colspan="9" class="checklist-filter-empty">
                                        Không tìm thấy công việc phù hợp với điều kiện lọc.
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </section>
                    </c:otherwise>
                </c:choose>
            </main>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js" defer></script>
<script src="${pageContext.request.contextPath}/assets/js/checklist-list.js?v=20261105-1" defer></script>
<script src="${pageContext.request.contextPath}/assets/js/checklist-quick-assign.js?v=20261105-1" defer></script>
</body>
</html>
