<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="registration-forms" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết đợt đăng ký | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261104-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261104-9">
</head>
<body>
<div class="activity-app-shell">
    <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
    <div class="activity-app-layout">
        <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
        <div class="activity-app-main">
            <main class="page activity-list-page activity-form-page activity-detail-page">
                <header class="activity-hero">
                    <div class="activity-hero-copy">
                        <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ ĐĂNG KÝ</span>
                        <h1>Chi tiết đợt đăng ký</h1>
                        <p><c:out value="${form.title}"/></p>
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

                <section class="card activity-form-card activity-detail-card registration-detail-card">
                    <div class="activity-table-heading">
                        <div>
                            <h2>Thông tin chi tiết form</h2>
                            <p>Chi tiết nội dung và tình trạng đăng ký của form.</p>
                        </div>
                        <div class="registration-detail-actions">
                            <a class="button button-back" href="<c:out value='${returnTo}'/>">Quay lại</a>
                            <a class="button button-secondary" href="${pageContext.request.contextPath}/registration-forms?action=list">Danh sách form</a>

                                <a class="button button-primary" href="${pageContext.request.contextPath}/registration-forms?action=edit&amp;id=${form.id}&amp;from=detail">Chỉnh sửa</a>

                        </div>
                    </div>

                    <div class="activity-tabs-nav registration-detail-tabs" role="tablist">
                        <button type="button" class="activity-tab-btn active" id="btn-tab-form-info"
                                data-tab="tab-form-info" role="tab" aria-selected="true" aria-controls="tab-form-info">
                            Thông tin chi tiết form
                        </button>
                        <button type="button" class="activity-tab-btn" id="btn-tab-form-applications"
                                data-tab="tab-form-applications" role="tab" aria-selected="false" aria-controls="tab-form-applications">
                            Đơn đã gửi <span class="activity-tab-badge"><c:out value="${form.registrationCount}"/></span>
                        </button>
                    </div>

                    <div class="activity-detail-content registration-detail-tab-content">
                        <div id="tab-form-info" class="activity-tab-pane active" role="tabpanel" aria-labelledby="btn-tab-form-info">
                            <section class="activity-detail-section">
                                <h3>Thông tin cơ bản</h3>
                                <dl class="detail-grid activity-detail-grid">
                                    <div class="detail-item">
                                        <dt>Tiêu đề form</dt>
                                        <dd><c:out value="${form.title}"/></dd>
                                    </div>
                                    <div class="detail-item">
                                        <dt>Hoạt động</dt>
                                        <dd><c:out value="${form.activityName}"/></dd>
                                    </div>
                                    <div class="detail-item">
                                        <dt>Trạng thái</dt>
                                        <dd><span class="badge status-pill ${st:css(form.status)}">${st:label(form.status)}</span></dd>
                                    </div>
                                    <div class="detail-item">
                                        <dt>Số chỗ đang giữ</dt>
                                        <dd>
                                            <c:choose>
                                                <c:when test="${not empty form.maxParticipants}">
                                                    <c:out value="${form.reservedSeatCount}"/> / <c:out value="${form.maxParticipants}"/>
                                                </c:when>
                                                <c:otherwise><c:out value="${form.reservedSeatCount}"/> / Không giới hạn</c:otherwise>
                                            </c:choose>
                                        </dd>
                                    </div>
                                </dl>
                            </section>

                            <section class="activity-detail-section">
                                <h3>Thời gian đăng ký</h3>
                                <dl class="detail-grid activity-detail-grid">
                                    <div class="detail-item">
                                        <dt>Mở đăng ký</dt>
                                        <dd><c:choose><c:when test="${not empty form.openAt}"><c:out value="${dt:format(form.openAt)}"/></c:when><c:otherwise>Không giới hạn</c:otherwise></c:choose></dd>
                                    </div>
                                    <div class="detail-item">
                                        <dt>Đóng đăng ký</dt>
                                        <dd><c:choose><c:when test="${not empty form.closeAt}"><c:out value="${dt:format(form.closeAt)}"/></c:when><c:otherwise>Không giới hạn</c:otherwise></c:choose></dd>
                                    </div>
                                </dl>
                            </section>

                            <section class="activity-detail-section">
                                <h3>Mô tả và hướng dẫn</h3>
                                <dl class="detail-grid activity-detail-grid">
                                    <div class="detail-item field-wide">
                                        <dt>Nội dung</dt>
                                        <dd class="activity-detail-description"><c:out value="${form.description}" default="Chưa có mô tả"/></dd>
                                    </div>
                                </dl>
                            </section>

                            <section class="activity-detail-section">
                                <h3>Câu hỏi bổ sung</h3>
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
                                            <dd>Form này không có câu hỏi bổ sung.</dd>
                                        </div>
                                    </c:if>
                                </dl>
                            </section>
                        </div>

                        <div id="tab-form-applications" class="activity-tab-pane" role="tabpanel" aria-labelledby="btn-tab-form-applications">
                            <section class="activity-detail-section registration-applications-tab">
                                <div class="registration-applications-heading">
                                    <div>
                                        <h3>Đơn sinh viên đăng ký</h3>
                                        <p>Các đơn đã gửi cho form này, bao gồm trạng thái xét duyệt.</p>
                                    </div>
                                </div>
                                <c:if test="${not form.quick}">
                                    <form class="registration-bulk-reject-form" method="post"
                                          action="${pageContext.request.contextPath}/registration-forms"
                                          id="application-bulk-review" data-bulk-review-form>
                                    <input type="hidden" name="action" value="bulkApproveApplications"
                                           data-bulk-review-action>
                                    <input type="hidden" name="formId" value="${form.id}">
                                    <div class="registration-bulk-reject-controls">
                                        <span class="registration-selected-count" data-selected-count>Chưa chọn đơn nào</span>
                                        <span class="registration-bulk-hint">Tối đa 100 đơn chờ, theo thứ tự gửi</span>
                                        <button class="registration-bulk-select-all" type="button"
                                                data-select-all-applications>Chọn tất cả đơn chờ</button>
                                        <button class="button registration-bulk-reject" type="button"
                                                data-show-bulk-reason disabled>Từ chối đơn đã chọn</button>
                                        <button class="button registration-bulk-approve" type="submit"
                                                data-bulk-approve disabled>Duyệt đơn đã chọn</button>
                                    </div>
                                    <div class="registration-reject-reason-panel" data-bulk-reason-panel hidden>
                                        <label for="bulkRejectionReason">Lý do từ chối <span class="required-mark">*</span></label>
                                        <textarea id="bulkRejectionReason" name="rejectionReason" rows="2"
                                                  maxlength="1000" data-bulk-rejection-reason
                                                  placeholder="Một lý do sẽ được ghi cho tất cả đơn đã chọn"></textarea>
                                        <button class="button registration-bulk-reject" type="submit"
                                                data-confirm-bulk-reject disabled>Xác nhận từ chối</button>
                                        <button class="button button-secondary" type="button"
                                                data-cancel-bulk-reject>Hủy</button>
                                    </div>
                                    </form>
                                </c:if>
                                <div class="table-wrap activity-table-wrap">
                                    <table class="registration-table registration-applications-table registration-form-applications-table ${form.quick ? 'registration-form-applications-table--quick' : 'registration-form-applications-table--review'}">
                                        <thead>
                                        <tr>
                                            <c:if test="${not form.quick}">
                                                <th class="registration-select-column" aria-label="Chọn đơn"></th>
                                            </c:if>
                                            <th>Sinh viên (Mã sinh viên)</th>
                                            <th>Lớp</th>
                                            <th>Email</th>
                                            <th class="registration-sticky-status">Trạng thái</th>
                                            <th class="registration-sticky-actions">Chi tiết</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        <c:forEach items="${applications}" var="application">
                                            <tr>
                                                <c:if test="${not form.quick}">
                                                    <td class="registration-select-column">
                                                    <c:if test="${application.status eq 'SUBMITTED'}">
                                                        <input type="checkbox" name="registrationIds"
                                                               value="${application.id}" data-application-selection
                                                               form="application-bulk-review"
                                                               aria-label="Chọn đơn đăng ký">
                                                    </c:if>
                                                    </td>
                                                </c:if>
                                                <td>
                                                    <strong><c:out value="${application.studentName}"/></strong>
                                                    <div class="muted">Mã sinh viên: <c:out value="${application.studentCode}"/></div>
                                                </td>
                                                <td><c:out value="${application.classCode}" default="—"/></td>
                                                <td><c:out value="${application.email}" default="—"/></td>
                                                <td class="registration-sticky-status">
                                                    <c:choose>
                                                        <c:when test="${application.status eq 'APPROVED'}"><span class="badge status-pill status-green">Đã duyệt</span></c:when>
                                                        <c:when test="${application.status eq 'REJECTED'}"><span class="badge status-pill status-red">Từ chối</span><c:if test="${not empty application.rejectionReason}"><div class="registration-list-rejection-reason">Lý do: <c:out value="${application.rejectionReason}"/></div></c:if></c:when>
                                                        <c:when test="${application.status eq 'CANCELLED'}"><span class="badge status-pill status-gray">Sinh viên đã hủy</span></c:when>
                                                        <c:when test="${application.status eq 'WITHDRAWN'}"><span class="badge status-pill status-blue">Sinh viên đã rút đơn</span></c:when>
                                                        <c:otherwise><span class="badge status-pill status-yellow">Chờ xử lý</span></c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="registration-sticky-actions"><a class="activity-action-link" href="${pageContext.request.contextPath}/registration-forms?action=application&amp;id=${application.id}">Xem đơn</a></td>
                                            </tr>
                                        </c:forEach>
                                        <c:if test="${empty applications}">
                                            <tr>
                                                <td class="empty-state registration-empty-state"
                                                    colspan="${form.quick ? 5 : 6}">
                                                    <strong>Chưa có sinh viên gửi đơn</strong>
                                                    <span>Đơn đăng ký sẽ xuất hiện tại đây sau khi sinh viên gửi form.</span>
                                                </td>
                                            </tr>
                                        </c:if>
                                        </tbody>
                                    </table>
                                </div>
                            </section>
                        </div>
                    </div>
                </section>
            </main>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/activity.js?v=20261020-2"></script>
