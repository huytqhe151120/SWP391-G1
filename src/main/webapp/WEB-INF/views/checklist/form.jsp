<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<% request.setAttribute("activeNav", "checklist"); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${isEdit ? 'Chỉnh sửa' : 'Tạo'}"/> công việc checklist | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/checklist.css?v=20261029-10">
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
                        <p class="subtitle">Nhập thông tin và chọn nhân sự được phân công.</p>
                    </div>
                    <a class="checklist-back-link" href="${pageContext.request.contextPath}/checklist">Quay lại checklist</a>
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
                                <label for="code">Mã công việc <span>*</span></label>
                                <input id="code" name="code" type="text" maxlength="50" required
                                       value="<c:out value='${formCode}'/>" placeholder="Ví dụ: TASK-001">
                            </div>
                            <div class="checklist-field">
                                <label for="name">Tên công việc <span>*</span></label>
                                <input id="name" name="name" type="text" maxlength="255" required
                                       value="<c:out value='${formName}'/>" placeholder="Nhập tên công việc">
                            </div>
                            <div class="checklist-field checklist-field-wide">
                                <label for="description">Công việc chuẩn bị</label>
                                <textarea id="description" name="description" rows="4"
                                          placeholder="Nhập công việc cần chuẩn bị"><c:out value="${formDescription}"/></textarea>
                            </div>
                            <div class="checklist-field">
                                <label for="deadline">Hạn hoàn thành <span>*</span></label>
                                <input id="deadline" name="deadline" type="datetime-local" required
                                       value="<c:out value='${formDeadline}'/>">
                            </div>
                            <div class="checklist-field">
                                <label for="status">Trạng thái <span>*</span></label>
                                <select id="status" name="status" required>
                                    <option value="Chưa hoàn thành" ${formStatus == 'Chưa hoàn thành' ? 'selected' : ''}>Chưa hoàn thành</option>
                                    <option value="Đang hoàn thành" ${formStatus == 'Đang hoàn thành' ? 'selected' : ''}>Đang hoàn thành</option>
                                    <option value="Đã hoàn thành" ${formStatus == 'Đã hoàn thành' ? 'selected' : ''}>Đã hoàn thành</option>
                                </select>
                            </div>
                        </div>
                    </section>

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
                                        <div class="checklist-assignee-list" data-assignee-list="staff">
                                            <c:forEach var="staff" items="${staffOptions}">
                                                <label class="checklist-assignee-option" data-assignee-option>
                                                    <input type="checkbox" name="staffIds" value="${staff.id}"
                                                           <c:if test="${selectedStaffIds.contains(staff.id)}">checked</c:if>>
                                                    <span><strong><c:out value="${staff.name}"/></strong>
                                                        <small><c:out value="${staff.code}"/></small></span>
                                                </label>
                                            </c:forEach>
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
                                        <div class="checklist-assignee-list" data-assignee-list="partner">
                                            <c:forEach var="partnerStaff" items="${partnerStaffOptions}">
                                                <label class="checklist-assignee-option" data-assignee-option>
                                                    <input type="checkbox" name="partnerStaffIds" value="${partnerStaff.id}"
                                                           <c:if test="${selectedPartnerStaffIds.contains(partnerStaff.id)}">checked</c:if>>
                                                    <span><strong><c:out value="${partnerStaff.name}"/></strong>
                                                        <small><c:out value="${partnerStaff.code}"/>
                                                            <c:if test="${not empty partnerStaff.companyName}">
                                                                · <c:out value="${partnerStaff.companyName}"/>
                                                            </c:if>
                                                        </small></span>
                                                </label>
                                            </c:forEach>
                                        </div>
                                        <p class="checklist-no-search-results" data-no-assignee-results="partner" hidden>
                                            Không tìm thấy nhân viên đối tác phù hợp.
                                        </p>
                                    </c:otherwise>
                                </c:choose>
                            </fieldset>
                        </div>
                    </section>

                    <div class="checklist-form-actions">
                        <a class="checklist-cancel-button" href="${pageContext.request.contextPath}/checklist">Hủy</a>
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
<script src="${pageContext.request.contextPath}/assets/js/checklist-form.js" defer></script>
</body>
</html>
