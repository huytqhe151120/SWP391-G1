<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Partner Staff Form</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/partner-staff.css">
</head>

<body>

<c:set var="editing" value="${not empty staff}"/>

<div class="page activity-form-page">

    <!-- ================= HERO ================= -->
    <div class="page-header activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">PARTNER STAFF MANAGEMENT</span>
            <h1>
                <c:choose>
                    <c:when test="${editing}">Sửa nhân viên đối tác</c:when>
                    <c:otherwise>Thêm nhân viên đối tác</c:otherwise>
                </c:choose>
            </h1>
            <p>Nhập thông tin liên hệ và công ty của nhân viên đối tác</p>
        </div>
    </div>


    <!-- ================= FORM CARD ================= -->
    <form action="${pageContext.request.contextPath}/partner-staff"
          method="post"
          class="card activity-form-card">

        <input type="hidden" name="action" value="${editing ? 'update' : 'create'}">
        <c:if test="${editing}">
            <input type="hidden" name="id" value="${staff.id}">
        </c:if>

        <div class="activity-table-heading">
            <div>
                <h2>
                    <c:choose>
                        <c:when test="${editing}">Cập nhật nhân viên</c:when>
                        <c:otherwise>Nhân viên mới</c:otherwise>
                    </c:choose>
                </h2>
                <p>Các trường có dấu <span class="required-mark">*</span> là bắt buộc</p>
            </div>
        </div>

        <div class="activity-form-content">

            <!-- ===== Thông tin cá nhân ===== -->
            <div class="activity-form-section">
                <h3>Thông tin cá nhân</h3>
                <p>Họ tên, email và số điện thoại liên hệ</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field field-wide">
                    <label for="fullName">Họ tên <span class="required-mark">*</span></label>
                    <input id="fullName" name="fullName" required
                           value="<c:out value='${staff.fullName}'/>">
                </div>

                <div class="field">
                    <label for="email">Email <span class="required-mark">*</span></label>
                    <input id="email" type="email" name="email" required
                           value="<c:out value='${staff.email}'/>">
                </div>

                <div class="field">
                    <label for="phone">Số điện thoại <span class="required-mark">*</span></label>
                    <input id="phone" name="phone" required
                           value="<c:out value='${staff.phone}'/>">
                </div>

            </div>


            <!-- ===== Công việc ===== -->
            <div class="activity-form-section">
                <h3>Công việc</h3>
                <p>Chức vụ, công ty đối tác và trạng thái</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field">
                    <label for="position">Chức vụ</label>
                    <input id="position" name="position"
                           value="<c:out value='${staff.position}'/>">
                </div>

                <div class="field">
                    <label for="partnerId">Partner ID <span class="required-mark">*</span></label>
                    <input id="partnerId" type="number" name="partnerId" min="1" required
                           value="<c:out value='${staff.partnerId}'/>">
                </div>

                <div class="field">
                    <label for="isActive">Active</label>
                    <select id="isActive" name="isActive">
                        <option value="true"  ${staff.isActive != false ? 'selected' : ''}>Có</option>
                        <option value="false" ${staff.isActive == false ? 'selected' : ''}>Không</option>
                    </select>
                </div>

            </div>

        </div>


        <!-- ================= ACTIONS ================= -->
        <div class="form-actions activity-form-actions">
            <a class="button activity-cancel-button"
               href="${pageContext.request.contextPath}/partner-staff">Hủy</a>
            <button type="submit" class="button button-primary">Lưu</button>
        </div>

    </form>

</div>

</body>
</html>
