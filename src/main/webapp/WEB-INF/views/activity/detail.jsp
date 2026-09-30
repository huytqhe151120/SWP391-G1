<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết hoạt động | SWP391-G1</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<main class="page activity-list-page activity-form-page activity-detail-page">
    <header class="activity-hero">
        <div class="activity-hero-copy">
            <span class="eyebrow">SWP391-G1 <span aria-hidden="true">/</span> QUẢN LÝ HOẠT ĐỘNG</span>
            <h1>Chi tiết hoạt động</h1>
            <p><span class="activity-code"><c:out value="${activity.code}"/></span> <c:out value="${activity.name}"/></p>
        </div>
        <a class="button activity-create-button" href="${pageContext.request.contextPath}/activities">
            <span aria-hidden="true">←</span>
            Danh sách hoạt động
        </a>
    </header>

    <section class="card activity-form-card activity-detail-card">
        <div class="activity-table-heading">
            <div>
                <h2>Thông tin hoạt động</h2>
                <p>Thông tin tổ chức, thời gian và trạng thái hoạt động.</p>
            </div>
            <a class="button button-primary"
               href="${pageContext.request.contextPath}/activities?action=edit&amp;id=${activity.id}">Chỉnh sửa</a>
        </div>

        <div class="activity-detail-content">
            <section class="activity-detail-section">
                <h3>Thông tin cơ bản</h3>
                <dl class="detail-grid activity-detail-grid">
                    <div class="detail-item"><dt>Mã hoạt động</dt><dd><c:out value="${activity.code}" default="—"/></dd></div>
                    <div class="detail-item"><dt>Tên hoạt động</dt><dd><c:out value="${activity.name}" default="—"/></dd></div>
                    <div class="detail-item"><dt>Học kỳ</dt><dd><c:out value="${activity.semesterCode}" default="—"/> <c:if test="${not empty activity.semesterName}">- <c:out value="${activity.semesterName}"/></c:if></dd></div>
                    <div class="detail-item"><dt>Loại hoạt động</dt><dd><c:out value="${activity.activityTypeCode}" default="—"/> <c:if test="${not empty activity.activityTypeName}">- <c:out value="${activity.activityTypeName}"/></c:if></dd></div>
                    <div class="detail-item"><dt>Trạng thái hoạt động</dt><dd><span class="badge activity-status"><c:out value="${activity.activityStatus}" default="Chưa xác định"/></span></dd></div>
                    <div class="detail-item"><dt>Trạng thái duyệt</dt><dd><span class="badge activity-status"><c:out value="${activity.approvalStatus}" default="Chưa xác định"/></span></dd></div>
                </dl>
            </section>

            <section class="activity-detail-section">
                <h3>Phụ trách và đối tác</h3>
                <dl class="detail-grid activity-detail-grid">
                    <div class="detail-item"><dt>Đơn vị phụ trách</dt><dd><c:out value="${activity.responsibleDepartmentCode}" default="—"/> <c:if test="${not empty activity.responsibleDepartmentName}">- <c:out value="${activity.responsibleDepartmentName}"/></c:if></dd></div>
                    <div class="detail-item"><dt>Nhân viên phụ trách</dt><dd><c:out value="${activity.responsibleStaffCode}" default="—"/> <c:if test="${not empty activity.responsibleStaffName}">- <c:out value="${activity.responsibleStaffName}"/></c:if></dd></div>
                    <div class="detail-item"><dt>Công ty đối tác</dt><dd><c:out value="${activity.partnerCompanyName}" default="Không có"/></dd></div>
                    <div class="detail-item"><dt>Nhân viên đối tác</dt><dd><c:out value="${activity.partnerStaffName}" default="Không có"/></dd></div>
                </dl>
            </section>

            <section class="activity-detail-section">
                <h3>Thời gian, địa điểm và điểm số</h3>
                <dl class="detail-grid activity-detail-grid">
                    <div class="detail-item"><dt>Bắt đầu</dt><dd><c:out value="${activity.startTime}" default="Chưa xác định"/></dd></div>
                    <div class="detail-item"><dt>Kết thúc</dt><dd><c:out value="${activity.endTime}" default="Chưa xác định"/></dd></div>
                    <div class="detail-item"><dt>Điểm thưởng</dt><dd><c:out value="${activity.bonusPoint}" default="—"/></dd></div>
                    <div class="detail-item"><dt>Điểm phạt</dt><dd><c:out value="${activity.penaltyPoint}" default="—"/></dd></div>
                    <div class="detail-item field-wide"><dt>Địa điểm</dt><dd><c:out value="${activity.address}" default="Chưa cập nhật"/></dd></div>
                    <div class="detail-item field-wide"><dt>Mô tả</dt><dd class="activity-detail-description"><c:out value="${activity.description}" default="Chưa có mô tả"/></dd></div>
                </dl>
            </section>
        </div>
        <div class="detail-actions activity-detail-actions">
            <a class="button" href="${pageContext.request.contextPath}/activities">Đóng</a>
            <c:if test="${activity.approvalStatus == 'DRAFT' || activity.approvalStatus == 'REJECTED'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="submit" class="button button-primary">Gửi duyệt</button>
                </form>
            </c:if>
            <c:if test="${activity.approvalStatus == 'PENDING'}">
                <form action="${pageContext.request.contextPath}/activity/approval" method="post" style="display:inline">
                    <input type="hidden" name="id" value="${activity.id}">
                    <button type="submit" name="action" value="approve" class="button button-primary">Duyệt</button>
                    <button type="submit" name="action" value="reject" class="button">Từ chối</button>
                </form>
            </c:if>
        </div>
    </section>
</main>
</body>
</html>
