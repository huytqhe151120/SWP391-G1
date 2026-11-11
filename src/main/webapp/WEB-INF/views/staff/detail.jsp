<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="st" uri="http://swp391.g1/status" %>
<c:set var="activeNav" value="staff" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết nhân viên | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/account.css?v=20261110-3">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hero-scroll.css?v=20261107-2">
</head>
<body>
<div class="activity-app-shell hero-scroll">
    <jsp:include page="/WEB-INF/views/layout/app-sidebar.jsp"/>
    <div class="activity-app-main-wrapper">
        <jsp:include page="/WEB-INF/views/layout/app-navigation.jsp"/>
        <div class="activity-app-main">
<main class="page activity-list-page activity-form-page account-page">
    <header class="activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ NHÂN VIÊN</span>
            <h1>Chi tiết nhân viên</h1>
            <p><span class="activity-code"><c:out value="${staff.code}"/></span></p>
        </div>
        <a class="button activity-create-button" href="${pageContext.request.contextPath}/staff">Danh sách nhân viên</a>
    </header>

    <c:if test="${param.msg == 'updated'}">
        <div class="alert alert-success activity-alert" role="status">Cập nhật thông tin nhân viên thành công.</div>
    </c:if>

    <section class="card activity-form-card activity-detail-card">
        <div class="activity-table-heading">
            <div><h2>Thông tin nhân viên</h2><p>Thông tin hồ sơ và trạng thái của nhân viên.</p></div>
            <a class="button button-primary"
               href="${pageContext.request.contextPath}/staff?action=edit&amp;id=${staff.id}">Chỉnh sửa</a>
        </div>
        <div class="activity-detail-content">
            <section class="activity-detail-section">
                <h3>Thông tin cá nhân</h3>
                <dl class="detail-grid activity-detail-grid">
                    <div class="detail-item"><dt>ID nhân viên</dt><dd><c:out value="${staff.id}"/></dd></div>
                    <div class="detail-item"><dt>Mã nhân viên</dt><dd><c:out value="${staff.code}"/></dd></div>
                    <div class="detail-item"><dt>Họ và tên</dt><dd><c:out value="${staff.name}"/></dd></div>
                    <div class="detail-item"><dt>Ngày sinh</dt><dd><c:out value="${staff.dob}" default="—"/></dd></div>
                    <div class="detail-item"><dt>Giới tính</dt><dd>
                        <c:choose><c:when test="${staff.gender == 'true'}">Nam</c:when>
                            <c:when test="${staff.gender == 'false'}">Nữ</c:when><c:otherwise>—</c:otherwise></c:choose>
                    </dd></div>
                    <div class="detail-item"><dt>Account ID</dt><dd><c:out value="${staff.accountId > 0 ? staff.accountId : '—'}"/></dd></div>
                    <div class="detail-item"><dt>Trạng thái</dt><dd><span class="badge status-pill ${st:css(staff.status)}">${st:label(staff.status)}</span></dd></div>
                </dl>
            </section>
        </div>
        <div class="detail-actions activity-detail-actions">
            <a class="button" href="${pageContext.request.contextPath}/staff">Quay lại danh sách</a>
        </div>
    </section>
</main>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
</body>
</html>
