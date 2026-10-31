<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activity-series" scope="request"/>
<c:set var="isEdit" value="false" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tạo chuỗi hoạt động | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261022-4">
<link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <main class="activity-app-main page activity-list-page activity-form-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ CHUỖI HOẠT ĐỘNG</span>
                    <h1>Tạo chuỗi hoạt động mới</h1>
                    <p>Điền thông tin để tạo chuỗi hoạt động lặp lại theo tháng hoặc năm.</p>
                </div>
            </header>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error activity-alert"><c:out value="${errorMessage}"/></div>
    </c:if>

    <section class="card activity-form-card">
        <div class="activity-table-heading">
            <div>
                <h2>Thông tin chuỗi hoạt động</h2>
                <p>Các trường có dấu <span class="required-mark">*</span> là bắt buộc</p>
            </div>
        </div>
        <form id="seriesForm" novalidate method="post"
              action="${pageContext.request.contextPath}/activity-series">
            <input type="hidden" name="action" value="create"/>

            <div class="activity-form-content">
            <div class="activity-form-section form-section">
                <h3>Thông tin cơ bản</h3>
                <p>Thông tin nhận diện và phân loại chuỗi hoạt động.</p>

                <div class="form-grid activity-form-grid">
                    <div class="field form-group-custom">
                        <label for="seriesCode">Mã chuỗi <span class="required-mark">*</span></label>
                        <input id="seriesCode" name="seriesCode" type="text" required
                               placeholder="Ví dụ: ACT-MONTHLY-001"
                               value="${param.seriesCode}"/>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="name">Tên chuỗi <span class="required-mark">*</span></label>
                        <input id="name" name="name" type="text" required
                               placeholder="Ví dụ: Hội thảo tháng"
                               value="${param.name}"/>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="activityTypeId">Mã loại hoạt động <span class="required-mark">*</span></label>
                        <select id="activityTypeId" name="activityTypeId" data-required>
                            <option value="">-- Chọn loại hoạt động --</option>
                            <c:forEach var="type" items="${activityTypes}">
                                <option value="${type.id}" ${param.activityTypeId == type.id ? 'selected' : ''}>
                                    <c:out value="${type.code}"/> - <c:out value="${type.name}"/></option>
                            </c:forEach>
                        </select>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="responsibleDepartmentId">Mã phòng ban phụ trách <span class="required-mark">*</span></label>
                        <select id="responsibleDepartmentId" name="responsibleDepartmentId" data-required>
                            <option value="">-- Chọn phòng ban --</option>
                            <c:forEach var="dept" items="${departments}">
                                <option value="${dept.id}" ${param.responsibleDepartmentId == dept.id ? 'selected' : ''}>
                                    <c:out value="${dept.code}"/> - <c:out value="${dept.name}"/></option>
                            </c:forEach>
                        </select>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="responsibleStaffId">Mã nhân viên phụ trách <span class="required-mark">*</span></label>
                        <select id="responsibleStaffId" name="responsibleStaffId" data-required>
                            <option value="">-- Chọn nhân viên --</option>
                            <c:forEach var="staff" items="${staffs}">
                                <option value="${staff.id}" ${param.responsibleStaffId == staff.id ? 'selected' : ''}>
                                    <c:out value="${staff.code}"/> - <c:out value="${staff.name}"/></option>
                            </c:forEach>
                        </select>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field field-wide">
                        <label for="description">Mô tả</label>
                        <textarea id="description" name="description" rows="3"
                                  placeholder="Mô tả chi tiết về chuỗi hoạt động...">${param.description}</textarea>
                    </div>
                    <div class="checkbox-inline">
                        <input id="registrationFormRequired" name="registrationFormRequired" type="checkbox" value="true" <c:if test="${param.registrationFormRequired == 'true'}">checked</c:if>/>
                        <label for="registrationFormRequired">Bắt buộc điền form đăng ký</label>
                    </div>
                </div>
            </div>

            <div class="activity-form-section form-section">
                <h3>Cấu hình lặp lại</h3>
                <p>Cấu hình chế độ và tần suất lặp lại của chuỗi hoạt động.</p>

                <div class="form-grid activity-form-grid">
                    <div class="field form-group-custom">
                        <label for="recurrenceMode">Chế độ lặp <span class="required-mark">*</span></label>
                        <select id="recurrenceMode" name="recurrenceMode" data-required required>
                            <option value="">-- Chọn chế độ --</option>
                            <option value="MONTHLY" ${param.recurrenceMode == 'MONTHLY' ? 'selected' : ''}>Theo tháng (MONTHLY)</option>
                            <option value="YEARLY" ${param.recurrenceMode == 'YEARLY' ? 'selected' : ''}>Theo năm (YEARLY)</option>
                        </select>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="recurrenceInterval">Khoảng cách lặp <span class="required-mark">*</span></label>
                        <input id="recurrenceInterval" name="recurrenceInterval" type="number" min="1" required
                               placeholder="Ví dụ: 1 (lặp mỗi 1 tháng/năm)"
                               value="${param.recurrenceInterval}"/>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="seriesStartDate">Ngày bắt đầu chuỗi <span class="required-mark">*</span></label>
                        <input id="seriesStartDate" name="seriesStartDate" type="date" required
                               value="${param.seriesStartDate}"/>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="seriesEndDate">Ngày kết thúc chuỗi</label>
                        <input id="seriesEndDate" name="seriesEndDate" type="date"
                               value="${param.seriesEndDate}"/>
                        <small class="error-msg">Ngày kết thúc không được trước ngày bắt đầu.</small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="maxOccurrences">Số sự kiện tối đa</label>
                        <input id="maxOccurrences" name="maxOccurrences" type="number" min="1"
                               placeholder="Không giới hạn"
                               value="${param.maxOccurrences}"/>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="occurrenceDurationMinutes">Thời lượng sự kiện (phút) <span class="required-mark">*</span></label>
                        <input id="occurrenceDurationMinutes" name="occurrenceDurationMinutes" type="number" min="1" required
                               placeholder="Ví dụ: 90"
                               value="${param.occurrenceDurationMinutes}"/>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="timeOfDay">Giờ trong ngày <span class="required-mark">*</span></label>
                        <input id="timeOfDay" name="timeOfDay" type="time" required
                               value="${param.timeOfDay}"/>
                        <small class="error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="semesterId">Mã học kỳ</label>
                        <select id="semesterId" name="semesterId">
                            <option value="">-- Chọn học kỳ (tùy chọn) --</option>
                            <c:forEach var="sem" items="${semesters}">
                                <option value="${sem.id}" ${param.semesterId == sem.id ? 'selected' : ''}>
                                    <c:out value="${sem.code}"/> - <c:out value="${sem.name}"/></option>
                            </c:forEach>
                        </select>
                        <small class="error-msg"></small>
                    </div>
                </div>
            </div>
            </div>

            <div class="form-actions-footer activity-form-actions">
                <a class="button activity-cancel-button"
                   href="${pageContext.request.contextPath}/activity-series">Hủy</a>
                <button class="button button-primary" type="submit">Tạo chuỗi hoạt động</button>
            </div>
        </form>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261016-1"></script>
</body>
</html>
