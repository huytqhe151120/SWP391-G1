<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="activeNav" value="registration-forms" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:choose><c:when test="${form.id > 0}">Chỉnh sửa đợt đăng ký</c:when><c:otherwise>Tạo đợt đăng ký</c:otherwise></c:choose> | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261021-11">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261103-5">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <div class="activity-app-main">
            <main class="page activity-list-page activity-form-page">
                <header class="activity-hero">
                    <div class="activity-hero-copy">
                        <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ FORM ĐĂNG KÝ</span>
                        <h1><c:choose><c:when test="${form.id > 0}">Chỉnh sửa đợt đăng ký</c:when><c:otherwise>Tạo đợt đăng ký</c:otherwise></c:choose></h1>
                        <p>Tạo form cho sinh viên đăng ký tham gia hoạt động ngoại khóa.</p>
                    </div>
                    <a class="button activity-create-button" href="<c:out value='${returnTo}'/>">
                        <svg class="ui-icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/icons/ui-icons.svg#arrow-left"/></svg>
                        Quay lại
                    </a>
                </header>

                <c:if test="${not empty error}">
                    <div class="alert alert-error activity-alert"><c:out value="${error}"/></div>
                </c:if>

                <section class="card activity-form-card">
                    <div class="activity-table-heading">
                        <div>
                            <h2>Thông tin đợt đăng ký</h2>
                            <p>Form chỉ hiển thị sinh viên khi được mở, đúng thời hạn và hoạt động đã được duyệt.</p>
                        </div>
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/registration-forms">
                        <input type="hidden" name="action" value="save">
                        <input type="hidden" name="id" value="${form.id}">
                        <input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>">

                        <div class="activity-form-content">
                            <div class="activity-form-section">
                                <h3>Thông tin cơ bản</h3>
                                <p>Chọn hoạt động và cung cấp nội dung hướng dẫn đăng ký.</p>
                            </div>
                            <div class="form-grid activity-form-grid">
                                <c:if test="${form.quick}">
                                    <div class="field field-wide">
                                        <label>Hoạt động</label>
                                        <p><strong><c:out value="${form.activityName}"/></strong></p>
                                        <small class="field-hint">Đăng ký nhanh: sinh viên bấm Đăng ký và được duyệt tự động, không điền form. Bạn chỉ chỉnh mô tả và thời gian đăng ký.</small>
                                    </div>
                                </c:if>
                                <c:if test="${not form.quick}">
                                <div class="field field-wide">
                                    <label for="activityId">Hoạt động <span class="required-mark">*</span></label>
                                    <select id="activityId" name="activityId" required
                                            <c:if test="${form.registrationCount > 0}">disabled</c:if>>
                                        <option value="">-- Chọn hoạt động --</option>
                                        <c:forEach items="${activities}" var="a">
                                            <option value="${a.id}" <c:if test="${form.activityId == a.id}">selected</c:if>>
                                                <c:out value="${a.code}"/> - <c:out value="${a.name}"/>
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <c:if test="${empty activities}">
                                        <small class="field-hint">
                                            <c:choose>
                                                <c:when test="${hasEligibleApprovedActivities}">Tất cả hoạt động đã duyệt và chưa diễn ra đều đã có đợt đăng ký.</c:when>
                                                <c:otherwise>Chưa có hoạt động nào vừa được duyệt vừa ở trạng thái chưa diễn ra để tạo form.</c:otherwise>
                                            </c:choose>
                                        </small>
                                    </c:if>
                                </div>
                                <div class="field field-wide">
                                    <label for="title">Tiêu đề form <span class="required-mark">*</span></label>
                                    <input id="title" name="title" type="text" maxlength="200" required
                                           <c:if test="${form.registrationCount > 0}">disabled</c:if>
                                           value="<c:out value='${form.title}'/>"
                                           placeholder="Ví dụ: Đăng ký tham gia workshop">
                                </div>
                                </c:if>
                                <div class="field field-wide">
                                    <label for="description">Mô tả / hướng dẫn</label>
                                    <textarea id="description" name="description" rows="5" maxlength="4000"
                                              <c:if test="${form.registrationCount > 0 and not form.quick}">disabled</c:if>
                                              placeholder="Nhập thông tin sinh viên cần biết trước khi đăng ký..."><c:out value="${form.description}"/></textarea>
                                </div>
                            </div>

                            <c:if test="${not form.quick}">
                                <section class="registration-builder-section" id="question-builder">
                                    <div class="activity-form-section registration-builder-heading">
                                        <div>
                                            <h3>Câu hỏi bổ sung <span class="registration-question-count" data-question-count></span></h3>
                                            <p>Mỗi câu hỏi có thể là tự luận, chọn một hoặc chọn nhiều đáp án.</p>
                                        </div>
                                    </div>
                                    <div class="registration-question-list" data-question-list>
                                        <c:forEach items="${form.fields}" var="field" varStatus="question">
                                            <article class="registration-question-card" data-question-card>
                                                <input type="hidden" name="questionRequired_${question.index}"
                                                       value="${field.required ? 'true' : 'false'}" data-required-value>
                                                <input type="hidden" name="choiceCount_${question.index}"
                                                       value="${field.optionCount}" data-choice-count>
                                                <div class="registration-question-card-heading">
                                                    <span class="registration-question-number" data-question-number>Câu ${question.count}</span>
                                                    <select name="questionType_${question.index}" aria-label="Loại câu hỏi" data-question-type>
                                                        <option value="TEXTAREA" ${field.fieldType eq 'TEXTAREA' or field.fieldType eq 'TEXT' ? 'selected' : ''}>Tự luận</option>
                                                        <option value="SELECT" ${field.fieldType eq 'SELECT' ? 'selected' : ''}>Dropdown (chọn 1)</option>
                                                        <option value="MULTISELECT" ${field.fieldType eq 'MULTISELECT' or field.fieldType eq 'CHECKBOX' ? 'selected' : ''}>Nhiều lựa chọn</option>
                                                    </select>
                                                    <label class="registration-question-required">
                                                        <input type="checkbox" data-required-toggle
                                                               <c:if test="${field.required}">checked</c:if>>
                                                        Bắt buộc
                                                    </label>
                                                    <button class="registration-remove-question" type="button" data-remove-question
                                                            aria-label="Xóa câu hỏi">×</button>
                                                </div>
                                                <label class="registration-question-label">
                                                    Nội dung câu hỏi
                                                    <input name="questionLabel_${question.index}" type="text" maxlength="200"
                                                           value="<c:out value='${field.label}'/>" required>
                                                </label>
                                                <div class="registration-choice-editor" data-choice-editor>
                                                    <div class="registration-choice-list" data-choice-list>
                                                        <c:forEach items="${field.optionList}" var="option" varStatus="choice">
                                                            <label class="registration-choice-row">
                                                                <span class="registration-choice-marker" data-choice-marker>${choice.count}.</span>
                                                                <input name="choice_${question.index}_${choice.index}" type="text" maxlength="200"
                                                                       value="<c:out value='${option}'/>" required>
                                                                <button type="button" data-remove-choice aria-label="Xóa lựa chọn">×</button>
                                                            </label>
                                                        </c:forEach>
                                                    </div>
                                                    <button class="registration-add-choice" type="button" data-add-choice>+ Thêm lựa chọn</button>
                                                    <small data-choice-limit></small>
                                                </div>
                                            </article>
                                        </c:forEach>
                                    </div>
                                    <div class="registration-add-question-panel">
                                        <strong>Thêm câu hỏi</strong>
                                        <div>
                                            <button type="button" data-add-question="TEXTAREA">+ Tự luận</button>
                                            <button type="button" data-add-question="SELECT">+ Dropdown (chọn 1)</button>
                                            <button type="button" data-add-question="MULTISELECT">+ Nhiều lựa chọn</button>
                                        </div>
                                        <small>Tối đa 10 câu hỏi và 10 đáp án cho mỗi câu hỏi.</small>
                                    </div>
                                    <input type="hidden" name="questionCount" value="${fn:length(form.fields)}" data-question-total>
                                </section>
                            </c:if>

                            <div class="activity-form-section">
                                <h3>Thời gian đăng ký</h3>
                                <p>Thời gian mở và đóng là bắt buộc. Hạn đóng phải trước giờ bắt đầu hoạt động ít nhất 24 giờ.</p>
                            </div>
                            <div class="form-grid activity-form-grid">
                                <div class="field">
                                    <label for="openAt">Thời gian mở đăng ký <span class="required-mark">*</span></label>
                                    <input id="openAt" name="openAt" type="datetime-local" required
                                           <c:if test="${not form.quick and form.id == 0}">min="${minimumRegistrationTime}"</c:if>
                                           value="${form.openAt}" <c:if test="${form.registrationCount > 0 and not form.quick}">disabled</c:if>>
                                </div>
                                <div class="field">
                                    <label for="closeAt">Thời gian đóng đăng ký <span class="required-mark">*</span></label>
                                    <input id="closeAt" name="closeAt" type="datetime-local" required
                                           value="${form.closeAt}" <c:if test="${form.registrationCount > 0 and not form.quick}">disabled</c:if>>
                                </div>
                            </div>
                        </div>

                        <div class="form-actions activity-form-actions">
                            <a class="button activity-cancel-button" href="<c:out value='${returnTo}'/>">Hủy</a>
                            <c:choose>
                                <c:when test="${form.registrationCount > 0 and not form.quick}">
                                    <span>Không thể chỉnh sửa form đã có sinh viên đăng ký.</span>
                                </c:when>
                                <c:otherwise>
                                    <button class="button button-primary" type="submit">
                                        <c:choose><c:when test="${form.id > 0}">Lưu thay đổi</c:when><c:otherwise>Tạo form</c:otherwise></c:choose>
                                    </button>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </form>
                </section>
            </main>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/registration-form-builder.js?v=20261103-1"></script>
</body>
</html>