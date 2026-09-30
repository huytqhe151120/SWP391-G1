<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Partner Company Detail</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/css/partner-company.css">
</head>

<body>

<div class="page activity-form-page">

    <!-- ================= HERO ================= -->
    <div class="page-header activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">PARTNER MANAGEMENT</span>
            <h1>Chi tiết đối tác</h1>
            <p><c:out value="${partner.name}"/></p>
        </div>
    </div>


    <!-- ================= DETAIL CARD ================= -->
    <div class="card activity-detail-card">

        <div class="activity-table-heading">
            <div>
                <h2>Thông tin đối tác</h2>
                <p>Chi tiết đầy đủ của công ty đối tác</p>
            </div>

            <a class="button"
               href="${pageContext.request.contextPath}/partner-company">Quay lại</a>
        </div>

        <div class="activity-detail-content">

            <div class="activity-detail-section">
                <h3>Thông tin chung</h3>

                <dl class="detail-grid activity-detail-grid">

                    <div class="detail-item">
                        <dt>ID</dt>
                        <dd><c:out value="${partner.id}"/></dd>
                    </div>

                    <div class="detail-item">
                        <dt>Tên</dt>
                        <dd><strong class="activity-name"><c:out value="${partner.name}"/></strong></dd>
                    </div>

                    <div class="detail-item">
                        <dt>Email</dt>
                        <dd><c:out value="${partner.email}" default="—"/></dd>
                    </div>

                    <div class="detail-item">
                        <dt>Số điện thoại</dt>
                        <dd><c:out value="${partner.phone}" default="—"/></dd>
                    </div>

                    <div class="detail-item">
                        <dt>Địa chỉ</dt>
                        <dd><c:out value="${partner.address}" default="—"/></dd>
                    </div>

                    <div class="detail-item">
                        <dt>Trạng thái</dt>
                        <dd>
                            <c:choose>
                                <c:when test="${partner.status == 'ACTIVE'}">
                                    <span class="badge partner-company-active">Active</span>
                                </c:when>
                                <c:when test="${partner.status == 'INACTIVE'}">
                                    <span class="badge partner-company-inactive">Inactive</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge"><c:out value="${partner.status}" default="—"/></span>
                                </c:otherwise>
                            </c:choose>
                        </dd>
                    </div>

                    <div class="detail-item">
                        <dt>Ngày tạo</dt>
                        <dd><c:out value="${partner.createdAt}" default="—"/></dd>
                    </div>

                </dl>
            </div>

        </div>


        <!-- ================= ACTIONS ================= -->
        <div class="detail-actions activity-detail-actions">
            <a class="button"
               href="${pageContext.request.contextPath}/partner-company?action=edit&amp;id=${partner.id}">Sửa</a>
        </div>

    </div>

</div>

</body>
</html>
