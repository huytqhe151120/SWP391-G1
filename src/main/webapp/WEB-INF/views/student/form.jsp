<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="activeNav" value="students" scope="request"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chỉnh sửa sinh viên | SWP391-G1</title>
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
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ SINH VIÊN</span>
            <h1>Chỉnh sửa sinh viên</h1>
            <p>Cập nhật hồ sơ <span class="activity-code">#<c:out value="${student.id}"/></span></p>
        </div>
        <a class="button activity-create-button"
           href="${pageContext.request.contextPath}/student?action=detail&amp;id=${student.id}">Chi tiết sinh viên</a>
    </header>

    <c:if test="${not empty formError}">
        <div class="alert alert-error activity-alert" role="alert"><c:out value="${formError}"/></div>
    </c:if>

    <section class="card activity-form-card">
        <div class="activity-table-heading">
            <div><h2>Thông tin sinh viên</h2><p>Cập nhật các thông tin hồ sơ bên dưới.</p></div>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/student">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="id" value="<c:out value='${student.id}'/>">
            <div class="activity-form-content">
                <div class="form-grid activity-form-grid account-form-grid">
                    <div class="field">
                        <label for="code">Mã sinh viên <span class="required-mark">*</span></label>
                        <input id="code" name="code" value="<c:out value='${student.code}'/>" required>
                    </div>
                    <div class="field">
                        <label for="name">Họ và tên <span class="required-mark">*</span></label>
                        <input id="name" name="name" value="<c:out value='${student.name}'/>" required>
                    </div>
                    <div class="field">
                        <label for="dob">Ngày sinh</label>
                        <input id="dob" type="date" name="dob" value="<c:out value='${student.dob}'/>">
                    </div>
                    <div class="field">
                        <label for="gender">Giới tính</label>
                        <select id="gender" name="gender">
                            <option value="">Chưa cập nhật</option>
                            <option value="true" ${student.gender == 'true' ? 'selected' : ''}>Nam</option>
                            <option value="false" ${student.gender == 'false' ? 'selected' : ''}>Nữ</option>
                        </select>
                    </div>
                    <div class="field">
                        <label for="email">Email</label>
                        <input id="email" type="email" name="email" value="<c:out value='${student.email}'/>">
                    </div>
                    <div class="field">
                        <label for="phoneNumber">Số điện thoại</label>
                        <input id="phoneNumber" type="tel" name="phoneNumber" value="<c:out value='${student.phoneNumber}'/>">
                    </div>
                    <div class="field">
                        <label for="status">Trạng thái <span class="required-mark">*</span></label>
                        <select id="status" name="status" required>
                            <option value="ACTIVE" ${student.status == 'ACTIVE' ? 'selected' : ''}>Đang hoạt động</option>
                            <option value="INACTIVE" ${student.status == 'INACTIVE' ? 'selected' : ''}>Không hoạt động</option>
                        </select>
                    </div>
                    <div class="field">
                        <span class="field-label">Lớp</span>
                        <input value="<c:out value='${student.mainClassCode}' default='Chưa xếp lớp'/>" disabled>
                    </div>
                    <div class="field">
                        <span class="field-label">Khoa</span>
                        <input value="<c:out value='${student.departmentName}' default='Chưa cập nhật'/>" disabled>
                    </div>
                    <div class="field">
                        <span class="field-label">Ngành</span>
                        <input value="<c:out value='${student.majorName}' default='Chưa cập nhật'/>" disabled>
                        <span class="field-hint">Lớp, khoa, ngành và tài khoản liên kết được xác định từ chương trình đào tạo.</span>
                    </div>
                </div>
            </div>
            <div class="form-actions activity-form-actions">
                <a class="button activity-cancel-button"
                   href="${pageContext.request.contextPath}/student?action=detail&amp;id=${student.id}">Hủy</a>
                <button class="button button-primary" type="submit">Lưu thay đổi</button>
            </div>
        </form>
    </section>
</main>
<script src="${pageContext.request.contextPath}/assets/js/hero-scroll.js?v=1"></script>
        </div>
    </div>
</div>
</body>
</html>
