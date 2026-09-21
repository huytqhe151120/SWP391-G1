<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><title>Activity detail</title></head>
<body>
<h1>Chi tiết activity</h1>
<dl>
    <dt>ID</dt><dd><c:out value="${activity.id}"/></dd><dt>Mã</dt><dd><c:out value="${activity.code}"/></dd>
    <dt>Tên</dt><dd><c:out value="${activity.name}"/></dd><dt>Học kỳ</dt><dd><c:out value="${activity.semesterId}"/></dd>
    <dt>Loại</dt><dd><c:out value="${activity.activityType}"/></dd><dt>Đơn vị phụ trách</dt><dd><c:out value="${activity.responsibleDepartmentId}"/></dd>
    <dt>Nhân viên phụ trách</dt><dd><c:out value="${activity.responsibleStaffId}"/></dd><dt>Partner</dt><dd><c:out value="${activity.partnerId}"/></dd>
    <dt>Partner staff</dt><dd><c:out value="${activity.partnerStaffId}"/></dd><dt>Điểm cộng</dt><dd><c:out value="${activity.bonusPoint}"/></dd>
    <dt>Điểm trừ</dt><dd><c:out value="${activity.penaltyPoint}"/></dd><dt>Địa chỉ</dt><dd><c:out value="${activity.address}"/></dd>
    <dt>Mô tả</dt><dd><c:out value="${activity.description}"/></dd><dt>Trạng thái</dt><dd><c:out value="${activity.activityStatus}"/></dd>
    <dt>Trạng thái duyệt</dt><dd><c:out value="${activity.approvalStatus}"/></dd><dt>Ngày tạo</dt><dd><c:out value="${activity.createdAt}"/></dd>
</dl>
<a href="${pageContext.request.contextPath}/activities">Quay lại</a>
</body></html>