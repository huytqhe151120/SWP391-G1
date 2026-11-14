<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="registration-forms" scope="request"/>
<c:set var="submittedCount" value="0"/>
<c:set var="approvedCount" value="0"/>
<c:set var="rejectedCount" value="0"/>
<c:set var="cancelledOrWithdrawnCount" value="0"/>
<c:forEach items="${applications}" var="appItem">
    <c:choose>
        <c:when test="${appItem.status eq 'SUBMITTED'}">
            <c:set var="submittedCount" value="${submittedCount + 1}"/>
        </c:when>
        <c:when test="${appItem.status eq 'APPROVED'}">
            <c:set var="approvedCount" value="${approvedCount + 1}"/>
        </c:when>
        <c:when test="${appItem.status eq 'REJECTED'}">
            <c:set var="rejectedCount" value="${rejectedCount + 1}"/>
        </c:when>
        <c:when test="${appItem.status eq 'CANCELLED' or appItem.status eq 'WITHDRAWN'}">
            <c:set var="cancelledOrWithdrawnCount" value="${cancelledOrWithdrawnCount + 1}"/>
        </c:when>
    </c:choose>
</c:forEach>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>EAMS | Chi tiết đợt đăng ký</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261110-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/action-menu.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/attendance.css?v=20261110-1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261110-1">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>

    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <main class="activity-app-main page activity-list-page activity-form-page activity-detail-page">
            <header class="activity-hero">
                <div class="activity-hero-copy">
                    <span class="eyebrow">EAMS <span aria-hidden="true">/</span> QUẢN LÝ ĐĂNG KÝ</span>
                    <h1>Chi tiết đợt đăng ký</h1>
                    <p>
                        <c:choose>
                            <c:when test="${not empty form}"><c:out value="${form.title}"/></c:when>
                            <c:when test="${not empty activity}"><c:out value="${activity.code}"/> – <c:out value="${activity.name}"/></c:when>
                            <c:otherwise>Chưa có thông tin đợt đăng ký</c:otherwise>
                        </c:choose>
                    </p>
                </div>
            </header>

            <c:if test="${not empty requestScope.errorMessage}">
                <div class="alert alert-error activity-alert"><c:out value="${requestScope.errorMessage}"/></div>
            </c:if>
            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.successMessage}">
                <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
                <c:remove var="successMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.warningMessage}">
                <div class="alert alert-error activity-alert"><c:out value="${sessionScope.warningMessage}"/></div>
                <c:remove var="warningMessage" scope="session"/>
            </c:if>

            <section class="card activity-table-card activity-form-card activity-detail-card registration-detail-card"
                     aria-labelledby="registration-detail-title">
                <div class="activity-table-heading">
                    <div>
                        <h2 id="registration-detail-title">Danh sách đăng ký &amp; Thông tin đợt</h2>
                        <p>
                            <c:choose>
                                <c:when test="${not empty activity}">
                                    <c:out value="${activity.code}"/> – <c:out value="${activity.name}"/>
                                </c:when>
                                <c:when test="${not empty form}">
                                    <c:out value="${form.activityName}"/>
                                </c:when>
                                <c:otherwise>Chọn hoạt động để xem thông tin đợt đăng ký và danh sách đơn</c:otherwise>
                            </c:choose>
                        </p>
                    </div>
                    <div class="activity-heading-actions registration-detail-actions">
                        <c:if test="${not empty form}">
                            <span class="activity-count" data-header-app-count>
                                <c:out value="${fn:length(applications)}"/> sinh viên đăng ký
                            </span>
                        </c:if>
                        <c:choose>
                            <c:when test="${not empty form and (form.quick or form.registrationCount == 0)}">
                                <a class="button button-sm button-primary"
                                   href="${pageContext.request.contextPath}/registration-forms?action=edit&amp;id=${form.id}&amp;from=detail">Chỉnh sửa</a>
                            </c:when>
                            <c:when test="${empty form and not empty activity}">
                                <a class="button button-sm button-primary"
                                   href="${pageContext.request.contextPath}/registration-forms?action=create&amp;activityId=${activity.id}&amp;from=detail">Tạo mới đợt đăng ký</a>
                            </c:when>
                        </c:choose>
                        <c:if test="${not empty form and ((not form.quick and form.registrationCount == 0) or extendableForm)}">
                            <details class="action-menu">
                                <summary aria-label="Thao tác khác"><span aria-hidden="true">⋯</span></summary>
                                <div class="action-menu__items">
                                    <c:if test="${extendableForm}">
                                        <form class="registration-extend-form" method="post"
                                              action="${pageContext.request.contextPath}/registration-forms">
                                            <input type="hidden" name="action" value="extendFormClose">
                                            <input type="hidden" name="formId" value="${form.id}">
                                            <label for="extend-close-${form.id}">Thời gian đóng mới</label>
                                            <input id="extend-close-${form.id}" type="datetime-local"
                                                   name="closeAt" min="${minimumExtensionTime}" required>
                                            <button class="action-menu-item action-menu-item--extend" type="submit">
                                                Gia hạn đăng ký
                                            </button>
                                        </form>
                                    </c:if>
                                    <c:if test="${not form.quick and form.registrationCount == 0}">
                                        <form method="post" action="${pageContext.request.contextPath}/registration-forms"
                                              onsubmit="return confirm('Bạn chắc chắn muốn xóa form này?')">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="id" value="${form.id}">
                                            <button class="action-menu-item action-menu-item--delete" type="submit">
                                                <span class="action-menu-item__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg></span>
                                                Xóa
                                            </button>
                                        </form>
                                    </c:if>
                                </div>
                            </details>
                        </c:if>
                    </div>
                </div>

                <details class="activity-filter-panel" open>
                    <summary class="activity-filter-toggle"><span>Chọn hoạt động</span></summary>
                    <form class="activity-filter attendance-filter attendance-filter--single" action="${pageContext.request.contextPath}/registration-forms" method="get" data-registration-activity-form>
                        <input type="hidden" name="action" value="view">
                        <div class="activity-filter-field attendance-filter-activity">
                            <label id="registration-activity-label">Hoạt động</label>
                            <div class="multi-select-combobox" data-multi-select data-single data-auto-submit
                                 data-item-label="hoạt động" data-input-name="activityId">
                                <button type="button" class="multi-select-trigger" aria-expanded="false"
                                        aria-controls="registration-activity-options" aria-labelledby="registration-activity-label">
                                    <span data-selection-label>-- Chọn hoạt động --</span>
                                    <span aria-hidden="true">▾</span>
                                </button>
                                <div class="multi-select-dropdown" id="registration-activity-options" hidden>
                                    <input class="multi-select-search" type="search"
                                           placeholder="Tìm mã hoặc tên hoạt động..." aria-label="Tìm hoạt động">
                                    <fieldset class="multi-select-options">
                                        <legend class="visually-hidden">Chọn hoạt động</legend>
                                        <c:forEach var="actOption" items="${activities}">
                                            <c:if test="${actOption.registrationFormRequired}">
                                                <label class="multi-select-option">
                                                    <input type="radio" name="activityId" value="${actOption.id}" ${(not empty activity and activity.id == actOption.id) or (not empty form and form.activityId == actOption.id) ? 'checked' : ''}>
                                                    <span><c:out value="${actOption.code}"/> – <c:out value="${actOption.name}"/><c:if test="${not empty actOption.semesterName}"> (<c:out value="${actOption.semesterName}"/>)</c:if><c:if test="${not empty actOption.organizationLevelName}"> [<c:out value="${actOption.organizationLevelName}"/>]</c:if></span>
                                                </label>
                                            </c:if>
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

                <c:choose>
                <c:when test="${empty form and empty activity}">
                    <div class="empty-state activity-empty-state registration-empty-state registration-detail-empty-state">
                        <span class="empty-state-mark" aria-hidden="true">✓</span>
                        <strong>Chọn hoạt động để xem đợt đăng ký</strong>
                        <span>Thông tin đợt đăng ký và danh sách đơn gửi sẽ hiển thị tại đây.</span>
                    </div>
                </c:when>
                <c:when test="${empty form}">
                    <div class="empty-state activity-empty-state registration-empty-state registration-detail-empty-state">
                        <span class="empty-state-mark" aria-hidden="true">+</span>
                        <strong>Chưa có thông tin đợt đăng ký</strong>
                        <span>Hoạt động này chưa tạo form đăng ký. Bạn có thể tạo mới đợt đăng ký cho hoạt động này.</span>
                        <c:if test="${not empty activity}">
                            <a class="button button-primary"
                               href="${pageContext.request.contextPath}/registration-forms?action=create&amp;activityId=${activity.id}&amp;from=detail">
                                Tạo mới đợt đăng ký
                            </a>
                        </c:if>
                    </div>
                </c:when>
                <c:otherwise>
                <div class="registration-period-summary">
                    <div class="attendance-summary-item">
                        <span class="attendance-summary-label">Tiêu đề form</span>
                        <strong class="activity-name"><c:out value="${form.title}"/></strong>
                    </div>
                    <div class="attendance-summary-item">
                        <span class="attendance-summary-label">Cấp tổ chức (Hệ số)</span>
                        <span class="activity-time">
                            <c:out value="${not empty activity and not empty activity.organizationLevelName ? activity.organizationLevelName : 'Mặc định'}"/>
                            <c:if test="${not empty activity and not empty activity.formattedOrganizationLevelCoefficient}">
                                (x<c:out value="${activity.formattedOrganizationLevelCoefficient}"/>)
                            </c:if>
                        </span>
                    </div>
                    <div class="attendance-summary-item">
                        <span class="attendance-summary-label">Mở đăng ký</span>
                        <span class="activity-time">
                            <c:choose>
                                <c:when test="${not empty form.openAt}"><c:out value="${dt:format(form.openAt)}"/></c:when>
                                <c:otherwise>Không giới hạn</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="attendance-summary-item">
                        <span class="attendance-summary-label">Đóng đăng ký</span>
                        <span class="activity-time">
                            <c:choose>
                                <c:when test="${not empty form.closeAt}"><c:out value="${dt:format(form.closeAt)}"/></c:when>
                                <c:otherwise>Không giới hạn</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="attendance-summary-item">
                        <span class="attendance-summary-label">Số chỗ đang giữ</span>
                        <span class="activity-code">
                            <c:out value="${form.reservedSeatCount}"/> /
                            <c:choose>
                                <c:when test="${not empty form.maxParticipants}"><c:out value="${form.maxParticipants}"/></c:when>
                                <c:otherwise>Không giới hạn</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div class="attendance-summary-item">
                        <span class="attendance-summary-label">Trạng thái đợt đăng ký</span>
                        <span class="badge status-pill ${st:css(form.status)}">${st:label(form.status)}</span>
                    </div>
                </div>
                <div class="activity-tabs-nav registration-detail-tabs" role="tablist">
                    <button type="button" class="activity-tab-btn active" id="btn-tab-form-info"
                            data-tab="tab-form-info" role="tab" aria-selected="true" aria-controls="tab-form-info">
                        Câu hỏi đăng ký
                    </button>
                    <button type="button" class="activity-tab-btn" id="btn-tab-form-applications"
                            data-tab="tab-form-applications" role="tab" aria-selected="false" aria-controls="tab-form-applications">
                        Đơn đã gửi <span class="activity-tab-badge"><c:out value="${fn:length(applications)}"/></span>
                    </button>
                </div>

                <div id="tab-form-info" class="activity-tab-pane active registration-info-pane" role="tabpanel" aria-labelledby="btn-tab-form-info">
                    <section class="activity-detail-section">
                        <dl class="detail-grid activity-detail-grid">
                            <c:set var="hasQuestions" value="false"/>
                            <c:set var="questionNumber" value="0"/>
                            <c:forEach items="${form.fields}" var="field">
                                <c:if test="${not empty field.fieldKey and fn:startsWith(field.fieldKey, 'question_')}">
                                    <c:set var="hasQuestions" value="true"/>
                                    <c:set var="questionNumber" value="${questionNumber + 1}"/>
                                    <div class="detail-item field-wide">
                                        <dt>Câu <c:out value="${questionNumber}"/>: <c:out value="${field.label}"/>
                                            <c:if test="${field.required}"><span class="required-mark">*</span></c:if>
                                        </dt>
                                        <dd>
                                            <c:choose>
                                                <c:when test="${field.fieldType eq 'SELECT'}">Dropdown — chọn một đáp án</c:when>
                                                <c:when test="${field.fieldType eq 'MULTISELECT' or field.fieldType eq 'CHECKBOX'}">Chọn nhiều đáp án</c:when>
                                                <c:otherwise>Tự luận</c:otherwise>
                                            </c:choose>
                                            <c:if test="${not empty field.optionList}">
                                                <ul class="registration-detail-question-options">
                                                    <c:forEach items="${field.optionList}" var="option">
                                                        <li><c:out value="${option}"/></li>
                                                    </c:forEach>
                                                </ul>
                                            </c:if>
                                        </dd>
                                    </div>
                                </c:if>
                            </c:forEach>
                            <c:if test="${not hasQuestions}">
                                <div class="detail-item field-wide">
                                    <dt>Câu hỏi</dt>
                                    <dd>Form này không có câu hỏi.</dd>
                                </div>
                            </c:if>
                        </dl>
                    </section>
                </div>

                <div id="tab-form-applications" class="activity-tab-pane registration-applications-pane" role="tabpanel" aria-labelledby="btn-tab-form-applications">
                    <details class="activity-filter-panel" id="application-filter-panel">
                        <summary class="activity-filter-toggle">
                            <span>Tìm kiếm &amp; Bộ lọc</span>
                            <span class="activity-filter-badge" data-app-filter-badge hidden>0</span>
                        </summary>
                        <form class="activity-filter" id="application-filter-form">
                            <div class="activity-filter-field">
                                <label for="filter-app-keyword">Tìm kiếm</label>
                                <input id="filter-app-keyword" type="search" placeholder="Mã SV, họ tên hoặc email">
                            </div>
                            <div class="activity-filter-field">
                                <label for="filter-app-department">Khoa</label>
                                <select id="filter-app-department">
                                    <option value="">Tất cả</option>
                                </select>
                            </div>
                            <div class="activity-filter-field">
                                <label for="filter-app-major">Ngành</label>
                                <select id="filter-app-major">
                                    <option value="">Tất cả</option>
                                </select>
                            </div>
                            <div class="activity-filter-field">
                                <label for="filter-app-program">Chương trình đào tạo</label>
                                <select id="filter-app-program">
                                    <option value="">Tất cả</option>
                                </select>
                            </div>
                            <div class="activity-filter-field">
                                <label for="filter-app-class">Lớp</label>
                                <select id="filter-app-class">
                                    <option value="">Tất cả</option>
                                </select>
                            </div>
                            <div class="activity-filter-field">
                                <label for="filter-app-status">Trạng thái đơn</label>
                                <select id="filter-app-status">
                                    <option value="">Tất cả</option>
                                    <option value="SUBMITTED">Chờ xử lý</option>
                                    <option value="APPROVED">Đã duyệt</option>
                                    <option value="REJECTED">Từ chối</option>
                                    <option value="CANCELLED">Sinh viên đã hủy</option>
                                    <option value="WITHDRAWN">Sinh viên đã rút đơn</option>
                                </select>
                            </div>
                            <div class="activity-filter-buttons">
                                <button type="submit" class="button button-primary button-sm">Tìm kiếm &amp; Lọc</button>
                                <button type="button" class="button button-sm" data-app-filter-reset hidden>Xóa Bộ lọc</button>
                            </div>
                        </form>
                    </details>

                    <ul class="application-count" aria-label="Thống kê đơn đăng ký">
                        <li class="badge status-pill status-yellow">Chờ xử lý: <strong><c:out value="${submittedCount}"/></strong></li>
                        <li class="badge status-pill status-green">Đã duyệt: <strong><c:out value="${approvedCount}"/></strong></li>
                        <li class="badge status-pill status-red">Từ chối: <strong><c:out value="${rejectedCount}"/></strong></li>
                        <li class="badge status-pill status-gray">Đã hủy / Rút: <strong><c:out value="${cancelledOrWithdrawnCount}"/></strong></li>
                    </ul>
                    <c:if test="${not form.quick}">
                        <form class="registration-bulk-toolbar-wrap" method="post"
                              action="${pageContext.request.contextPath}/registration-forms"
                              id="application-bulk-review" data-bulk-review-form>
                            <input type="hidden" name="action" value="bulkApproveApplications" data-bulk-review-action>
                            <input type="hidden" name="formId" value="${form.id}">
                            <div class="activity-bulk-toolbar">
                                <span class="activity-bulk-selection" data-selected-count>
                                    Tick chọn đơn đang chờ xử lý để thao tác hàng loạt (chọn các đơn đầu hàng đợi, tối đa 100 đơn).
                                </span>
                                <div class="activity-bulk-actions">
                                    <button class="button button-sm action-btn action-btn--bulk-approve" type="submit"
                                            formnovalidate data-bulk-approve disabled>
                                        <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></svg></span>
                                        Duyệt đã chọn (<span data-bulk-approve-count>0</span>)
                                    </button>
                                    <button class="button button-sm action-btn action-btn--bulk-reject" type="button"
                                            data-show-bulk-reason disabled>
                                        <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg></span>
                                        Từ chối đã chọn (<span data-bulk-reject-count>0</span>)
                                    </button>
                                </div>
                            </div>
                            <div class="registration-reject-reason-panel" data-bulk-reason-panel hidden>
                                <label for="bulkRejectionReason">Lý do từ chối <span class="required-mark">*</span></label>
                                <textarea id="bulkRejectionReason" name="rejectionReason" rows="2"
                                          maxlength="1000" data-bulk-rejection-reason
                                          placeholder="Một lý do sẽ được ghi cho tất cả đơn đã chọn"></textarea>
                                <button class="button button-sm action-btn action-btn--bulk-reject" type="submit"
                                        data-confirm-bulk-reject disabled>Xác nhận từ chối</button>
                                <button class="button button-sm button-secondary" type="button"
                                        data-cancel-bulk-reject>Hủy</button>
                            </div>
                        </form>
                    </c:if>

                    <c:set var="appColCount" value="${form.quick ? 8 : 9}"/>
                    <div class="table-wrap activity-table-wrap">
                        <table>
                            <thead>
                            <tr>
                                <c:if test="${not form.quick}">
                                    <th scope="col" class="activity-select-column">
                                        <input type="checkbox" class="activity-select-checkbox"
                                               data-select-all-applications
                                               aria-label="Chọn tất cả đơn đang chờ xử lý"
                                               ${submittedCount == 0 ? 'disabled' : ''}>
                                    </th>
                                </c:if>
                                <th scope="col" class="col-code">Mã SV</th>
                                <th scope="col" class="col-object-name">Họ tên</th>
                                <th scope="col" class="col-academic">Khoa / Ngành</th>
                                <th scope="col" class="col-academic">CTĐT / Lớp</th>
                                <th scope="col" class="col-single-line">Email</th>
                                <th scope="col" class="col-datetime">Thời gian gửi</th>
                                <th scope="col" class="col-status activity-status-column">Trạng thái duyệt</th>
                                <th scope="col" class="activity-action-column">Thao tác</th>
                            </tr>
                            </thead>
                            <tbody data-applications-tbody>
                            <c:choose>
                                <c:when test="${empty applications}">
                                    <tr>
                                        <td class="empty-state activity-empty-state" colspan="${appColCount}">
                                            <span class="empty-state-mark" aria-hidden="true">+</span>
                                            <strong>Chưa có sinh viên gửi đơn</strong>
                                            <span>Đơn đăng ký sẽ xuất hiện tại đây sau khi sinh viên gửi form.</span>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach items="${applications}" var="application">
                                        <tr data-application-row
                                            data-student-code="<c:out value='${application.studentCode}'/>"
                                            data-student-name="<c:out value='${application.studentName}'/>"
                                            data-department="<c:out value='${application.departmentName}'/>"
                                            data-major="<c:out value='${application.majorName}'/>"
                                            data-program="<c:out value='${application.academicProgramName}'/>"
                                            data-class-code="<c:out value='${application.classCode}'/>"
                                            data-email="<c:out value='${application.email}'/>"
                                            data-status="<c:out value='${application.status}'/>">
                                            <c:if test="${not form.quick}">
                                                <td class="activity-select-column">
                                                    <c:choose>
                                                        <c:when test="${application.status eq 'SUBMITTED'}">
                                                            <input type="checkbox" class="activity-select-checkbox"
                                                                   name="registrationIds" value="${application.id}"
                                                                   data-application-selection
                                                                   form="application-bulk-review"
                                                                   aria-label="Chọn đơn của <c:out value='${application.studentName}'/>">
                                                        </c:when>
                                                        <c:otherwise>
                                                            <input type="checkbox" class="activity-select-checkbox" disabled
                                                                   aria-label="Đơn này không ở trạng thái chờ xử lý">
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                            </c:if>
                                            <td class="col-code">
                                                <span class="activity-code"><c:out value="${application.studentCode}" default="—"/></span>
                                            </td>
                                            <td class="col-object-name" title="<c:out value='${application.studentName}'/>">
                                                <strong class="activity-name" title="<c:out value='${application.studentName}'/>"><c:out value="${application.studentName}" default="—"/></strong>
                                            </td>
                                            <td class="col-academic" title="<c:out value='${application.departmentName} - ${application.majorName}'/>">
                                                <c:out value="${application.departmentName}" default="—"/>
                                                <c:if test="${not empty application.majorName}"><br><small class="text-muted"><c:out value="${application.majorName}"/></small></c:if>
                                            </td>
                                            <td class="col-academic" title="<c:out value='${application.academicProgramName} - ${application.classCode}'/>">
                                                <c:out value="${application.academicProgramName}" default="—"/>
                                                <c:if test="${not empty application.classCode}"><br><small class="text-muted"><c:out value="${application.classCode}"/></small></c:if>
                                            </td>
                                            <td class="col-single-line" title="<c:out value='${application.email}'/>">
                                                <c:out value="${application.email}" default="—"/>
                                            </td>
                                            <td class="activity-time col-datetime">
                                                <c:choose>
                                                    <c:when test="${not empty application.submittedAt}"><c:out value="${dt:format(application.submittedAt)}"/></c:when>
                                                    <c:otherwise>—</c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="col-status activity-status-column">
                                                <c:choose>
                                                    <c:when test="${application.status eq 'APPROVED'}"><span class="badge status-pill status-green">Đã duyệt</span></c:when>
                                                    <c:when test="${application.status eq 'REJECTED'}">
                                                        <span class="badge status-pill status-red">Từ chối</span>
                                                        <c:if test="${not empty application.rejectionReason}">
                                                            <div class="registration-list-rejection-reason" title="<c:out value='${application.rejectionReason}'/>">Lý do: <c:out value="${application.rejectionReason}"/></div>
                                                        </c:if>
                                                    </c:when>
                                                    <c:when test="${application.status eq 'CANCELLED'}"><span class="badge status-pill status-gray">Sinh viên đã hủy</span></c:when>
                                                    <c:when test="${application.status eq 'WITHDRAWN'}"><span class="badge status-pill status-blue">Sinh viên đã rút đơn</span></c:when>
                                                    <c:otherwise><span class="badge status-pill status-yellow">Chờ xử lý</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="activity-action-column">
                                                <div class="actions activity-actions">
                                                    <a class="activity-action-link action-btn action-btn--view"
                                                       href="${pageContext.request.contextPath}/registration-forms?action=application&amp;id=${application.id}">
                                                        <span class="action-btn__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></span>
                                                        Xem đơn
                                                    </a>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <tr data-empty-filter-row hidden>
                                        <td class="empty-state activity-empty-state" colspan="${appColCount}">
                                            <span class="empty-state-mark" aria-hidden="true">+</span>
                                            <strong>Không tìm thấy sinh viên phù hợp</strong>
                                            <span>Hãy thử thay đổi từ khóa hoặc bộ lọc.</span>
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <c:if test="${not empty applications}">
                        <div class="activity-pagination">
                            <span class="activity-pagination-summary" data-app-pagination-summary>
                                Hiển thị 1–<c:out value="${fn:length(applications)}"/> trong <c:out value="${fn:length(applications)}"/> sinh viên
                            </span>
                        </div>
                    </c:if>
                </div>
                </c:otherwise>
                </c:choose>
            </section>
        </main>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/action-menu.js?v=5"></script>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261107-5"></script>
