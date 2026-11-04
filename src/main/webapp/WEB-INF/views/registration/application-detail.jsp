<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="${isStudent ? 'activities' : 'registration-forms'}" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết đơn đăng ký | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261104-2">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261104-5">
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
                        <h1><c:out value="${isStudent ? 'Chi tiết đơn đăng ký' : 'Chi tiết đơn sinh viên'}"/></h1>
                        <p><c:out value="${form.title}"/> · <c:out value="${form.activityName}"/></p>
                    </div>
                </header>

                <c:if test="${param.msg eq 'approved'}"><div class="alert alert-success activity-alert">Đã duyệt đơn đăng ký.</div></c:if>
                <c:if test="${param.msg eq 'rejected'}"><div class="alert alert-success activity-alert">Đã từ chối đơn đăng ký.</div></c:if>
                <c:if test="${param.msg eq 'reasonError'}"><div class="alert alert-error activity-alert">Vui lòng nhập lý do từ chối (tối đa 1000 ký tự).</div></c:if>
                <c:if test="${param.msg eq 'rejectionUndone'}"><div class="alert alert-success activity-alert">Đã hủy từ chối; đơn trở về trạng thái chờ duyệt và được xếp ở cuối hàng.</div></c:if>
                <c:if test="${param.msg eq 'approvalUndone'}"><div class="alert alert-success activity-alert">Đã hủy duyệt đơn; đơn trở về trạng thái chờ duyệt và được xếp ở cuối hàng.</div></c:if>
                <c:if test="${param.msg eq 'reviewError'}"><div class="alert alert-error activity-alert">Trạng thái đơn đã thay đổi hoặc không thể cập nhật.</div></c:if>
                <c:if test="${param.msg eq 'fifoError'}"><div class="alert alert-error activity-alert">Không thể xử lý đơn này vì còn đơn chờ xử lý được gửi trước. Hãy xử lý các đơn theo thứ tự gửi.</div></c:if>
                <c:if test="${param.msg eq 'capacityError'}"><div class="alert alert-error activity-alert">Không thể khôi phục đơn: hoạt động đã đủ số lượng tối đa. Đơn bị từ chối vẫn được giữ nguyên.</div></c:if>

                <section class="card activity-form-card activity-detail-card registration-detail-card">
                    <div class="activity-table-heading">
                        <div>
                            <h2><c:out value="${application.studentName}"/> <span class="badge status-pill status-blue">Đơn #<c:out value="${application.id}"/></span></h2>
                            <p>Thời gian gửi: <c:out value="${dt:format(application.submittedAt)}"/></p>
                        </div>
                        <c:choose>
                            <c:when test="${application.status eq 'APPROVED'}"><span class="badge status-pill status-green">Đã duyệt</span></c:when>
                            <c:when test="${application.status eq 'REJECTED'}"><span class="badge status-pill status-red">Đã từ chối</span></c:when>
                            <c:when test="${application.status eq 'CANCELLED'}"><span
                                    class="badge status-pill status-gray">Sinh viên đã hủy</span></c:when>
                            <c:when test="${application.status eq 'WITHDRAWN'}"><span class="badge status-pill status-blue">Sinh viên đã rút đơn</span></c:when>
                            <c:otherwise><span class="badge status-pill status-yellow">Chờ xử lý</span></c:otherwise>
                        </c:choose>
                    </div>

                    <div class="activity-detail-content">
                        <section class="activity-detail-section">
                            <h3>Thông tin sinh viên</h3>
                            <dl class="detail-grid activity-detail-grid">
                                <div class="detail-item"><dt>Mã sinh viên</dt><dd><c:out value="${application.studentCode}" default="—"/></dd></div>
                                <div class="detail-item"><dt>Họ và tên</dt><dd><c:out value="${application.studentName}" default="—"/></dd></div>
                                <div class="detail-item"><dt>Lớp</dt><dd><c:out value="${application.classCode}" default="—"/></dd></div>
                                <div class="detail-item"><dt>Email</dt><dd><c:out value="${application.email}" default="—"/></dd></div>
                                <div class="detail-item"><dt>Số điện thoại</dt><dd><c:out value="${application.phoneNumber}" default="—"/></dd></div>
                                <div class="detail-item"><dt>Ngày sinh</dt><dd><c:out value="${applicationDob}" default="—"/></dd></div>
                            </dl>
                        </section>

                        <section class="activity-detail-section">
                            <h3>Thông tin bổ sung</h3>
                            <dl class="detail-grid activity-detail-grid">
                                <c:set var="hasSupplementaryAnswers" value="false"/>
                                <c:forEach items="${questionFields}" var="field" varStatus="question">
                                    <c:set var="hasSupplementaryAnswers" value="true"/>
                                    <div class="detail-item field-wide">
                                        <dt>Câu <c:out value="${question.count}"/>: <c:out value="${field.label}"/></dt>
                                        <dd class="activity-detail-description"><c:out value="${not empty field.value ? field.value : '—'}"/></dd>
                                    </div>
                                </c:forEach>
                                <c:if test="${not hasSupplementaryAnswers}">
                                    <div class="detail-item field-wide"><dd>Không có câu trả lời bổ sung.</dd></div>
                                </c:if>
                                <c:if test="${not empty application.note}">
                                    <div class="detail-item field-wide"><dt>Ghi chú</dt><dd class="activity-detail-description"><c:out value="${application.note}"/></dd></div>
                                </c:if>
                                <c:if test="${empty application.note}">
                                    <div class="detail-item field-wide"><dt>Ghi chú</dt><dd>—</dd></div>
                                </c:if>
                            </dl>
                        </section>
                        <c:if test="${application.status eq 'REJECTED'}">
                            <section class="activity-detail-section">
                                <h3>Lý do từ chối</h3>
                                <p class="registration-rejection-reason"><c:out value="${application.rejectionReason}" default="Không có lý do được ghi nhận."/></p>
                            </section>
                        </c:if>
                    </div>

                    <div class="form-actions activity-form-actions">
                        <c:choose><c:when test="${isStudent}"><a class="button button-secondary" href="<c:out value='${returnTo}'/>">Quay lại</a></c:when><c:otherwise>
                            <a class="button button-secondary" href="${pageContext.request.contextPath}/activities?action=detail&amp;id=${form.activityId}">Quay lại hoạt động</a>
                            <a class="button button-secondary" href="${pageContext.request.contextPath}/registration-forms?action=view&amp;id=${form.id}&amp;tab=applications">Quay lại danh sách đơn</a></c:otherwise></c:choose>
                        <c:if test="${not isStudent}"><c:choose>
                            <c:when test="${application.status eq 'SUBMITTED'}">
                                <div class="registration-review-actions">
                                    <form class="registration-individual-rejection" method="post"
                                          action="${pageContext.request.contextPath}/registration-forms"
                                          data-individual-rejection-form>
                                        <input type="hidden" name="action" value="rejectApplication">
                                        <input type="hidden" name="registrationId" value="${application.id}">
                                        <button class="button registration-review-button registration-review-button--reject-outline"
                                                type="button" data-show-rejection-reason>Từ chối đơn</button>
                                        <div class="registration-individual-rejection-panel"
                                             data-individual-rejection-panel hidden>
                                            <label for="rejectionReason">Lý do từ chối <span class="required-mark">*</span></label>
                                            <textarea id="rejectionReason" name="rejectionReason" rows="3"
                                                      maxlength="1000" placeholder="Nhập lý do từ chối"></textarea>
                                            <div class="registration-rejection-actions">
                                                <button class="button registration-review-button registration-review-button--reject"
                                                        type="submit" data-confirm-rejection disabled>Xác nhận từ chối</button>
                                                <button class="button button-secondary" type="button"
                                                        data-cancel-rejection>Hủy</button>
                                            </div>
                                        </div>
                                    </form>
                                    <form method="post" action="${pageContext.request.contextPath}/registration-forms">
                                        <input type="hidden" name="action" value="approveApplication">
                                        <input type="hidden" name="registrationId" value="${application.id}">
                                        <button class="button registration-review-button registration-review-button--approve"
                                                type="submit">Duyệt đơn</button>
                                    </form>
                                </div>
                            </c:when>
                            <c:when test="${application.status eq 'APPROVED' and not form.quick}">
                                <form method="post" action="${pageContext.request.contextPath}/registration-forms"
                                      onsubmit="return confirm('Hủy duyệt đơn này và chuyển về trạng thái chờ xử lý?')">
                                    <input type="hidden" name="action" value="undoApplicationApproval">
                                    <input type="hidden" name="registrationId" value="${application.id}">
                                    <button class="button registration-review-button registration-review-button--undo-approval" type="submit">Hủy duyệt</button>
                                </form>
                            </c:when>
                            <c:when test="${application.status eq 'REJECTED'}">
                                <form method="post" action="${pageContext.request.contextPath}/registration-forms"
                                      onsubmit="return confirm('Hủy từ chối đơn này và chuyển về trạng thái chờ duyệt?')">
                                    <input type="hidden" name="action" value="undoApplicationRejection">
                                    <input type="hidden" name="registrationId" value="${application.id}">
                                    <button class="button registration-review-button registration-review-button--undo-rejection" type="submit">Hủy từ chối</button>
                                </form>
                            </c:when>
                        </c:choose></c:if>
                    </div>
                </section>
            </main>
        </div>
    </div>
