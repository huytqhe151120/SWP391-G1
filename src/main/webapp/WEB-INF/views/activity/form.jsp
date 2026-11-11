<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<%--@elvariable id="formData" type="com.swp391.g1.dto.request.ExtracurricularActivityRequestDTO"--%>
<%--@elvariable id="activity" type="com.swp391.g1.model.ExtracurricularActivity"--%>
<c:set var="activeNav" value="activities" scope="request"/>
<c:if test="${empty organizationLevels}">
    <jsp:useBean id="organizationLevelFallbackService" class="com.swp391.g1.service.impl.ActivityOrganizationLevelServiceImpl"/>
    <c:set var="organizationLevels" value="${organizationLevelFallbackService.allOrganizationLevels}"/>
</c:if>
<c:if test="${empty targetDepartments}">
    <jsp:useBean id="targetFallbackService" class="com.swp391.g1.service.impl.ActivityTargetParticipantServiceImpl"/>
    <c:set var="targetDepartments" value="${targetFallbackService.activeAcademicDepartments}"/>
    <c:set var="targetCohorts" value="${targetFallbackService.activeCohorts}"/>
    <c:set var="targetMajors" value="${targetFallbackService.activeMajors}"/>
    <c:set var="targetPrograms" value="${targetFallbackService.activeAcademicPrograms}"/>
    <c:set var="targetClasses" value="${targetFallbackService.activeMainClasses}"/>