<script>
    (function() {
        const bulkForm = document.querySelector('[data-bulk-review-form]');
        const selectAllCheckbox = document.querySelector('[data-select-all-applications]');
        if (bulkForm) {
            const selections = Array.from(document.querySelectorAll(
                '[form="application-bulk-review"][data-application-selection]'));
            const actionInput = bulkForm.querySelector('[data-bulk-review-action]');
            const countLabel = bulkForm.querySelector('[data-selected-count]');
            const approveButton = bulkForm.querySelector('[data-bulk-approve]');
            const rejectButton = bulkForm.querySelector('[data-show-bulk-reason]');
            const approveCountBadge = bulkForm.querySelector('[data-bulk-approve-count]');
            const rejectCountBadge = bulkForm.querySelector('[data-bulk-reject-count]');
            const reasonPanel = bulkForm.querySelector('[data-bulk-reason-panel]');
            const reasonInput = bulkForm.querySelector('[data-bulk-rejection-reason]');
            const confirmReject = bulkForm.querySelector('[data-confirm-bulk-reject]');
            const cancelReject = bulkForm.querySelector('[data-cancel-bulk-reject]');
            const isOutOfOrderSelection = function() {
                let seenUnchecked = false;
                for (let i = 0; i < selections.length; i++) {
                    if (selections[i].checked) {
                        if (seenUnchecked) return true;
                    } else {
                        seenUnchecked = true;
                    }
                }
                return false;
            };
            const updateSelection = function() {
                const selectedCount = selections.filter(function(item) { return item.checked; }).length;
                const outOfOrder = selectedCount > 0 && isOutOfOrderSelection();
                countLabel.textContent = selectedCount === 0
                    ? 'Tick chọn đơn đang chờ xử lý để thao tác hàng loạt (tối đa 100 đơn).'
                    : 'Đã chọn ' + selectedCount + ' đơn đang chờ xử lý'
                        + (outOfOrder ? ' (Cảnh báo: Còn đơn chờ xử lý được gửi trước chưa được chọn)' : '');
                if (approveCountBadge) approveCountBadge.textContent = selectedCount;
                if (rejectCountBadge) rejectCountBadge.textContent = selectedCount;
                approveButton.disabled = selectedCount === 0 || selectedCount > 100;
                rejectButton.disabled = selectedCount === 0 || selectedCount > 100;
                confirmReject.disabled = selectedCount === 0 || selectedCount > 100
                    || !reasonInput.value.trim();
                if (selectAllCheckbox) {
                    const maxSelectable = Math.min(selections.length, 100);
                    selectAllCheckbox.checked = maxSelectable > 0 && selectedCount === maxSelectable;
                    selectAllCheckbox.indeterminate = selectedCount > 0 && selectedCount < maxSelectable;
                }
            };
            selections.forEach(function(item) {
                item.addEventListener('change', function() {
                    const selectedCount = selections.filter(function(el) { return el.checked; }).length;
                    if (item.checked && selectedCount > 100) {
                        item.checked = false;
                        return;
                    }
                    updateSelection();
                });
            });
            if (selectAllCheckbox) {
                selectAllCheckbox.addEventListener('change', function() {
                    const select = selectAllCheckbox.checked;
                    selections.forEach(function(item, index) { item.checked = select && index < 100; });
                    updateSelection();
                });
            }
            rejectButton.addEventListener('click', function() {
                reasonPanel.hidden = false;
                reasonInput.required = true;
                reasonInput.focus();
            });
            cancelReject.addEventListener('click', function() {
                reasonPanel.hidden = true;
                reasonInput.required = false;
                reasonInput.value = '';
                updateSelection();
            });
            reasonInput.addEventListener('input', updateSelection);
            approveButton.addEventListener('click', function() {
                actionInput.value = 'bulkApproveApplications';
                reasonPanel.hidden = true;
                reasonInput.required = false;
            });
            confirmReject.addEventListener('click', function() {
                actionInput.value = 'bulkRejectApplications';
            });
            bulkForm.addEventListener('submit', function(event) {
                const selectedCount = selections.filter(function(item) { return item.checked; }).length;
                const rejecting = actionInput.value === 'bulkRejectApplications';
                if (selectedCount === 0 || selectedCount > 100
                        || (rejecting && !reasonInput.value.trim())) {
                    event.preventDefault();
                    return;
                }
                const outOfOrder = isOutOfOrderSelection();
                const confirmMsg = outOfOrder
                    ? 'Cảnh báo: Còn đơn chờ xử lý được gửi trước chưa được chọn. Bạn vẫn muốn '
                        + (rejecting ? 'từ chối ' : 'duyệt ') + selectedCount + ' đơn đã chọn?'
                    : (rejecting ? 'Từ chối ' : 'Duyệt ') + selectedCount + ' đơn đã chọn?';
                if (!window.confirm(confirmMsg)) {
                    event.preventDefault();
                }
            });
            updateSelection();
        }

        const rows = Array.from(document.querySelectorAll('[data-application-row]'));
        const filterForm = document.getElementById('application-filter-form');
        if (filterForm && rows.length > 0) {
            const keywordInput = document.getElementById('filter-app-keyword');
            const departmentSelect = document.getElementById('filter-app-department');
            const majorSelect = document.getElementById('filter-app-major');
            const programSelect = document.getElementById('filter-app-program');
            const classSelect = document.getElementById('filter-app-class');
            const statusSelect = document.getElementById('filter-app-status');
            const resetBtn = filterForm.querySelector('[data-app-filter-reset]');
            const badge = document.querySelector('[data-app-filter-badge]');
            const emptyFilterRow = document.querySelector('[data-empty-filter-row]');
            const paginationSummary = document.querySelector('[data-app-pagination-summary]');
            const headerCount = document.querySelector('[data-header-app-count]');

            const populateSelectOptions = function(selectEl, attrName) {
                if (!selectEl) return;
                const values = Array.from(new Set(rows.map(function(r) {
                    return (r.getAttribute(attrName) || '').trim();
                }).filter(Boolean))).sort(function(a, b) {
                    return a.localeCompare(b, 'vi');
                });
                values.forEach(function(val) {
                    const opt = document.createElement('option');
                    opt.value = val;
                    opt.textContent = val;
                    selectEl.appendChild(opt);
                });
            };

            populateSelectOptions(departmentSelect, 'data-department');
            populateSelectOptions(majorSelect, 'data-major');
            populateSelectOptions(programSelect, 'data-program');
            populateSelectOptions(classSelect, 'data-class-code');

            const applyFilter = function() {
                const kw = (keywordInput.value || '').trim().toLowerCase();
                const dept = departmentSelect ? departmentSelect.value : '';
                const major = majorSelect ? majorSelect.value : '';
                const prog = programSelect ? programSelect.value : '';
                const cls = classSelect.value;
                const st = statusSelect.value;
                let activeFilters = 0;
                if (kw) activeFilters++;
                if (dept) activeFilters++;
                if (major) activeFilters++;
                if (prog) activeFilters++;
                if (cls) activeFilters++;
                if (st) activeFilters++;

                if (badge) {
                    badge.hidden = activeFilters === 0;
                    badge.textContent = activeFilters;
                }
                if (resetBtn) {
                    resetBtn.hidden = activeFilters === 0;
                }

                let visibleCount = 0;
                rows.forEach(function(row) {
                    const code = (row.getAttribute('data-student-code') || '').toLowerCase();
                    const name = (row.getAttribute('data-student-name') || '').toLowerCase();
                    const email = (row.getAttribute('data-email') || '').toLowerCase();
                    const rowDept = (row.getAttribute('data-department') || '').trim();
                    const rowMajor = (row.getAttribute('data-major') || '').trim();
                    const rowProg = (row.getAttribute('data-program') || '').trim();
                    const rowClass = (row.getAttribute('data-class-code') || '').trim();
                    const rowStatus = (row.getAttribute('data-status') || '').trim();

                    const matchKw = !kw || code.includes(kw) || name.includes(kw) || email.includes(kw);
                    const matchDept = !dept || rowDept === dept;
                    const matchMajor = !major || rowMajor === major;
                    const matchProg = !prog || rowProg === prog;
                    const matchCls = !cls || rowClass === cls;
                    const matchSt = !st || rowStatus === st;
                    const visible = matchKw && matchDept && matchMajor && matchProg && matchCls && matchSt;
                    row.hidden = !visible;
                    if (visible) visibleCount++;
                });

                if (emptyFilterRow) {
                    emptyFilterRow.hidden = visibleCount > 0;
                }
                if (paginationSummary) {
                    paginationSummary.textContent = visibleCount === 0
                        ? 'Hiển thị 0 trong ' + rows.length + ' sinh viên'
                        : 'Hiển thị 1–' + visibleCount + ' trong ' + rows.length + ' sinh viên';
                }
                if (headerCount) {
                    headerCount.textContent = activeFilters > 0
                        ? visibleCount + ' / ' + rows.length + ' sinh viên đăng ký'
                        : rows.length + ' sinh viên đăng ký';
                }
            };

            filterForm.addEventListener('submit', function(e) {
                e.preventDefault();
                applyFilter();
            });
            keywordInput.addEventListener('input', applyFilter);
            if (departmentSelect) departmentSelect.addEventListener('change', applyFilter);
            if (majorSelect) majorSelect.addEventListener('change', applyFilter);
            if (programSelect) programSelect.addEventListener('change', applyFilter);
            classSelect.addEventListener('change', applyFilter);
            statusSelect.addEventListener('change', applyFilter);
            if (resetBtn) {
                resetBtn.addEventListener('click', function() {
                    keywordInput.value = '';
                    if (departmentSelect) departmentSelect.value = '';
                    if (majorSelect) majorSelect.value = '';
                    if (programSelect) programSelect.value = '';
                    classSelect.value = '';
                    statusSelect.value = '';
                    applyFilter();
                });
            }
        }

        const urlParams = new URLSearchParams(window.location.search);
        const tab = urlParams.get('tab');
        if (tab === 'applications') {
            const tabBtn = document.getElementById('btn-tab-form-applications');
            const tabPane = document.getElementById('tab-form-applications');
            const infoBtn = document.getElementById('btn-tab-form-info');
            const infoPane = document.getElementById('tab-form-info');
            if (tabBtn && tabPane && infoBtn && infoPane) {
                infoBtn.classList.remove('active');
                infoBtn.setAttribute('aria-selected', 'false');
                infoPane.classList.remove('active');
                tabBtn.classList.add('active');
                tabBtn.setAttribute('aria-selected', 'true');
                tabPane.classList.add('active');
            }
        }

        document.querySelectorAll('.multi-select-combobox[data-auto-submit]').forEach(function(combobox) {
            const form = combobox.closest('form');
            if (!form) return;
            combobox.addEventListener('change', function(event) {
                const input = event.target;
                if (!input || input.type !== 'radio' || !input.checked) return;
                form.submit();
            });
        });
    })();
</script>
</body>
</html>