</div>
<script>
    (function() {
        const rejectionForm = document.querySelector('[data-individual-rejection-form]');
        if (!rejectionForm) return;
        const showReason = rejectionForm.querySelector('[data-show-rejection-reason]');
        const reasonPanel = rejectionForm.querySelector('[data-individual-rejection-panel]');
        const reasonInput = rejectionForm.querySelector('[name="rejectionReason"]');
        const cancelButton = rejectionForm.querySelector('[data-cancel-rejection]');
        const confirmButton = rejectionForm.querySelector('[data-confirm-rejection]');
        showReason.addEventListener('click', function() {
            reasonPanel.hidden = false;
            reasonInput.required = true;
            showReason.hidden = true;
            reasonInput.focus();
        });
        cancelButton.addEventListener('click', function() {
            reasonPanel.hidden = true;
            reasonInput.required = false;
            reasonInput.value = '';
            showReason.hidden = false;
            confirmButton.disabled = true;
        });
        reasonInput.addEventListener('input', function() {
            confirmButton.disabled = !reasonInput.value.trim();
        });
        rejectionForm.addEventListener('submit', function(event) {
            if (!reasonInput.value.trim() || !window.confirm('Từ chối đơn với lý do đã nhập?')) {
                event.preventDefault();
            }
        });
    })();
</script>
</body>
</html>