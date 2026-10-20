<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="activeNav" value="registration-forms" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Gửi đăng ký | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/activity.css?v=20261020-11">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261021-2">
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
                    <a class="button activity-create-button" href="${pageContext.request.contextPath}/registration-forms?action=student">
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
                            <p>Thông tin đăng ký hoạt động</p>
                        </div>
                        <span class="badge status-pill status-green">Đang mở</span>
                    </div>
                    <div class="activity-form-content">
                        <h3 class="student-registration-activity-name"><c:out value="${form.activityName}"/></h3>
                        <p class="student-registration-detail-description"><c:out value="${form.description}"/></p>
                        <form method="post" action="${pageContext.request.contextPath}/registration-forms">
                            <input type="hidden" name="action" value="register">
                            <input type="hidden" name="id" value="${form.id}">
                            <div class="form-grid activity-form-grid">
                                <div class="field field-wide">
                                    <label for="note">Ghi chú thêm <span class="registration-optional">(không bắt buộc)</span></label>
                                    <textarea id="note" name="note" rows="5" maxlength="2000"
                                              placeholder="Thông tin bạn muốn ban tổ chức biết..."></textarea>
                                </div>
                            </div>
                            <div class="form-actions activity-form-actions student-registration-actions">
                                <a class="button activity-cancel-button" href="${pageContext.request.contextPath}/registration-forms?action=student">Quay lại</a>
                                <button class="button button-primary" type="submit">Gửi đăng ký</button>
                            </div>
                        </form>
                    </div>
                </section>
            </main>
        </div>
    </div>
</div>
</body>
</html>
