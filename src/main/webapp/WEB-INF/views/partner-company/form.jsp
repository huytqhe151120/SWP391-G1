<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Partner Company Form</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/partner-company.css">
</head>

<body>

<c:set var="editing" value="${not empty partner}"/>

<div class="page activity-form-page">

    <!-- ================= HERO ================= -->
    <div class="page-header activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">PARTNER MANAGEMENT</span>
            <h1>
                <c:choose>
                    <c:when test="${editing}">Sửa đối tác</c:when>
                    <c:otherwise>Thêm đối tác</c:otherwise>
                </c:choose>
            </h1>
            <p>Nhập thông tin liên hệ của công ty đối tác</p>
        </div>
    </div>


    <!-- ================= FORM CARD ================= -->
    <form action="${pageContext.request.contextPath}/partner-company"
          method="post"
          class="card activity-form-card">

        <input type="hidden" name="action" value="${editing ? 'update' : 'create'}">
        <c:if test="${editing}">
            <input type="hidden" name="id" value="${partner.id}">
        </c:if>

        <div class="activity-table-heading">
            <div>
                <h2>
                    <c:choose>
                        <c:when test="${editing}">Cập nhật đối tác</c:when>
                        <c:otherwise>Đối tác mới</c:otherwise>
                    </c:choose>
                </h2>
                <p>Các trường có dấu <span class="required-mark">*</span> là bắt buộc</p>
            </div>
        </div>

        <div class="activity-form-content">

            <div class="activity-form-section">
                <h3>Thông tin đối tác</h3>
                <p>Tên, email và số điện thoại liên hệ</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field field-wide">
                    <label for="name">Tên <span class="required-mark">*</span></label>
                    <input id="name" name="name" required
                           value="<c:out value='${partner.name}'/>">
                </div>

                <div class="field">
                    <label for="email">Email <span class="required-mark">*</span></label>
                    <input id="email" type="email" name="email" required
                           value="<c:out value='${partner.email}'/>">
                </div>

                <div class="field">
                    <label for="phone">Số điện thoại <span class="required-mark">*</span></label>
                    <input id="phone" name="phone" required
                           value="<c:out value='${partner.phone}'/>">
                </div>

                <div class="field field-wide">
                    <label for="address">Địa chỉ</label>
                    <input id="address" name="address"
                           value="<c:out value='${partner.address}'/>">
                </div>

            </div>


            <div class="activity-form-section">
                <h3>Trạng thái</h3>
                <p>Trạng thái hoạt động của đối tác</p>
            </div>

            <div class="form-grid activity-form-grid">

                <div class="field">
                    <label for="status">Trạng thái <span class="required-mark">*</span></label>
                    <input id="status" name="status" required
                           placeholder="ACTIVE hoặc INACTIVE"
                           value="<c:out value='${partner.status}'/>">
                </div>

            </div>

        </div>


        <!-- ================= ACTIONS ================= -->
        <div class="form-actions activity-form-actions">
            <a class="button activity-cancel-button"
               href="${pageContext.request.contextPath}/partner-company">Hủy</a>
            <button type="submit" class="button button-primary">Lưu</button>
        </div>

    </form>

</div>

</body>
</html>