<script>
    (function() {
        const bulkForm = document.querySelector('[data-bulk-review-form]');
        if (bulkForm) {
            const selections = Array.from(document.querySelectorAll(
                '[form="application-bulk-review"][data-application-selection]'));
            const actionInput = bulkForm.querySelector('[data-bulk-review-action]');
            const countLabel = bulkForm.querySelector('[data-selected-count]');
            const approveButton = bulkForm.querySelector('[data-bulk-approve]');
            const rejectButton = bulkForm.querySelector('[data-show-bulk-reason]');
            const selectAllButton = bulkForm.querySelector('[data-select-all-applications]');
            const reasonPanel = bulkForm.querySelector('[data-bulk-reason-panel]');
            const reasonInput = bulkForm.querySelector('[data-bulk-rejection-reason]');
            const confirmReject = bulkForm.querySelector('[data-confirm-bulk-reject]');
            const cancelReject = bulkForm.querySelector('[data-cancel-bulk-reject]');
            const updateSelection = function() {
                const selectedCount = selections.filter(function(item) { return item.checked; }).length;
                countLabel.textContent = selectedCount === 0
                    ? 'Chưa chọn đơn nào'
                    : 'Đã chọn ' + selectedCount + ' đơn';
                approveButton.disabled = selectedCount === 0 || selectedCount > 100;
                rejectButton.disabled = selectedCount === 0 || selectedCount > 100;
                confirmReject.disabled = selectedCount === 0 || selectedCount > 100
                    || !reasonInput.value.trim();
                selectAllButton.textContent = selectedCount === selections.length && selections.length > 0
                    ? 'Bỏ chọn tất cả'
                    : 'Chọn tất cả đơn chờ';
            };
            selections.forEach(function(item) {
                item.addEventListener('change', updateSelection);
            });
            selectAllButton.addEventListener('click', function() {
                const select = selections.some(function(item) { return !item.checked; });
                selections.forEach(function(item, index) { item.checked = select && index < 100; });
                updateSelection();
            });
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
                if (!window.confirm((rejecting ? 'Từ chối ' : 'Duyệt ') + selectedCount
                        + ' đơn đang chờ theo thứ tự gửi?')) {
                    event.preventDefault();
                }
            });
            updateSelection();
        }

        const rejectionForm = document.querySelector('[data-individual-rejection-form]');
        if (rejectionForm) {
            const showReason = document.querySelector('[data-show-rejection-reason]');
            const reasonPanel = rejectionForm.querySelector('[data-individual-rejection-panel]');
            const reasonInput = rejectionForm.querySelector('[name="rejectionReason"]');
            const cancelButton = rejectionForm.querySelector('[data-cancel-rejection]');
            const confirmButton = rejectionForm.querySelector('[data-confirm-rejection]');
            showReason.addEventListener('click', function() {
                reasonPanel.hidden = false;
                reasonInput.required = true;
                reasonInput.focus();
                showReason.hidden = true;
            });
            cancelButton.addEventListener('click', function() {
                reasonPanel.hidden = true;
                reasonInput.required = false;
                reasonInput.value = '';
                showReason.hidden = false;
            });
            reasonInput.addEventListener('input', function() {
                confirmButton.disabled = !reasonInput.value.trim();
            });
            rejectionForm.addEventListener('submit', function(event) {
                if (!reasonInput.value.trim()
                        || !window.confirm('Từ chối đơn với lý do đã nhập?')) {
                    event.preventDefault();
                }
            });
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
    })();
</script>
</body>
</html>