</c:if>
<c:set var="semesterValue" value="${not empty formData ? formData.semesterId : activity.semesterId}"/>
<c:set var="typeValue" value="${not empty formData ? formData.activityTypeId : activity.activityTypeId}"/>
<c:set var="organizationLevelValue" value="${not empty formData ? formData.organizationLevelId : activity.organizationLevelId}"/>
<c:set var="departmentValue" value="${not empty formData ? formData.responsibleDepartmentId : activity.responsibleDepartmentId}"/>
<c:set var="staffValue" value="${not empty formData ? formData.responsibleStaffId : activity.responsibleStaffId}"/>
<c:set var="registrationValue" value="${not empty formData ? formData.registrationFormRequired == 'true' : activity.registrationFormRequired}"/>
<c:set var="summaryValue" value="${not empty formData ? formData.summaryRequired == 'true' : activity.summaryRequired}"/>
<c:set var="attendanceSessionCountValue" value="${not empty formData and not empty formData.attendanceSessionCount ? formData.attendanceSessionCount : (not empty activity and activity.attendanceSessionCount > 0 ? activity.attendanceSessionCount : 1)}"/>
<c:set var="targetScopeValue" value="${not empty formData ? formData.targetScope : (not empty activity ? activity.targetScope : 'ALL')}"/>
<c:set var="selectedTargetDepts" value="${not empty formData ? formData.targetDepartmentIds : (not empty activity ? activity.targetDepartmentIdStrings : null)}"/>
<c:set var="selectedTargetCohorts" value="${not empty formData ? formData.targetCohortIds : (not empty activity ? activity.targetCohortIdStrings : null)}"/>
<c:set var="selectedTargetMajors" value="${not empty formData ? formData.targetMajorIds : (not empty activity ? activity.targetMajorIdStrings : null)}"/>
<c:set var="selectedTargetPrograms" value="${not empty formData ? formData.targetProgramIds : (not empty activity ? activity.targetProgramIdStrings : null)}"/>
<c:set var="selectedTargetClasses" value="${not empty formData ? formData.targetClassIds : (not empty activity ? activity.targetClassIdStrings : null)}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:choose><c:when test="${isEdit}">Cập nhật hoạt động</c:when><c:otherwise>Tạo hoạt động</c:otherwise></c:choose> | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261107-4">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr@4.6.13/dist/flatpickr.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page activity-form-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ HOẠT ĐỘNG</span>
                    <h1><c:choose><c:when test="${isEdit}">Cập nhật hoạt động</c:when><c:otherwise>Tạo hoạt động mới</c:otherwise></c:choose></h1>
                    <p>Điền thông tin để quản lý và theo dõi hoạt động ngoại khóa.</p>
                </div>
            </header>

            <c:if test="${not empty errorMessage}">
                <div class="alert alert-error activity-alert"><c:out value="${errorMessage}"/></div>
            </c:if>

            <section class="card activity-form-card">
                <div class="activity-table-heading">
                    <div>
                        <h2>Thông tin hoạt động</h2>
                        <p>Các trường có dấu <span class="required-mark">*</span> là bắt buộc</p>
                    </div>
                </div>
                <form id="activityForm" novalidate method="post" action="${pageContext.request.contextPath}/activities"
                      <c:if test="${isEdit and activity.approvalStatus == 'APPROVED'}">data-confirm="Hoạt động đã duyệt mà có thay đổi nội dung sẽ chuyển về Bản nháp và cần gửi duyệt lại. Bạn có chắc chắn muốn cập nhật thông tin hoạt động?"</c:if>>
                    <input type="hidden" name="action" value="${isEdit ? 'update' : 'create'}">
                    <c:if test="${isEdit}">
                        <input type="hidden" name="id" value="${activity.id}">
                    </c:if>

                    <div class="activity-form-content">
                        <div class="activity-form-section form-section">
                            <h3>Thông tin cơ bản</h3>
                            <p>Thông tin nhận diện và phân loại hoạt động.</p>
                        </div>
                        <div class="form-grid activity-form-grid">
                            <div class="field form-group-custom">
                                <label for="code">Mã hoạt động <span class="required-mark">*</span></label>
                                <input id="code" name="code" type="text" minlength="3" maxlength="15" data-required
                                       value="<c:out value='${not empty formData ? formData.code : activity.code}'/>">
                                <small class="text-danger error-msg"></small>
                            </div>
                            <div class="field form-group-custom">
                                <label for="name">Tên hoạt động <span class="required-mark">*</span></label>
                                <input id="name" name="name" type="text" minlength="5" maxlength="150" data-required
                                       value="<c:out value='${not empty formData ? formData.name : activity.name}'/>">
                                <small class="text-danger error-msg"></small>
                            </div>
                            <div class="field form-group-custom">
                                <label for="semesterId">Học kỳ <span class="required-mark">*</span></label>
                                <select id="semesterId" name="semesterId" data-required>
                                    <option value="">Chọn học kỳ</option>
                                    <c:forEach var="semester" items="${semesters}">
                                        <option value="${semester.id}" <c:if test="${semesterValue == semester.id}">selected</c:if>>
                                            <c:out value="${semester.code}"/> - <c:out value="${semester.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                <small class="text-danger error-msg"></small>
                            </div>
                            <div class="field form-group-custom">
                                <label for="activityTypeId">Loại hoạt động <span class="required-mark">*</span></label>
                                <select id="activityTypeId" name="activityTypeId" data-required>
                                    <option value="">Chọn loại hoạt động</option>
                                    <c:forEach var="type" items="${activityTypes}">
                                        <option value="${type.id}" <c:if test="${typeValue == type.id}">selected</c:if>>
                                            <c:out value="${type.code}"/> - <c:out value="${type.name}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                                <small class="text-danger error-msg"></small>
                            </div>
                            <div class="field form-group-custom">
                                <label for="organizationLevelId">Cấp tổ chức <span class="required-mark">*</span></label>
                                <select id="organizationLevelId" name="organizationLevelId" data-required>
                                    <option value="">Chọn cấp tổ chức</option>
                                    <c:forEach var="orgLevel" items="${organizationLevels}">
                                        <option value="${orgLevel.id}" <c:if test="${organizationLevelValue == orgLevel.id}">selected</c:if>>
                                            <c:out value="${orgLevel.name}"/> (x<c:out value="${orgLevel.formattedCoefficient}"/>)
                                        </option>
                                    </c:forEach>
                                </select>
                                <small class="text-danger error-msg"></small>
                            </div>
                        </div>

                        <div class="activity-form-section form-section">
                            <h3>Phụ trách chính</h3>
                            <p>Chọn đơn vị phụ trách chính và nhân viên tổ chức. Đối tác được khai báo riêng trong tab "Đối tác hoạt động" ở trang chi tiết.</p>
                        </div>
                        <div class="form-grid activity-form-grid">
                            <div class="field form-group-custom">
                                <span class="field-label" id="responsibleDepartmentLabel">Đơn vị phụ trách chính <span class="required-mark">*</span></span>
                                <div class="multi-select-combobox" id="departmentCombobox" data-multi-select data-single data-required
                                     data-item-label="đơn vị" data-input-name="responsibleDepartmentId"
                                     data-staff-endpoint="${pageContext.request.contextPath}/activities/staffs-by-department">
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="department-options" aria-labelledby="responsibleDepartmentLabel">
                                        <span data-selection-label>Chọn đơn vị</span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="department-options" hidden>
                                        <input class="multi-select-search" type="search" placeholder="Tìm đơn vị..." aria-label="Tìm đơn vị">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn đơn vị</legend>
                                            <c:forEach var="department" items="${departments}">
                                                <label class="multi-select-option">
                                                    <input type="radio" name="responsibleDepartmentId" value="${department.id}" <c:if test="${departmentValue == department.id}">checked</c:if>>
                                                    <span><c:out value="${department.code}"/> - <c:out value="${department.name}"/></span>
                                                </label>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy đơn vị phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Chưa chọn đơn vị</span>
                                </div>
                            </div>
                            <c:set var="staffLocked" value="${empty departmentValue or departmentValue == 0}"/>
                            <c:set var="departmentLabel" value=""/>
                            <c:forEach var="department" items="${departments}">
                                <c:if test="${departmentValue == department.id}"><c:set var="departmentLabel" value="${department.code} - ${department.name}"/></c:if>
                            </c:forEach>
                            <div class="field form-group-custom">
                                <span class="field-label" id="responsibleStaffLabel">Nhân viên phụ trách chính <span class="required-mark">*</span></span>
                                <div class="multi-select-combobox" id="staffCombobox" data-multi-select data-single data-required
                                     data-item-label="nhân viên" data-input-name="responsibleStaffId"
                                     <c:if test="${staffLocked}">data-disabled</c:if>>
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="staff-options" aria-labelledby="responsibleStaffLabel"
                                            <c:if test="${staffLocked}">disabled</c:if>>
                                        <span data-selection-label><c:choose><c:when test="${staffLocked}">Chọn đơn vị phụ trách trước</c:when><c:otherwise>Chọn nhân viên</c:otherwise></c:choose></span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="staff-options" hidden>
                                        <input class="multi-select-search" type="search" placeholder="Tìm nhân viên..." aria-label="Tìm nhân viên">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn nhân viên</legend>
                                            <c:forEach var="staff" items="${staffs}">
                                                <label class="multi-select-option">
                                                    <input type="radio" name="responsibleStaffId" value="${staff.id}" <c:if test="${staffValue == staff.id}">checked</c:if> <c:if test="${staffLocked}">disabled</c:if>>
                                                    <span><c:out value="${staff.code}"/> - <c:out value="${staff.name}"/></span>
                                                </label>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy nhân viên phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Chưa chọn nhân viên</span>
                                </div>
                                <span id="responsibleStaffMessage" class="field-hint helper-text" aria-live="polite"></span>
                            </div>
                        </div>

                        <div class="activity-form-section form-section">
                            <h3>Thời gian và địa điểm</h3>
                            <p>Sắp xếp thời gian diễn ra và nơi tổ chức hoạt động.</p>
                        </div>
                        <div class="form-grid activity-form-grid">
                            <div class="field form-group-custom">
                                <label for="startTime">Thời gian bắt đầu <span class="required-mark">*</span></label>
                                <div class="date-input-group">
                                    <input id="startTime" name="startTime" type="text" autocomplete="off" placeholder="dd/mm/yyyy HH:mm" data-required
                                           value="<c:out value='${not empty formData ? formData.startTime : dt:formatInput(activity.startTime)}'/>">
                                    <span class="date-input-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg></span>
                                </div>
                                <small class="text-danger error-msg"></small>
                            </div>
                            <div class="field form-group-custom">
                                <label for="endTime">Thời gian kết thúc <span class="required-mark">*</span></label>
                                <div class="date-input-group">
                                    <input id="endTime" name="endTime" type="text" autocomplete="off" placeholder="dd/mm/yyyy HH:mm" data-required
                                           value="<c:out value='${not empty formData ? formData.endTime : dt:formatInput(activity.endTime)}'/>">
                                    <span class="date-input-icon" aria-hidden="true"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg></span>
                                </div>
                                <small class="text-danger error-msg"></small>
                            </div>
                            <div class="field field-wide">
                                <label for="address">Địa điểm</label>
                                <input id="address" name="address" type="text" maxlength="500"
                                       value="<c:out value='${not empty formData ? formData.address : activity.address}'/>">
                            </div>
                        </div>

                        <div class="activity-form-section form-section">
                            <h3>Đối tượng tham gia</h3>
                            <p>Thiết lập phạm vi sinh viên được phép tham gia (Toàn trường hoặc theo nhóm Khoa, Niên khóa, Ngành, Chương trình đào tạo, Lớp. Trường không chọn mặc định là Tất cả).</p>
                        </div>
                        <div class="form-grid activity-form-grid">
                            <div class="field field-wide form-group-custom" role="radiogroup" aria-labelledby="targetScopeLabel">
                                <span class="field-label" id="targetScopeLabel">Phạm vi đối tượng <span class="required-mark">*</span></span>
                                <div class="target-scope-options">
                                    <div class="checkbox-inline">
                                        <input id="targetScopeAll" type="radio" name="targetScope" value="ALL"
                                               <c:if test="${empty targetScopeValue or targetScopeValue == 'ALL'}">checked</c:if>>
                                        <label for="targetScopeAll">Toàn trường</label>
                                    </div>
                                    <div class="checkbox-inline">
                                        <input id="targetScopeGroup" type="radio" name="targetScope" value="GROUP"
                                               <c:if test="${targetScopeValue == 'GROUP'}">checked</c:if>>
                                        <label for="targetScopeGroup">Theo nhóm chỉ định</label>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <c:set var="targetLocked" value="${empty targetScopeValue or targetScopeValue != 'GROUP'}"/>
                        <div id="targetGroupFields" class="form-grid activity-form-grid">
                            <div class="field form-group-custom">
                                <span class="field-label" id="targetDeptLabel">Khoa</span>
                                <div class="multi-select-combobox" id="targetDepartmentCombobox" data-multi-select
                                     data-item-label="khoa" data-empty-count="Không chọn giá trị, mặc định: Tất cả khoa." data-input-name="targetDepartmentIds"
                                     <c:if test="${targetLocked}">data-disabled</c:if>>
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="target-dept-options" aria-labelledby="targetDeptLabel"
                                            <c:if test="${targetLocked}">disabled</c:if>>
                                        <span data-selection-label>Tất cả khoa</span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="target-dept-options" hidden>
                                        <input class="multi-select-search" type="search" placeholder="Tìm khoa..." aria-label="Tìm khoa">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn khoa</legend>
                                            <c:forEach var="dept" items="${targetDepartments}">
                                                <c:set var="deptIdStr" value="${dept.id.toString()}"/>
                                                <label class="multi-select-option">
                                                    <input type="checkbox" name="targetDepartmentIds" value="${dept.id}"
                                                           <c:if test="${not empty selectedTargetDepts and selectedTargetDepts.contains(deptIdStr)}">checked</c:if>
                                                           <c:if test="${targetLocked}">disabled</c:if>>
                                                    <span><c:out value="${dept.code}"/> - <c:out value="${dept.name}"/></span>
                                                </label>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy khoa phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Không chọn giá trị, mặc định: Tất cả khoa</span>
                                </div>
                            </div>

                            <div class="field form-group-custom">
                                <span class="field-label" id="targetCohortLabel">Niên khóa</span>
                                <div class="multi-select-combobox" id="targetCohortCombobox" data-multi-select
                                     data-item-label="niên khóa" data-empty-count="Không chọn giá trị, mặc định: Tất cả niên khóa" data-input-name="targetCohortIds"
                                     <c:if test="${targetLocked}">data-disabled</c:if>>
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="target-cohort-options" aria-labelledby="targetCohortLabel"
                                            <c:if test="${targetLocked}">disabled</c:if>>
                                        <span data-selection-label>Tất cả niên khóa</span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="target-cohort-options" hidden>
                                        <input class="multi-select-search" type="search" placeholder="Tìm niên khóa..." aria-label="Tìm niên khóa">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn niên khóa</legend>
                                            <c:forEach var="cohort" items="${targetCohorts}">
                                                <c:set var="cohortIdStr" value="${cohort.id.toString()}"/>
                                                <label class="multi-select-option">
                                                    <input type="checkbox" name="targetCohortIds" value="${cohort.id}"
                                                           <c:if test="${not empty selectedTargetCohorts and selectedTargetCohorts.contains(cohortIdStr)}">checked</c:if>
                                                           <c:if test="${targetLocked}">disabled</c:if>>
                                                    <span><c:out value="${cohort.code}"/> - <c:out value="${cohort.name}"/></span>
                                                </label>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy niên khóa phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Không chọn giá trị, mặc định: Tất cả niên khóa</span>
                                </div>
                            </div>

                            <div class="field form-group-custom">
                                <span class="field-label" id="targetMajorLabel">Ngành học</span>
                                <div class="multi-select-combobox" id="targetMajorCombobox" data-multi-select
                                     data-item-label="ngành học" data-empty-count="Không chọn giá trị, mặc định: Tất cả ngành học" data-input-name="targetMajorIds"
                                     <c:if test="${targetLocked}">data-disabled</c:if>>
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="target-major-options" aria-labelledby="targetMajorLabel"
                                            <c:if test="${targetLocked}">disabled</c:if>>
                                        <span data-selection-label>Tất cả ngành học</span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="target-major-options" hidden>
                                        <input class="multi-select-search" type="search" placeholder="Tìm ngành học..." aria-label="Tìm ngành học">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn ngành học</legend>
                                            <c:forEach var="major" items="${targetMajors}">
                                                <c:set var="majorIdStr" value="${major.id.toString()}"/>
                                                <label class="multi-select-option">
                                                    <input type="checkbox" name="targetMajorIds" value="${major.id}"
                                                           <c:if test="${not empty selectedTargetMajors and selectedTargetMajors.contains(majorIdStr)}">checked</c:if>
                                                           <c:if test="${targetLocked}">disabled</c:if>>
                                                    <span><c:out value="${major.code}"/> - <c:out value="${major.name}"/></span>
                                                </label>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy ngành học phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Không chọn giá trị, mặc định: Tất cả ngành học</span>
                                </div>
                            </div>

                            <div class="field form-group-custom">
                                <span class="field-label" id="targetProgramLabel">Chương trình đào tạo</span>
                                <div class="multi-select-combobox" id="targetProgramCombobox" data-multi-select
                                     data-item-label="chương trình đào tạo" data-empty-count="Không chọn giá trị, mặc định: Tất cả chương trình đào tạo" data-input-name="targetProgramIds"
                                     <c:if test="${targetLocked}">data-disabled</c:if>>
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="target-program-options" aria-labelledby="targetProgramLabel"
                                            <c:if test="${targetLocked}">disabled</c:if>>
                                        <span data-selection-label>Tất cả chương trình đào tạo</span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="target-program-options" hidden>
                                        <input class="multi-select-search" type="search" placeholder="Tìm chương trình đào tạo..." aria-label="Tìm chương trình đào tạo">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn chương trình đào tạo</legend>
                                            <c:forEach var="prog" items="${targetPrograms}">
                                                <c:set var="progIdStr" value="${prog.id.toString()}"/>
                                                <label class="multi-select-option">
                                                    <input type="checkbox" name="targetProgramIds" value="${prog.id}"
                                                           <c:if test="${not empty selectedTargetPrograms and selectedTargetPrograms.contains(progIdStr)}">checked</c:if>
                                                           <c:if test="${targetLocked}">disabled</c:if>>
                                                    <span><c:out value="${prog.code}"/> - <c:out value="${prog.name}"/></span>
                                                </label>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy chương trình đào tạo phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Không chọn giá trị, mặc định: Tất cả chương trình đào tạo</span>
                                </div>
                            </div>

                            <div class="field form-group-custom">
                                <span class="field-label" id="targetClassLabel">Lớp sinh hoạt</span>
                                <div class="multi-select-combobox" id="targetClassCombobox" data-multi-select
                                     data-item-label="lớp sinh hoạt" data-empty-count="Không chọn giá trị, mặc định: Tất cả lớp sinh hoạt" data-input-name="targetClassIds"
                                     <c:if test="${targetLocked}">data-disabled</c:if>>
                                    <button type="button" class="multi-select-trigger" aria-expanded="false"
                                            aria-controls="target-class-options" aria-labelledby="targetClassLabel"
                                            <c:if test="${targetLocked}">disabled</c:if>>
                                        <span data-selection-label>Tất cả lớp sinh hoạt</span>
                                        <span aria-hidden="true">▾</span>
                                    </button>
                                    <div class="multi-select-dropdown" id="target-class-options" hidden>
                                        <input class="multi-select-search" type="search" placeholder="Tìm lớp sinh hoạt..." aria-label="Tìm lớp sinh hoạt">
                                        <fieldset class="multi-select-options">
                                            <legend class="visually-hidden">Chọn lớp sinh hoạt</legend>
                                            <c:forEach var="mc" items="${targetClasses}">
                                                <c:set var="mcIdStr" value="${mc.id.toString()}"/>
                                                <label class="multi-select-option">
                                                    <input type="checkbox" name="targetClassIds" value="${mc.id}"
                                                           <c:if test="${not empty selectedTargetClasses and selectedTargetClasses.contains(mcIdStr)}">checked</c:if>
                                                           <c:if test="${targetLocked}">disabled</c:if>>
                                                    <span><c:out value="${mc.code}"/></span>
                                                </label>
                                            </c:forEach>
                                        </fieldset>
                                        <div class="multi-select-empty" hidden>Không tìm thấy lớp sinh hoạt phù hợp.</div>
                                    </div>
                                    <span class="multi-select-count" aria-live="polite">Không chọn giá trị, mặc định: Tất cả lớp sinh hoạt</span>
                                </div>
                            </div>
                        </div>

                        <div class="activity-form-section form-section">
                            <h3>Cấu hình và mô tả</h3>
                            <p>Cấu hình tính chất, số lượng tham gia và thông tin bổ sung.</p>
                        </div>
                        <div class="form-grid activity-form-grid">
                            <div class="field form-group-custom">
                                <label for="nature">Tính chất <span class="required-mark">*</span></label>
                                <c:set var="natureValue" value="${not empty formData ? formData.nature : activity.nature}"/>
                                <select id="nature" name="nature" data-required>
                                    <option value="">Chọn tính chất</option>
                                    <option value="REQUIRE_REGISTRATION" ${natureValue == 'REQUIRE_REGISTRATION' ? 'selected' : ''}>Cần đăng ký trước</option>
                                    <option value="VOLUNTARY" ${natureValue == 'VOLUNTARY' ? 'selected' : ''}>Tham gia tự do</option>
                                    <option value="MANDATORY" ${natureValue == 'MANDATORY' ? 'selected' : ''}>Chỉ định tham gia</option>
                                </select>
                                <small class="text-danger error-msg"></small>
                            </div>
                            <div class="field form-group-custom">
                                <label for="maxParticipants">Số lượng tham gia tối đa</label>
                                <input id="maxParticipants" name="maxParticipants" type="number" min="1" max="99999" step="1"
                                       placeholder="Không giới hạn"
                                       value="<c:out value='${not empty formData ? formData.maxParticipants : activity.maxParticipants}'/>">
                            </div>
                            <div class="field form-group-custom">
                                <label for="attendanceSessionCount">Số buổi điểm danh <span class="required-mark">*</span></label>
                                <input id="attendanceSessionCount" name="attendanceSessionCount" type="number" min="1" max="50" step="1" data-required
                                       placeholder="Nhập số buổi điểm danh (VD: 1, 2, 3...)"
                                       value="<c:out value='${attendanceSessionCountValue}'/>">
                                <small class="field-hint helper-text">Hệ thống sẽ tự động tạo tương ứng Buổi 1, Buổi 2,... theo số lượng đã nhập.</small>
                                <small class="text-danger error-msg"></small>
                            </div>
                            <div class="field field-wide form-group-custom">
                                <div class="activity-checkbox-group target-scope-options">
                                    <div class="checkbox-inline">
                                        <input id="summaryRequired" name="summaryRequired" type="checkbox" value="true" <c:if test="${summaryValue}">checked</c:if>>
                                        <label for="summaryRequired">Yêu cầu nộp bài tổng kết</label>
                                    </div>
                                    <div class="checkbox-inline">
                                        <input id="registrationFormRequired" name="registrationFormRequired" type="checkbox" value="true" <c:if test="${registrationValue}">checked</c:if>>
                                        <label for="registrationFormRequired">Yêu cầu sinh viên đăng ký bằng form</label>
                                    </div>
                                </div>
                            </div>
                            <div class="field field-wide">
                                <label for="description">Mô tả</label>
                                <textarea id="description" name="description"><c:out value="${not empty formData ? formData.description : activity.description}"/></textarea>
                            </div>
                        </div>
                    </div>

                    <div class="form-actions-footer activity-form-actions">
                        <a class="button activity-cancel-button" href="${pageContext.request.contextPath}/activities">Hủy</a>
                        <button class="button button-primary" type="submit">
                            <c:choose><c:when test="${isEdit}">Lưu thay đổi</c:when><c:otherwise>Tạo hoạt động</c:otherwise></c:choose>
                        </button>
                    </div>
                </form>
            </section>
        </main>
    </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/flatpickr@4.6.13/dist/flatpickr.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/flatpickr@4.6.13/dist/l10n/vn.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261107-4"></script>
<script src="${pageContext.request.contextPath}/assets/js/activity-form.js?v=20261107-5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
