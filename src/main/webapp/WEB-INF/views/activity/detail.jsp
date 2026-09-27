<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Activity Detail</title>
</head>
<body>

<h1>Chi tiết hoạt động ngoại khóa</h1>

<dl>
    <dt>ID</dt>
    <dd><c:out value="${activity.id}"/></dd>

    <dt>Mã hoạt động</dt>
    <dd><c:out value="${activity.code}"/></dd>

    <dt>Tên hoạt động</dt>
    <dd><c:out value="${activity.name}"/></dd>

    <dt>Học kỳ ID</dt>
    <dd><c:out value="${activity.semesterId}"/></dd>

    <dt>Activity Type ID</dt>
    <dd><c:out value="${activity.activityType}"/></dd>

    <dt>Đơn vị phụ trách ID</dt>
    <dd><c:out value="${activity.responsibleDepartmentId}"/></dd>

    <dt>Nhân viên phụ trách ID</dt>
    <dd><c:out value="${activity.responsibleStaffId}"/></dd>

    <dt>Partner Company ID</dt>
    <dd>
        <c:choose>
            <c:when test="${activity.partnerId > 0}">
                <c:out value="${activity.partnerId}"/>
            </c:when>
            <c:otherwise>--</c:otherwise>
        </c:choose>
    </dd>

    <dt>Partner Staff ID</dt>
    <dd>
        <c:choose>
            <c:when test="${activity.partnerStaffId > 0}">
                <c:out value="${activity.partnerStaffId}"/>
            </c:when>
            <c:otherwise>--</c:otherwise>
        </c:choose>
    </dd>

    <dt>Điểm cộng</dt>
    <dd><c:out value="${activity.bonusPoint}"/></dd>

    <dt>Điểm trừ</dt>
    <dd><c:out value="${activity.penaltyPoint}"/></dd>

    <dt>Địa chỉ</dt>
    <dd><c:out value="${activity.address}"/></dd>

    <dt>Mô tả</dt>
    <dd><c:out value="${activity.description}"/></dd>

    <dt>Activity Status</dt>
    <dd><c:out value="${activity.activityStatus}"/></dd>

    <dt>Approval Status</dt>
    <dd><c:out value="${activity.approvalStatus}"/></dd>

    <dt>Ngày tạo</dt>
    <dd><c:out value="${activity.createdAt}"/></dd>
</dl>

<c:if test="${activity.approvalStatus == 'PENDING'}">
    <form action="${pageContext.request.contextPath}/activities"
          method="post"
          style="display:inline">
        <input type="hidden" name="action" value="approve">
        <input type="hidden" name="id" value="${activity.id}">
        <button type="submit">Duyệt hoạt động</button>
    </form>

    <form action="${pageContext.request.contextPath}/activities"
          method="post"
          style="display:inline">
        <input type="hidden" name="action" value="reject">
        <input type="hidden" name="id" value="${activity.id}">
        <input type="text" name="note" placeholder="Lý do từ chối">
        <button type="submit">Từ chối</button>
    </form>
</c:if>

<p>
    <a href="${pageContext.request.contextPath}/activities">Quay lại</a>
</p>

</body>
</html>
