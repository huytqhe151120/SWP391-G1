<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="activity-series" scope="request"/>
<c:set var="isEdit" value="true" scope="request"/>
<c:set var="approved" value="${series.approvalStatus == 'APPROVED'}" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cập nhật chuỗi hoạt động | EAMS</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261020-11">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>

    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <main class="page activity-list-page activity-form-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ CHUỖI HOẠT ĐỘNG</span>
                    <h1>Cập nhật chuỗi hoạt động</h1>
                    <p>Chỉnh sửa thông tin chuỗi hoạt động lặp lại.</p>
                </div>
            </header>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-error activity-alert"><c:out value="${errorMessage}"/></div>
    </c:if>

    <c:if test="${approved}">
        <div class="alert alert-error activity-alert">
            Chuỗi hoạt động đã được duyệt (APPROVED). Chỉ có thể thay đổi mô tả (description).
            Các trường lặp lại sẽ bị khóa và không được thay đổi.
        </div>
    </c:if>

    <section class="card activity-form-card">
        <div class="activity-table-heading">
            <div>
                <h2>Cập nhật chuỗi hoạt động</h2>
                <p>Các trường có dấu <span class="required-mark">*</span> là bắt buộc</p>
            </div>
        </div>
        <form id="seriesForm" novalidate method="post"
              action="${pageContext.request.contextPath}/activity-series">
            <input type="hidden" name="action" value="update"/>
            <input type="hidden" name="id" value="${series.id}"/>

            <div class="activity-form-content">
            <div class="activity-form-section form-section">
                <h3>Thông tin cơ bản</h3>
                <p>Thông tin nhận diện và phân loại chuỗi hoạt động.</p>

                <div class="form-grid activity-form-grid">
                    <div class="field form-group-custom">
                        <label for="seriesCode">Mã chuỗi <span class="required-mark">*</span></label>
                        <input id="seriesCode" name="seriesCode" type="text" required
                               value="${series.seriesCode}" ${approved ? 'disabled' : ''}/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="name">Tên chuỗi <span class="required-mark">*</span></label>
                        <input id="name" name="name" type="text" required
                               value="${series.name}" ${approved ? 'disabled' : ''}/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="activityTypeId">Mã loại hoạt động <span class="required-mark">*</span></label>
                        <input id="activityTypeId" name="activityTypeId" type="number" min="1" required
                               value="${series.activityTypeId}" ${approved ? 'disabled' : ''}/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="responsibleDepartmentId">Mã phòng ban phụ trách <span class="required-mark">*</span></label>
                        <input id="responsibleDepartmentId" name="responsibleDepartmentId" type="number" min="1" required
                               value="${series.responsibleDepartmentId}" ${approved ? 'disabled' : ''}/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="responsibleStaffId">Mã nhân viên phụ trách <span class="required-mark">*</span></label>
                        <input id="responsibleStaffId" name="responsibleStaffId" type="number" min="1" required
                               value="${series.responsibleStaffId}" ${approved ? 'disabled' : ''}/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field field-wide">
                        <label for="description">Mô tả</label>
                        <textarea id="description" name="description" rows="3">${series.description}</textarea>
                    </div>
                    <div class="checkbox-inline">
                        <input id="registrationFormRequired" name="registrationFormRequired" type="checkbox" value="true"
                               ${series.registrationFormRequired ? 'checked' : ''} ${approved ? 'disabled' : ''}/>
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
                        <select id="recurrenceMode" name="recurrenceMode" data-required required ${approved ? 'disabled' : ''}>
                            <option value="">-- Chọn chế độ --</option>
                            <option value="MONTHLY" ${series.recurrenceMode == 'MONTHLY' ? 'selected' : ''}>Theo tháng (MONTHLY)</option>
                            <option value="YEARLY" ${series.recurrenceMode == 'YEARLY' ? 'selected' : ''}>Theo năm (YEARLY)</option>
                        </select>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="recurrenceInterval">Khoảng cách lặp <span class="required-mark">*</span></label>
                        <input id="recurrenceInterval" name="recurrenceInterval" type="number" min="1" value="1" required
                               value="${series.recurrenceInterval}" ${approved ? 'disabled' : ''}
                               placeholder="Ví dụ: 1 (lặp mỗi 1 tháng/năm)"/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="seriesStartDate">Ngày bắt đầu chuỗi <span class="required-mark">*</span></label>
                        <input id="seriesStartDate" name="seriesStartDate" type="date" required
                               value="${series.seriesStartDate}" ${approved ? 'disabled' : ''}/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="seriesEndDate">Ngày kết thúc chuỗi</label>
                        <input id="seriesEndDate" name="seriesEndDate" type="date"
                               value="${series.seriesEndDate}" ${approved ? 'disabled' : ''}/>
                        <small class="text-danger error-msg">Ngày kết thúc không được trước ngày bắt đầu.</small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="maxOccurrences">Số sự kiện tối đa</label>
                        <input id="maxOccurrences" name="maxOccurrences" type="number" min="1"
                               value="${series.maxOccurrences}" ${approved ? 'disabled' : ''}
                               placeholder="Không giới hạn"/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="occurrenceDurationMinutes">Thời lượng sự kiện (phút) <span class="required-mark">*</span></label>
                        <input id="occurrenceDurationMinutes" name="occurrenceDurationMinutes" type="number" min="1" required
                               value="${series.occurrenceDurationMinutes}" ${approved ? 'disabled' : ''}
                               placeholder="Ví dụ: 90"/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="timeOfDay">Giờ trong ngày <span class="required-mark">*</span></label>
                        <input id="timeOfDay" name="timeOfDay" type="time" required
                               value="${series.timeOfDay}" ${approved ? 'disabled' : ''}/>
                        <small class="text-danger error-msg"></small>
                    </div>
                    <div class="field form-group-custom">
                        <label for="semesterId">Mã học kỳ</label>
                        <input id="semesterId" name="semesterId" type="number" min="1"
                               value="${series.semesterId}" ${approved ? 'disabled' : ''}
                               placeholder="ID học kỳ (tùy chọn)"/>
                        <small class="text-danger error-msg"></small>
                    </div>
                </div>
            </div>
            </div>
            <!-- Hidden inputs for disabled fields (APPROVED series) -->
            <c:if test="${approved}">
                <input type="hidden" name="seriesCode" value="${series.seriesCode}"/>
                <input type="hidden" name="name" value="${series.name}"/>
                <input type="hidden" name="activityTypeId" value="${series.activityTypeId}"/>
                <input type="hidden" name="responsibleDepartmentId" value="${series.responsibleDepartmentId}"/>
                <input type="hidden" name="responsibleStaffId" value="${series.responsibleStaffId}"/>
                <input type="hidden" name="registrationFormRequired" value="${series.registrationFormRequired}"/>
                <input type="hidden" name="recurrenceMode" value="${series.recurrenceMode}"/>
                <input type="hidden" name="recurrenceInterval" value="${series.recurrenceInterval}"/>
                <input type="hidden" name="seriesStartDate" value="${series.seriesStartDate}"/>
                <input type="hidden" name="seriesEndDate" value="${series.seriesEndDate}"/>
                <input type="hidden" name="maxOccurrences" value="${series.maxOccurrences}"/>
                <input type="hidden" name="occurrenceDurationMinutes" value="${series.occurrenceDurationMinutes}"/>
                <input type="hidden" name="timeOfDay" value="${series.timeOfDay}"/>
                <input type="hidden" name="semesterId" value="${series.semesterId}"/>
            </c:if>

            <div class="form-actions-footer activity-form-actions">
                <a class="button activity-cancel-button"
                   href="${pageContext.request.contextPath}/activity-series">Hủy</a>
                <button class="button button-primary" type="submit">Lưu thay đổi</button>
            </div>
        </form>
    </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity-form.js?v=20261020-9"></script>
</body>
</html>