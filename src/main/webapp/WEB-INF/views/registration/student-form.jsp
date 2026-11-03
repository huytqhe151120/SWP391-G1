<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="activities" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Gửi đăng ký | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261020-11">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261103-4">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <div class="activity-app-main">
            <main class="page activity-list-page activity-form-page student-registration-form-page">
                <header class="activity-hero">
                    <div class="activity-hero-copy">
                        <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> ĐĂNG KÝ HOẠT ĐỘNG</span>
                        <h1>Đăng ký tham gia</h1>
                        <p>Kiểm tra thông tin hoạt động và gửi đăng ký của bạn.</p>
                    </div>
                    <a class="button activity-create-button" href="<c:out value='${returnTo}'/>">
                        <svg class="ui-icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/icons/ui-icons.svg#arrow-left"/></svg>
                        Danh sách hoạt động
                    </a>
                </header>

                <c:if test="${not empty error}">
                    <div class="alert alert-error activity-alert"><c:out value="${error}"/></div>
                </c:if>

                <section class="card activity-form-card student-registration-detail">
                    <div class="activity-table-heading">
                        <div>
                            <h2><c:out value="${form.title}"/></h2>
                            <p class="student-registration-form-description"><c:out value="${form.description}"/></p>
                        </div>
                        <span class="badge status-pill status-green">Đang mở đăng ký</span>
                    </div>
                    <div class="activity-form-content">
                        <section class="student-registration-summary" aria-label="Thông tin hoạt động">
                            <c:if test="${not empty activity.activityTypeCode or not empty activity.activityTypeName}">
                                <span class="student-registration-type">
                                    <c:out value="${activity.activityTypeCode}"/> - <c:out value="${activity.activityTypeName}"/>
                                </span>
                            </c:if>
                            <dl class="student-registration-summary-grid">
                                <div>
                                    <dt>Thời gian diễn ra</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${not empty activity.startTime}">
                                                <c:out value="${dt:format(activity.startTime)}"/>
                                            </c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                                <div>
                                    <dt>Địa điểm</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${not empty activity.address}"><c:out value="${activity.address}"/></c:when>
                                            <c:otherwise>—</c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                                <div>
                                    <dt>Đóng đăng ký</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${not empty form.closeAt}"><c:out value="${dt:format(form.closeAt)}"/></c:when>
                                            <c:otherwise>Không giới hạn</c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                                <div>
                                    <dt>Số chỗ</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${not empty form.maxParticipants}">
                                                Còn <c:out value="${form.maxParticipants - form.reservedSeatCount}"/> / <c:out value="${form.maxParticipants}"/>
                                            </c:when>
                                            <c:otherwise>Không giới hạn</c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                            </dl>
                            <p class="student-registration-cancellation-note">
                                Hạn hủy hoặc rút đơn: 24 giờ trước giờ bắt đầu. Đơn đã được duyệt, sau khi hủy sẽ không đăng ký lại được.
                            </p>
                        </section>
                        <c:if test="${not empty savedProfile}">
                            <p>Thông tin đã lưu trong hồ sơ được điền sẵn; bạn có thể cập nhật trước khi gửi.</p>
                        </c:if>
                        <form method="post" action="${pageContext.request.contextPath}/registration-forms">
                            <input type="hidden" name="action" value="register">
                            <input type="hidden" name="id" value="${form.id}">
                            <input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>">
                            <div class="form-grid activity-form-grid">
                                <div class="field field-wide">
                                    <h3>Thông tin sinh viên</h3>
                                </div>
                                <div class="field">
                                    <label for="studentCode">Mã sinh viên <span class="required-mark">*</span></label>
                                    <input id="studentCode" type="text" maxlength="50"
                                           value="<c:out value='${personalInfo.studentCode}'/>" readonly aria-readonly="true">
                                </div>
                                <div class="field">
                                    <label for="studentName">Họ và tên <span class="required-mark">*</span></label>
                                    <input id="studentName" type="text" maxlength="255"
                                           value="<c:out value='${personalInfo.studentName}'/>" readonly aria-readonly="true">
                                </div>
                                <div class="field">
                                    <label for="classCode">Lớp <span class="required-mark">*</span></label>
                                    <input id="classCode" type="text" maxlength="50"
                                           value="<c:out value='${personalInfo.classCode}'/>" readonly aria-readonly="true">
                                </div>
                                <div class="field">
                                    <label for="email">Email <span class="required-mark">*</span></label>
                                    <input id="email" type="email" maxlength="255"
                                           value="<c:out value='${personalInfo.email}'/>" readonly aria-readonly="true">
                                </div>
                                <div class="field">
                                    <label for="phoneNumber">Số điện thoại <span class="required-mark">*</span></label>
                                    <input id="phoneNumber" type="text" maxlength="30"
                                           value="<c:out value='${personalInfo.phoneNumber}'/>" readonly aria-readonly="true">
                                </div>
                                <div class="field">
                                    <label for="dateOfBirth">Ngày sinh <span class="required-mark">*</span></label>
                                    <input id="dateOfBirth" type="text"
                                           value="<c:out value='${personalInfo.dobDisplay}'/>" readonly aria-readonly="true">
                                </div>
                                <div class="field field-wide registration-student-additional-heading">
                                    <h3>Thông tin bổ sung</h3>
                                    <p>Ban tổ chức cần thêm một số thông tin cho hoạt động này.</p>
                                </div>
                                <c:forEach items="${form.fields}" var="field" varStatus="question">
                                    <div class="field field-wide">
                                        <label for="field_${field.fieldKey}">
                                            Câu <c:out value="${question.count}"/>:
                                            <c:out value="${field.label}"/>
                                            <c:if test="${field.required}"><span class="required-mark">*</span></c:if>
                                        </label>
                                        <c:choose>
                                            <c:when test="${field.fieldType eq 'TEXTAREA'}">
                                                <textarea id="field_${field.fieldKey}" name="field_${field.fieldKey}" rows="4"
                                                          <c:if test="${field.required}">required</c:if>><c:out value="${field.value}"/></textarea>
                                            </c:when>
                                            <c:when test="${field.fieldType eq 'SELECT'}">
                                                <select id="field_${field.fieldKey}" name="field_${field.fieldKey}"
                                                        <c:if test="${field.required}">required</c:if>>
                                                    <option value="">-- Chọn --</option>
                                                    <c:forEach items="${field.optionList}" var="option">
                                                        <option value="<c:out value='${option}'/>"
                                                                <c:if test="${field.value eq option}">selected</c:if>><c:out value="${option}"/></option>
                                                    </c:forEach>
                                                </select>
                                            </c:when>
                                            <c:when test="${field.fieldType eq 'MULTISELECT'}">
                                                <div class="registration-student-choice-list">
                                                    <c:forEach items="${field.optionList}" var="option">
                                                        <label class="registration-student-choice">
                                                            <input type="checkbox" name="field_${field.fieldKey}"
                                                                   value="<c:out value='${option}'/>"
                                                                   <c:if test="${field.selectedOptions.contains(option)}">checked</c:if>>
                                                            <span><c:out value="${option}"/></span>
                                                        </label>
                                                    </c:forEach>
                                                </div>
                                            </c:when>
                                            <c:when test="${field.fieldType eq 'CHECKBOX'}">
                                                <input id="field_${field.fieldKey}" name="field_${field.fieldKey}" type="checkbox"
                                                       value="true" <c:if test="${field.value eq 'true'}">checked</c:if>
                                                       <c:if test="${field.required}">required</c:if>>
                                            </c:when>
                                            <c:when test="${field.fieldType eq 'NUMBER'}">
                                                <input id="field_${field.fieldKey}" name="field_${field.fieldKey}" type="number"
                                                       value="<c:out value='${field.value}'/>"
                                                       <c:if test="${field.required}">required</c:if>>
                                            </c:when>
                                            <c:when test="${field.fieldType eq 'DATE'}">
                                                <input id="field_${field.fieldKey}" name="field_${field.fieldKey}" type="date"
                                                       value="<c:out value='${field.value}'/>"
                                                       <c:if test="${field.required}">required</c:if>>
                                            </c:when>
                                            <c:when test="${field.fieldType eq 'EMAIL'}">
                                                <input id="field_${field.fieldKey}" name="field_${field.fieldKey}" type="email"
                                                       value="<c:out value='${field.value}'/>"
                                                       <c:if test="${field.required}">required</c:if>>
                                            </c:when>
                                            <c:when test="${field.fieldType eq 'PHONE'}">
                                                <input id="field_${field.fieldKey}" name="field_${field.fieldKey}" type="tel"
                                                       value="<c:out value='${field.value}'/>"
                                                       <c:if test="${field.required}">required</c:if>>
                                            </c:when>
                                            <c:otherwise>
                                                <input id="field_${field.fieldKey}" name="field_${field.fieldKey}" type="text"
                                                       value="<c:out value='${field.value}'/>"
                                                       <c:if test="${field.required}">required</c:if>>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </c:forEach>
                                <div class="field field-wide">
                                    <label for="note">Ghi chú thêm <span class="registration-optional">(không bắt buộc)</span></label>
                                    <textarea id="note" name="note" rows="5" maxlength="2000"
                                              placeholder="Thông tin bạn muốn ban tổ chức biết..."></textarea>
                                    <small class="field-hint registration-note-hint">Chỉ ban tổ chức xem được nội dung này.</small>
                                </div>
                                <div class="field field-wide student-registration-commitment-field">
                                    <label class="student-registration-commitment">
                                        <input type="checkbox" name="attendanceCommitment" value="true"
                                               <c:if test="${attendanceCommitment}">checked</c:if> required>
                                        <span class="checkbox-label">Tôi cam kết tham dự đầy đủ và đúng giờ <span class="required-mark">*</span></span>
                                    </label>
                                </div>
                            </div>
                            <div class="form-actions activity-form-actions student-registration-actions">
                                <a class="button activity-cancel-button" href="<c:out value='${returnTo}'/>">Quay lại</a>
                                <button class="button button-primary" type="submit">Gửi đăng ký</button>
                            </div>
                        </form>
                    </div>
                </section>
            </main>
        </div>
    </div>
</div>
<script>
    document.querySelectorAll(".registration-student-choice-list").forEach(group => {
        const checkboxes = [...group.querySelectorAll('input[type="checkbox"]')];
        const required = group.closest(".field").querySelector(".required-mark") !== null;
        if (!required || checkboxes.length === 0) return;
        const validate = () => {
            const valid = checkboxes.some(checkbox => checkbox.checked);
            checkboxes[0].setCustomValidity(valid ? "" : "Chọn ít nhất một đáp án.");
        };
        checkboxes.forEach(checkbox => checkbox.addEventListener("change", validate));
        validate();
    });
</script>
</body>
</html>