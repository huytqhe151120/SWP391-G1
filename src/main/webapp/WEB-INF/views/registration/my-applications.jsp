<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="dt" uri="http://swp391.g1/datetime" %>
<c:set var="activeNav" value="registration-forms" scope="request"/>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Đơn đăng ký</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261104-2"><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261104-4"></head><body><div class="activity-app-shell">
<jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
<div class="activity-app-layout">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main">
        <main class="page activity-list-page">
            <header class="activity-hero">
                <div class="activity-hero-copy"><span class="eyebrow">SINH VIÊN / THEO DÕI ĐĂNG KÝ</span>
                    <h1>Đơn đăng ký</h1>
                    <p>Theo dõi các hoạt động bạn đã gửi đơn và kết quả xét duyệt.</p></div>
                <a class="button button-secondary" href="${pageContext.request.contextPath}/activities">Khám phá hoạt
                    động</a></header>
            <c:if test="${param.msg eq 'registered'}">
                <div class="alert alert-success activity-alert">Đã gửi đơn đăng ký thành công. Bạn có thể theo dõi trạng
                    thái tại đây.
                </div>
            </c:if>
            <c:if test="${not empty sessionScope.successMessage}">
                <div class="alert alert-success activity-alert"><c:out value="${sessionScope.successMessage}"/></div>
                <c:remove var="successMessage" scope="session"/>
            </c:if>
            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-error activity-alert"><c:out value="${sessionScope.errorMessage}"/></div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>
            <section class="card activity-table-card">
                <div class="activity-table-wrap">
                    <table class="activity-table">
                        <thead>
                        <tr>
                            <th>Hoạt động</th>
                            <th>Đợt đăng ký</th>
                            <th>Ngày gửi</th>
                            <th>Trạng thái</th>
                            <th>Thao tác</th>
                        </tr>
                        </thead>
                        <tbody><c:forEach items="${applications}" var="app">
                            <tr>
                                <td><strong><c:out value="${app.activityName}" default="Hoạt động"/></strong></td>
                                <td><c:out value="${app.formTitle}"/></td>
                                <td><c:choose><c:when test="${not empty app.submittedAt}"><c:out
                                        value="${dt:format(app.submittedAt)}"/></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                                <td><c:choose><c:when test="${app.status eq 'SUBMITTED'}"><span
                                        class="badge status-pill status-yellow">Chờ xử lý</span></c:when><c:when
                                        test="${app.status eq 'APPROVED'}"><span class="badge status-pill status-green">Đã duyệt</span></c:when><c:when
                                        test="${app.status eq 'REJECTED'}"><span class="badge status-pill status-red">Từ chối</span><c:if test="${not empty app.rejectionReason}"><small class="registration-list-rejection-reason"><c:out value="${app.rejectionReason}"/></small></c:if></c:when><c:when
                                        test="${app.status eq 'CANCELLED'}"><span class="badge status-pill status-gray">Đã hủy đăng ký</span></c:when><c:when
                                        test="${app.status eq 'WITHDRAWN'}"><span class="badge status-pill status-blue">Đã rút đơn</span></c:when><c:otherwise><span
                                        class="badge status-pill status-muted"><c:out value="${app.status}"/></span></c:otherwise></c:choose>
                                </td>
                                <td>
                                    <a class="button button-secondary"
                                       href="${pageContext.request.contextPath}/registration-forms?action=myApplication&amp;id=${app.id}&amp;returnTo=${applicationsReturnUrlParameter}">Xem
                                        đơn</a>
                                    <c:if test="${app.status eq 'SUBMITTED' or app.status eq 'APPROVED'}">
                                        <c:choose>
                                            <c:when test="${app.canCancel}">
                                                <c:set var="confirmText" value="${app.status eq 'SUBMITTED'
                                                    ? 'Bạn có chắc muốn rút đơn đăng ký này? Bạn vẫn có thể đăng ký lại nếu hoạt động còn mở.'
                                                    : (app.quick ? 'Bạn có chắc muốn hủy đăng ký hoạt động này?'
                                                    : 'Sau khi hủy, bạn sẽ không thể đăng ký lại hoạt động này. Bạn có chắc muốn hủy?')}"/>
                                                <form action="${pageContext.request.contextPath}/registration-forms"
                                                      method="post"
                                                      style="display:inline"
                                                      onsubmit="return confirm('${confirmText}');">
                                                    <input type="hidden" name="action" value="cancelRegistration">
                                                    <input type="hidden" name="id" value="${app.id}">
                                                    <input type="hidden" name="returnTo" value="<c:out value='${applicationsReturnUrl}'/>">
                                                    <button type="submit" class="button button-revoke">
                                                            ${app.status eq 'SUBMITTED' ? 'Rút đơn' : 'Hủy đăng ký'}</button>
                                                </form>
                                            </c:when>
                                            <c:otherwise><span class="field-hint">Đã quá hạn hủy</span></c:otherwise>
                                        </c:choose>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach><c:if test="${empty applications}">
                            <tr>
                                <td colspan="5" class="empty-state"><strong>Bạn chưa gửi đơn đăng ký nào</strong>
                                    <p>Vào danh sách hoạt động để tìm hoạt động đang mở đăng ký.</p><a
                                            class="button button-primary"
                                            href="${pageContext.request.contextPath}/activities">Xem hoạt động</a></td>
                            </tr>
                        </c:if></tbody>
                    </table>
                </div>
            </section>
        </main>
    </div>
</div>
</div></body></html>